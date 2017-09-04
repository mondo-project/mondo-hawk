/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.service.servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.servlet.ServletException;

import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadPoolServer.Args;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TZlibTransport;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.osgiserver.HManager;
import org.hawk.service.api.Hawk;
import org.hawk.service.api.Hawk.Iface;
import org.hawk.service.api.Hawk.Processor;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.artemis.server.Server;
import org.hawk.service.servlet.config.HawkServerConfigurator;
import org.hawk.service.servlet.processors.HawkThriftIface;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator for the Hawk servlet plugin. The plugin starts an embedded Apache
 * Artemis messaging server (for notifications). It listens on
 * {@link TransportConstants#DEFAULT_HOST} and port
 * {@link TransportConstants#DEFAULT_PORT} by default, but these can be changed
 * by setting the {@link #ARTEMIS_HOST_PROPERTY} and/or
 * {@link #ARTEMIS_PORT_PROPERTY} system properties.
 */
public class Activator implements BundleActivator {

	private static final String ARTEMIS_PORT_PROPERTY = "hawk.artemis.port";
	private static final String ARTEMIS_HOST_PROPERTY = "hawk.artemis.host";
	private static final String ARTEMIS_LISTENALL_PROPERTY = "hawk.artemis.listenAll";
	private static final String ARTEMIS_SSL_PROPERTY = "hawk.artemis.sslEnabled";
	private static final String TCP_PORT_PROPERTY = "hawk.tcp.port";
	private static final String TCP_TPROTOCOL_PROPERTY = "hawk.tcp.thriftProtocol";

	private static BundleContext context;
	private static Activator instance;
	private static HawkServerConfigurator serverConfigurator;
	public static Activator getInstance() {
		return instance;
	}

	public static String getPluginId() {
		return context.getBundle().getSymbolicName();
	}

	private Server artemis;
	private TThreadPoolServer tcpServer;

	public Activator() {
		Activator.instance = this;
	}

	public File getDataFile(String filename) {
		return context.getDataFile(filename);
	}

	public File writeToDataFile(String filename, ByteBuffer contents)
			throws FileNotFoundException, IOException {
		// Store in the plugin's persistent store
		final java.io.File destFile = getDataFile(filename);

		// The FOS is closed while closing the channel, so we can suppress this
		// warning
		try (@SuppressWarnings("resource")
		FileChannel fc = new FileOutputStream(destFile).getChannel()) {
			fc.write(contents);
		}

		return destFile;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {

		Activator.context = bundleContext;
		HManager.getInstance();

		String artemisHost = System.getProperty(ARTEMIS_HOST_PROPERTY);
		if (artemisHost == null) {
			artemisHost = TransportConstants.DEFAULT_HOST;
		}

		String sArtemisPort = System.getProperty(ARTEMIS_PORT_PROPERTY);
		int artemisPort;
		if (sArtemisPort == null) {
			artemisPort = TransportConstants.DEFAULT_PORT;
		} else {
			artemisPort = Integer.valueOf(sArtemisPort);
		}

		artemis = new Server(artemisHost, artemisPort);
		String sListenAll = System.getProperty(ARTEMIS_LISTENALL_PROPERTY);
		if (sListenAll != null) {
			artemis.setListenOnAllInterfaces(Boolean.valueOf(sListenAll));
		}
		String sSSLEnabled = System.getProperty(ARTEMIS_SSL_PROPERTY);
		if (sSSLEnabled != null) {
			artemis.setSSLEnabled(Boolean.valueOf(sSSLEnabled));
		}
		try {
			artemis.start();
		} catch (Exception e) {
			throw new ServletException(e);
		}

		final String sTCPPort = System.getProperty(TCP_PORT_PROPERTY);
		if (sTCPPort != null) {
			final String sThriftProtocol = System.getProperty(TCP_TPROTOCOL_PROPERTY);
			final ThriftProtocol thriftProtocol = (sThriftProtocol != null)
					? ThriftProtocol.valueOf(sThriftProtocol)
							: ThriftProtocol.TUPLE;

					final TServerSocket tcpServerSocket = new TServerSocket(Integer.valueOf(sTCPPort));
					final HawkThriftIface hawkIface = new HawkThriftIface(ThriftProtocol.TUPLE, null, artemis);
					final Processor<Iface> hawkTCPProcessor = new Hawk.Processor<Hawk.Iface>(hawkIface);
					serverConfigurator = new HawkServerConfigurator(hawkIface);
					serverConfigurator.loadHawkServerConfigurations();

					final Args tcpServerArgs = new TThreadPoolServer.Args(tcpServerSocket)
					.maxWorkerThreads(10_000)
					.protocolFactory(new TProtocolFactory() {
						private static final long serialVersionUID = 1L;

						@Override
						public TProtocol getProtocol(TTransport arg0) {
							return thriftProtocol.getProtocolFactory().getProtocol(new TZlibTransport(arg0));
						}
					})
					.processor(hawkTCPProcessor);

					tcpServer = new TThreadPoolServer(tcpServerArgs);
					new Thread(new Runnable() {
						@Override
						public void run() {



							final Bundle bundle = context.getBundle();
							Platform.getLog(bundle).log(new Status(IStatus.INFO, bundle.getSymbolicName(),
									"Starting Hawk TCP server on port " + sTCPPort + " with Thrift protocol " + thriftProtocol.name()));
							tcpServer.serve();


						}
					}).start();
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {

		/*if(serverConfigurator != null) {
			serverConfigurator.saveHawkServerConfigurations();
		}*/

		
		Activator.context = null;
		
		HManager.getInstance().stopAllRunningInstances(
				ShutdownRequestType.ONLY_LOCAL);
		
		
		if (artemis != null) {
			artemis.stop();
			artemis = null;
		}

		if (tcpServer != null) {
			tcpServer.stop();
			tcpServer = null;
		}
	}

	public Server getArtemisServer() {
		return artemis;
	}
}
