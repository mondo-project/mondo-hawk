/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *    Abel G�mez - Generic methods
 *******************************************************************************/
package org.hawk.service.api.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TZlibTransport;
import org.hawk.service.api.File;
import org.hawk.service.api.Subscription;
import org.hawk.service.api.SubscriptionDurability;
import org.hawk.service.artemis.consumer.Consumer;
import org.hawk.service.artemis.consumer.Consumer.QueueType;

/**
 * Utility methods for connecting to the Hawk APIs. These use the optional
 * dependency on Apache HTTP Components.
 */
public class APIUtils {

	/*
	 * Note: all values of this enum must have names in uppercase, so the
	 * 'tprotocol' values of the <code>hawk+http(s)://</code> URLs will be case
	 * insensitive.
	 */
	public static enum ThriftProtocol {
		/**
		 * Efficient, compatible with almost all languages in Thrift 0.9.2.
		 * (including JavaScript, with an unofficial extension).
		 */
		BINARY(new TBinaryProtocol.Factory()),
		/**
		 * More space efficient than {@link #BINARY} at the expense of some
		 * time, but not available for JavaScript.
		 */
		COMPACT(new TCompactProtocol.Factory()),
		/**
		 * More space efficient than {@link #COMPACT}, but only available
		 * for Java in Thrift 0.9.2.
		 */
		TUPLE(new TTupleProtocol.Factory()),
		/**
		 * Compatible with all languages of interest (including JavaScript) out
		 * of the box, but not very efficient.
		 */
		JSON(new TJSONProtocol.Factory());

		private final TProtocolFactory protocolFactory;

		private ThriftProtocol(TProtocolFactory factory) {
			this.protocolFactory = factory;
		}

		public static String[] strings() {
			return toStringArray(values());
		}

		public static ThriftProtocol guessFromURL(String location) {
			ThriftProtocol proto = TUPLE;
			if (location.endsWith("compact")) {
				proto = COMPACT;
			} else if (location.endsWith("binary")) {
				proto = BINARY;
			} else if (location.endsWith("json")) {
				proto = JSON;
			}
			return proto;
		}

		public TProtocolFactory getProtocolFactory() {
			return protocolFactory;
		}
	}

	private APIUtils() {
	}

	public static Consumer connectToArtemis(Subscription s, SubscriptionDurability sd) throws Exception {
		return Consumer.connectRemote(s.host, s.port, s.queueAddress, s.queueName, toQueueType(sd), s.sslRequired);
	}

	public static <T extends TServiceClient> T connectTo(Class<T> clazz, String url) throws TTransportException, URISyntaxException {
		return connectTo(clazz, url, ThriftProtocol.TUPLE);
	}

	public static <T extends TServiceClient> T connectTo(Class<T> clazz, String url, ThriftProtocol thriftProtocol) throws TTransportException, URISyntaxException {
		return connectTo(clazz, url, thriftProtocol, null, null);
	}

	public static <T extends TServiceClient> T connectTo(Class<T> clazz, String url, ThriftProtocol thriftProtocol, String username, String password) throws TTransportException, URISyntaxException {
		final UsernamePasswordCredentials credentials = (username != null && password != null) ? new UsernamePasswordCredentials(username, password) : null;
		return connectTo(clazz, url, thriftProtocol, credentials);
	}

	@SuppressWarnings({ "deprecation", "restriction" })
	public static <T extends TServiceClient> T connectTo(Class<T> clazz, String url, ThriftProtocol thriftProtocol, final Credentials credentials) throws TTransportException, URISyntaxException {
		try {
			final URI parsed = new URI(url);

			TTransport transport;
			if (parsed.getScheme().startsWith("http")) {
				final DefaultHttpClient httpClient = APIUtils.createGZipAwareHttpClient();
				if (credentials != null) {
					httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1), credentials);
				}
				transport = new THttpClient(url, httpClient);
			} else {
				transport = new TZlibTransport(new TSocket(parsed.getHost(), parsed.getPort()));
				transport.open();
			}
			Constructor<T> constructor = clazz.getDeclaredConstructor(org.apache.thrift.protocol.TProtocol.class);
			return constructor.newInstance(thriftProtocol.getProtocolFactory().getProtocol(transport));
		} catch (InstantiationException 
				| IllegalAccessException 
				| IllegalArgumentException 
				| InvocationTargetException 
				| NoSuchMethodException
				| SecurityException e) {
			throw new TTransportException(e);
		}
	}

	public static File convertJavaFileToThriftFile(java.io.File rawFile) throws FileNotFoundException, IOException {
		try (FileInputStream fIS = new FileInputStream(rawFile)) {
			FileChannel chan = fIS.getChannel();

			/* Note: this cast limits us to 2GB files - this shouldn't
			 be a problem, but if it were we could use FileChannel#map
			 and call Hawk.Client#registerModels one file at a time. */
			ByteBuffer buf = ByteBuffer.allocate((int) chan.size());
			chan.read(buf);
			buf.flip();

			File mmFile = new File();
			mmFile.name = rawFile.getName();
			mmFile.contents = buf;
			return mmFile;
		}
	}

	@SuppressWarnings({ "restriction", "deprecation" })
	private static DefaultHttpClient createGZipAwareHttpClient() {
		/*
		 * Apache HttpClient 4.3 and later deprecate DefaultHttpClient in favour
		 * of HttpClientBuilder, but Hadoop 2.7.x (used by CloudATL) uses Apache
		 * HttpClient 4.2.5. Until Hadoop upgrades to HttpClient 4.3+, we'll
		 * have to keep using this deprecated API. After that, we'll be able to
		 * replace this bit of code with something like:
		 *
		 * <pre>
		 *  return HttpClientBuilder.create()
		 *      .addInterceptorFirst(new GZipRequestInterceptor())
		 *   .addInterceptorFirst(new GZipResponseInterceptor())
		 *   .build();
		 * </pre>
		 */
		final DefaultHttpClient client = new DefaultHttpClient();
		client.addRequestInterceptor(new GZipRequestInterceptor());
		client.addResponseInterceptor(new GZipResponseInterceptor());
		return client;
	}

	private static QueueType toQueueType(SubscriptionDurability sd) {
		switch (sd) {
		case DEFAULT: return QueueType.DEFAULT;
		case DURABLE: return QueueType.DURABLE;
		case TEMPORARY: return QueueType.TEMPORARY;
		default: throw new IllegalArgumentException("Unknown subscription durability " + sd);
		}
	}

	private static <T> String[] toStringArray(Object[] c) {
		final String[] strings = new String[c.length];
		int i = 0;
		for (Object o : c) {
			strings[i++] = o + "";
		}
		return strings;
	}

}
