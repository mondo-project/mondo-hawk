/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.service.artemis.consumer;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class for consuming messages sent to an Artemis address.
 * After opening a session with {@link #openSession()}, messages can be consumed
 * synchronously with {@link #processChangesSync(MessageHandler)} or
 * asynchronously with {@link #processChangesAsync(MessageHandler)}.
 */
public class Consumer implements Closeable {

	public static enum QueueType {
		/** Deleted after client or server disconnection. */ TEMPORARY,
		/** Survives disconnections and server restarts. */ DURABLE,
		/** Survives disconnections but not server restarts. */ DEFAULT;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

	private final TransportConfiguration transportConfig;
	private final String queueName;

	private ServerLocator locator;
	private ClientSessionFactory sessionFactory;
	private ClientSession session;
	private ClientConsumer consumer;

	private String queueAddress;

	private QueueType queueType;

	/**
	 * Factory method for connecting to a remote Artemis instance.
	 * @param String host Host name of the Artemis instance.
	 * @param int port Port in which Artemis is listening.
	 * @param String queueAddress
	 */
	public static Consumer connectRemote(String host, int port, String queueAddress, String queueName, QueueType queueType, boolean isSSLEnabled) throws Exception {
		final Map<String, Object> params = new HashMap<>();
		params.put(TransportConstants.HOST_PROP_NAME, host);
		params.put(TransportConstants.PORT_PROP_NAME, port);
		params.put(TransportConstants.SSL_ENABLED_PROP_NAME, isSSLEnabled);
		final TransportConfiguration config = new TransportConfiguration(NettyConnectorFactory.class.getName(), params);

		return new Consumer(config, queueAddress, queueName, queueType);
	}

	/**
	 * Creates a new consumer.
	 * @param address Artemis address where the queue should be created.
	 * @param name Name of the queue to be created or accessed.
	 * @param type Type of queue to be created.
	 * @throws Exception Could not connect to the Artemis server.
	 */
	private Consumer(TransportConfiguration config, String address, String queueName, QueueType queueType) {
		this.transportConfig = config;
		this.queueAddress = address;
		this.queueType = queueType;
		this.queueName = queueName;
	}

	public void openSession(String username, String password) throws Exception {
		if (session != null) return;

		locator = ActiveMQClient.createServerLocatorWithoutHA(transportConfig);
		sessionFactory = locator.createSessionFactory();
		session = sessionFactory.createSession(username, password, false, true, true,
				locator.isPreAcknowledge(), locator.getAckBatchSize());
		final boolean queueExists = session.queueQuery(new SimpleString(queueName)).isExists();
		if (!queueExists) {
			createQueue();
		}
		consumer = session.createConsumer(queueName);
		session.start();
	}

	public void commitSession() throws ActiveMQException {
		if (session == null) return;
		session.commit();
	}

	public void closeSession() throws ActiveMQException {
		if (consumer != null) {
			consumer.close();
			consumer = null;
		}

		if (session != null) {
			session.close();
			session = null;
		}

		if (sessionFactory != null) {
			sessionFactory.close();
			sessionFactory = null;
		}

		if (locator != null) {
			locator.close();
			locator = null;
		}
	}

	public boolean isSessionOpen() {
		return session != null && !session.isClosed();
	}

	/**
	 * Consumes all pending messages from the queue, translating them into
	 * events for the {@link IGraphChangeListener}.
	 * @throws ActiveMQException 
	 */
	public void processChangesSync(MessageHandler handler, long timeout) throws ActiveMQException {
		if (session == null) {
			LOGGER.warn("Session not open: cannot process any changes");
			return;
		}

		ClientMessage msg;
		while ((msg = consumer.receive(timeout)) != null) {
			handler.onMessage(msg);
		}
	}

	/**
	 * Sets up the consumer to asynchronously receive messages and translate
	 * them into events for the {@link IGraphChangeListener}.
	 * @throws ActiveMQException The session is open, but the consumer was closed.
	 */
	public void processChangesAsync(final MessageHandler handler) throws ActiveMQException {
		if (session == null) {
			LOGGER.warn("Session not open: cannot process any changes");
			return;
		}
		consumer.setMessageHandler(handler);
	}

	private void createQueue() throws ActiveMQException {
		switch (queueType) {
			case TEMPORARY:
				session.createTemporaryQueue(queueAddress, queueName);
				break;
			case DURABLE:
			case DEFAULT:
				session.createQueue(queueAddress, queueName, queueType == QueueType.DURABLE);
				break;
			default: throw new IllegalArgumentException("Unknown queue type");
		}
	}

	@Override
	public void close() throws IOException {
		try {
			closeSession();
		} catch (ActiveMQException e) {
			throw new IOException(e);
		}
	}
}
