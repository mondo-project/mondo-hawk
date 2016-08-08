/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package uk.ac.york.mondo.integration.hawk.servlet.artemis;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;
import org.hawk.core.IStateListener;
import org.hawk.osgiserver.HModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.york.mondo.integration.api.HawkStateEvent;
import uk.ac.york.mondo.integration.api.utils.APIUtils.ThriftProtocol;
import uk.ac.york.mondo.integration.api.utils.ActiveMQBufferTransport;

public class ArtemisProducerStateListener implements IStateListener {

	private final HModel model;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ArtemisProducerStateListener.class);

	private final ServerLocator locator;
	private final ClientSessionFactory sessionFactory;
	private final String queueAddress;
	private final TProtocolFactory protocolFactory;

	private ClientSession session;
	private ClientProducer producer;

	public ArtemisProducerStateListener(HModel model, String queueAddress) throws Exception {
		this.model = model;
		this.queueAddress = queueAddress;
		this.locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(
						InVMConnectorFactory.class.getName()));
		this.sessionFactory = locator.createSessionFactory();
		this.protocolFactory = ThriftProtocol.JSON.getProtocolFactory();
	}

	@Override
	public void state(HawkState state) {
		sendState();
	}

	@Override
	public void info(String s) {
		sendState();
	}

	@Override
	public void error(String s) {
		sendState();
	}


	private void sendState() {
		openSession();

		HawkStateEvent ev = new HawkStateEvent();
		ev.setTimestamp(System.currentTimeMillis());
		ev.setMessage(model.getInfo());
		ev.setState(mapHawkStateToThrift(model.getStatus()));

		sendEvent(ev);
	}

	private void sendEvent(HawkStateEvent change) {
		try {
			final ClientMessage msg = session.createMessage(Message.BYTES_TYPE, false);
			final TTransport trans = new ActiveMQBufferTransport(msg.getBodyBuffer());
			final TProtocol proto = protocolFactory.getProtocol(trans);
			change.write(proto);

			producer.send(msg);
		} catch (TException ex) {
			LOGGER.error("Serialization error", ex);
		} catch (ActiveMQException ex) {
			LOGGER.error("Error while sending event", ex);
		}
	}

	private void openSession() {
		if (session == null || session.isClosed()) {
			try {
				this.session = sessionFactory.createSession();
				this.producer = session.createProducer(queueAddress);
			} catch (ActiveMQException e) {
				LOGGER.error("Could not start a new Artemis session", e);
			}
		}
	}

	private void closeSession() {
		try {
			if (producer != null) {
				producer.close();
			}
			if (session != null) {
				session.close();
			}
		} catch (ActiveMQException e) {
			LOGGER.error("Could not close the session", e);
		} finally {
			session = null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArtemisProducerStateListener other = (ArtemisProducerStateListener) obj;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		return true;
	}

	@Override
	public void removed() {
		closeSession();
	}

	public static uk.ac.york.mondo.integration.api.HawkState mapHawkStateToThrift(IStateListener.HawkState state) {
		switch (state) {
		case RUNNING: return uk.ac.york.mondo.integration.api.HawkState.RUNNING;
		case UPDATING: return uk.ac.york.mondo.integration.api.HawkState.UPDATING;
		default: return uk.ac.york.mondo.integration.api.HawkState.STOPPED;
		}
	}

	public static IStateListener.HawkState mapThriftStateToHawk(uk.ac.york.mondo.integration.api.HawkState state) {
		switch (state) {
		case RUNNING: return IStateListener.HawkState.RUNNING;
		case UPDATING: return IStateListener.HawkState.UPDATING;
		default: return IStateListener.HawkState.STOPPED;
		}
	}
}
