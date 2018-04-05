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
package org.hawk.service.servlet.artemis;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.core.runtime.CompositeGraphChangeListener;
import org.hawk.service.api.CommitItem;
import org.hawk.service.api.CommitItemChangeType;
import org.hawk.service.api.HawkAttributeRemovalEvent;
import org.hawk.service.api.HawkAttributeUpdateEvent;
import org.hawk.service.api.HawkChangeEvent;
import org.hawk.service.api.HawkFileAdditionEvent;
import org.hawk.service.api.HawkFileRemovalEvent;
import org.hawk.service.api.HawkModelElementAdditionEvent;
import org.hawk.service.api.HawkModelElementRemovalEvent;
import org.hawk.service.api.HawkReferenceAdditionEvent;
import org.hawk.service.api.HawkReferenceRemovalEvent;
import org.hawk.service.api.HawkSynchronizationEndEvent;
import org.hawk.service.api.HawkSynchronizationStartEvent;
import org.hawk.service.api.SubscriptionDurability;
import org.hawk.service.api.utils.ActiveMQBufferTransport;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.servlet.utils.HawkModelElementEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hawk change listener that sends all changes to the specified address within
 * an Artemis in-VM server through its core protocol. Transaction management is
 * based on the {@link TransactionalSendTest} test suite in Artemis.
 *
 * This implementation redefines hashCode and equals based on the computed
 * address, so "duplicate" listeners that would result in duplicate events being
 * sent to the destination address are implicitly avoided by the
 * {@link CompositeGraphChangeListener} in most indexers.
 */
public class ArtemisProducerGraphChangeListener implements IGraphChangeListener {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ArtemisProducerGraphChangeListener.class);

	private final ServerLocator locator;
	private final ClientSessionFactory sessionFactory;
	private final boolean messagesAreDurable;
	private final String queueAddress;
	private final TProtocolFactory protocolFactory;

	private ClientSession session;
	private ClientProducer producer;

	private final Pattern repositoryURIPattern, filePathPattern;

	/**
	 * Simple interface for objects that when called, create a HawkChangeEvent.
	 * Used to delay the creation of an Artemis message until we know the real
	 * ID for that graph node.
	 */
	private interface HawkChangeEventFactory {
		HawkChangeEvent create();
	}

	/**
	 * We need to collect events until we complete a transaction, as the graph
	 * nodes might only have temporary IDs until then.
	 */
	private final List<HawkChangeEventFactory> collectedEventFactories = new ArrayList<>();

	// True if the current session was opened from synchronizeStart
	private boolean isSessionOpenedFromSync = false;

	public ArtemisProducerGraphChangeListener(String hawkInstance,
			String repositoryUri, List<String> filePaths,
			SubscriptionDurability durability, ThriftProtocol protocol)
			throws Exception {
		// Convert the repository URI and file paths into regexps, for faster
		// matching
		this.repositoryURIPattern = Pattern.compile(repositoryUri.replace("*",
				".*"));
		StringBuffer sbuf = new StringBuffer();
		boolean first = true;
		for (String filePath : filePaths) {
			if (first) {
				first = false;
			} else {
				sbuf.append("|");
			}
			sbuf.append(filePath.replace("*", ".*"));
		}
		this.filePathPattern = Pattern.compile(sbuf.toString());

		// Thrift protocol factory (for encoding the events)
		this.protocolFactory = protocol.getProtocolFactory();

		// Connect to Artemis (use compression for messages above 10KB)
		this.locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(
						InVMConnectorFactory.class.getName()));
		locator.setCompressLargeMessage(true);
		locator.setMinLargeMessageSize(10_240);

		this.sessionFactory = locator.createSessionFactory();
		this.messagesAreDurable = durability == SubscriptionDurability.DURABLE;
		this.queueAddress = String.format("hawk.graphchanges.%s.%s.%s.%s.%s",
				hawkInstance, protocol.toString().toLowerCase(), repositoryUri
						.hashCode(), filePaths.hashCode(), durability
						.toString().toLowerCase());
	}

	public String getQueueAddress() {
		return queueAddress;
	}

	@Override
	public String getName() {
		return "Artemis graph change listener";
	}

	@Override
	public void synchroniseStart() {
		openSession();
		isSessionOpenedFromSync = true;

		final HawkSynchronizationStartEvent ev = new HawkSynchronizationStartEvent(System.nanoTime());
		final HawkChangeEvent change = new HawkChangeEvent();
		change.setSyncStart(ev);
		sendEvent(change);
		try {
			session.commit();
		} catch (ActiveMQException e) {
			LOGGER.error("Could not commit the synchronisation start event", e);
		}
	}

	@Override
	public void synchroniseEnd() {
		try {
			final HawkSynchronizationEndEvent ev = new HawkSynchronizationEndEvent(System.nanoTime());
			final HawkChangeEvent change = new HawkChangeEvent();
			change.setSyncEnd(ev);
			sendEvent(change);
			session.commit();
		} catch (ActiveMQException e) {
			LOGGER.error("Could not commit the transaction", e);
		} finally {
			closeSession();
		}
	}

	@Override
	public void changeStart() {
		collectedEventFactories.clear();
		openSession();
	}

	@Override
	public void changeSuccess() {
		try {
			for (HawkChangeEventFactory eventFactory : collectedEventFactories) {
				sendEvent(eventFactory.create());
			}
			session.commit();
		} catch (ActiveMQException e) {
			LOGGER.error("Could not commit the transaction", e);
			try {
				session.rollback();
			} catch (ActiveMQException e1) {
				LOGGER.error("Could not rollback the transaction", e1);
			}
		} finally {
			collectedEventFactories.clear();
			if (!isSessionOpenedFromSync) {
				closeSession();
			}
		}
	}

	@Override
	public void changeFailure() {
		try {
			session.rollback();
			LOGGER.debug("Session rolled back");
		} catch (ActiveMQException e) {
			LOGGER.error("Could not rollback the transaction", e);
		} finally {
			collectedEventFactories.clear();
			if (!isSessionOpenedFromSync) {
				closeSession();
			}
		}
	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {
		// nothing to do!
	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {
		// nothing to do!
	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {
		if (!isAcceptedByFilter(s))
			return;

		final HawkFileAdditionEvent ev = new HawkFileAdditionEvent();
		ev.setVcsItem(mapToThrift(s));
		final HawkChangeEvent change = new HawkChangeEvent();
		change.setFileAddition(ev);

		collectedEventFactories.add(new HawkChangeEventFactory() {
			@Override
			public HawkChangeEvent create() {
				return change;
			}
		});
	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {
		if (!isAcceptedByFilter(s))
			return;

		final HawkFileRemovalEvent ev = new HawkFileRemovalEvent();
		ev.setVcsItem(mapToThrift(s));
		final HawkChangeEvent change = new HawkChangeEvent();
		change.setFileRemoval(ev);

		collectedEventFactories.add(new HawkChangeEventFactory() {
			@Override
			public HawkChangeEvent create() {
				return change;
			}
		});
	}

	@Override
	public void modelElementAddition(final VcsCommitItem s, final IHawkObject element, final IGraphNode elementNode,
			boolean isTransient) {
		if (isTransient || !isAcceptedByFilter(s))
			return;

		try {
			collectedEventFactories.add(new HawkChangeEventFactory() {
				@Override
				public HawkChangeEvent create() {
					final HawkModelElementAdditionEvent ev = new HawkModelElementAdditionEvent();
					ev.setVcsItem(mapToThrift(s));
					ev.setMetamodelURI(element.getType().getPackageNSURI());
					ev.setTypeName(element.getType().getName());
					ev.setId(elementNode.getId().toString());

					final HawkChangeEvent change = new HawkChangeEvent();
					change.setModelElementAddition(ev);
					return change;
				}
			});
		} catch (Exception e) {
			LOGGER.error("Could not encode a model element", e);
		}
	}

	@Override
	public void modelElementRemoval(final VcsCommitItem s, final IGraphNode elementNode, boolean isTransient) {
		if (isTransient || !isAcceptedByFilter(s))
			return;

		collectedEventFactories.add(new HawkChangeEventFactory() {
			@Override
			public HawkChangeEvent create() {
				final HawkModelElementRemovalEvent ev = new HawkModelElementRemovalEvent();
				ev.setVcsItem(mapToThrift(s));
				ev.setId(elementNode.getId().toString());

				final HawkChangeEvent change = new HawkChangeEvent();
				change.setModelElementRemoval(ev);
				return change;
			}
		});
	}

	@Override
	public void modelElementAttributeUpdate(final VcsCommitItem s, final IHawkObject eObject, final String attrName,
			final Object oldValue, final Object newValue, final IGraphNode elementNode, boolean isTransient) {
		if (isTransient || !isAcceptedByFilter(s))
			return;

		collectedEventFactories.add(new HawkChangeEventFactory() {
			@Override
			public HawkChangeEvent create() {
				final HawkAttributeUpdateEvent ev = new HawkAttributeUpdateEvent();
				ev.setAttribute(attrName);
				ev.setId(elementNode.getId().toString());
				ev.setValue(HawkModelElementEncoder.encodeAttributeSlot(attrName, newValue).value);
				ev.setVcsItem(mapToThrift(s));

				final HawkChangeEvent change = new HawkChangeEvent();
				change.setModelElementAttributeUpdate(ev);
				return change;
			}
		});
	}

	@Override
	public void modelElementAttributeRemoval(final VcsCommitItem s, final IHawkObject eObject, final String attrName,
			final IGraphNode elementNode, boolean isTransient) {
		if (isTransient || !isAcceptedByFilter(s))
			return;

		collectedEventFactories.add(new HawkChangeEventFactory() {
			@Override
			public HawkChangeEvent create() {
				final HawkAttributeRemovalEvent ev = new HawkAttributeRemovalEvent();
				ev.setAttribute(attrName);
				ev.setId(elementNode.getId().toString());
				ev.setVcsItem(mapToThrift(s));

				final HawkChangeEvent change = new HawkChangeEvent();
				change.setModelElementAttributeRemoval(ev);
				return change;
			}
		});
	}

	@Override
	public void referenceAddition(final VcsCommitItem s, final IGraphNode source, final IGraphNode target,
			final String refName, boolean isTransient) {
		if (isTransient || !isAcceptedByFilter(s))
			return;

		collectedEventFactories.add(new HawkChangeEventFactory() {
			@Override
			public HawkChangeEvent create() {
				final HawkReferenceAdditionEvent ev = new HawkReferenceAdditionEvent();
				ev.setSourceId(source.getId().toString());
				ev.setTargetId(target.getId().toString());
				ev.setVcsItem(mapToThrift(s));
				ev.setRefName(refName);

				final HawkChangeEvent change = new HawkChangeEvent();
				change.setReferenceAddition(ev);
				return change;
			}
		});
	}

	@Override
	public void referenceRemoval(final VcsCommitItem s, final IGraphNode source, final IGraphNode target,
			final String refName, boolean isTransient) {
		if (isTransient || !isAcceptedByFilter(s))
			return;

		collectedEventFactories.add(new HawkChangeEventFactory() {
			@Override
			public HawkChangeEvent create() {
				final HawkReferenceRemovalEvent ev = new HawkReferenceRemovalEvent();
				ev.setSourceId(source.getId().toString());
				ev.setTargetId(target.getId().toString());
				ev.setVcsItem(mapToThrift(s));
				ev.setRefName(refName);

				final HawkChangeEvent change = new HawkChangeEvent();
				change.setReferenceRemoval(ev);
				return change;
			}
		});
	}

	private boolean isAcceptedByFilter(VcsCommitItem s) {
		final VcsCommit commit = s.getCommit();
		final VcsRepositoryDelta delta = commit.getDelta();
		final String repositoryURL = delta.getManager().getLocation();
		return repositoryURIPattern.matcher(repositoryURL).matches()
				&& filePathPattern.matcher(s.getPath()).matches();
	}

	private CommitItem mapToThrift(VcsCommitItem s) {
		final VcsCommit commit = s.getCommit();

		final String repoURL = commit.getDelta().getManager().getLocation();
		final String revision = commit.getRevision();
		final String path = s.getPath();
		final CommitItemChangeType changeType = mapToThrift(s.getChangeType());

		return new CommitItem(repoURL, revision, path, changeType);
	}

	private CommitItemChangeType mapToThrift(VcsChangeType changeType) {
		switch (changeType) {
		case ADDED:
			return CommitItemChangeType.ADDED;
		case DELETED:
			return CommitItemChangeType.DELETED;
		case REPLACED:
			return CommitItemChangeType.REPLACED;
		case UPDATED:
			return CommitItemChangeType.UPDATED;
		default:
			return CommitItemChangeType.UNKNOWN;
		}
	}

	private void sendEvent(HawkChangeEvent change) {
		try {
			final ClientMessage msg = session.createMessage(Message.BYTES_TYPE,
					messagesAreDurable);
			final TTransport trans = new ActiveMQBufferTransport(
					msg.getBodyBuffer());
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
				this.session = sessionFactory.createSession(false, false, false);
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
			isSessionOpenedFromSync = false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((queueAddress == null) ? 0 : queueAddress.hashCode());
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
		ArtemisProducerGraphChangeListener other = (ArtemisProducerGraphChangeListener) obj;
		if (queueAddress == null) {
			if (other.queueAddress != null)
				return false;
		} else if (!queueAddress.equals(other.queueAddress))
			return false;
		return true;
	}

	@Override
	public void setModelIndexer(IModelIndexer m) {
		// not used

	}
}
