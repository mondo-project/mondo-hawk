/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.emf.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.http.auth.Credentials;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.DynamicEStoreEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.hawk.core.query.IQueryEngine;
import org.hawk.emfresource.HawkResource;
import org.hawk.emfresource.HawkResourceChangeListener;
import org.hawk.emfresource.impl.HawkFileResourceImpl;
import org.hawk.emfresource.impl.LocalHawkResourceImpl;
import org.hawk.emfresource.util.LazyEObjectFactory;
import org.hawk.emfresource.util.LazyResolver;
import org.hawk.service.api.AttributeSlot;
import org.hawk.service.api.ContainerSlot;
import org.hawk.service.api.EffectiveMetamodelRuleset;
import org.hawk.service.api.FailedQuery;
import org.hawk.service.api.Hawk;
import org.hawk.service.api.Hawk.Client;
import org.hawk.service.api.HawkAttributeRemovalEvent;
import org.hawk.service.api.HawkAttributeUpdateEvent;
import org.hawk.service.api.HawkChangeEvent;
import org.hawk.service.api.HawkFileAdditionEvent;
import org.hawk.service.api.HawkFileRemovalEvent;
import org.hawk.service.api.HawkInstanceNotFound;
import org.hawk.service.api.HawkInstanceNotRunning;
import org.hawk.service.api.HawkModelElementAdditionEvent;
import org.hawk.service.api.HawkModelElementRemovalEvent;
import org.hawk.service.api.HawkQueryOptions;
import org.hawk.service.api.HawkReferenceAdditionEvent;
import org.hawk.service.api.HawkReferenceRemovalEvent;
import org.hawk.service.api.HawkSynchronizationEndEvent;
import org.hawk.service.api.HawkSynchronizationStartEvent;
import org.hawk.service.api.InvalidQuery;
import org.hawk.service.api.MixedReference;
import org.hawk.service.api.ModelElement;
import org.hawk.service.api.ModelElementType;
import org.hawk.service.api.QueryResult;
import org.hawk.service.api.QueryResult._Fields;
import org.hawk.service.api.ReferenceSlot;
import org.hawk.service.api.Subscription;
import org.hawk.service.api.SubscriptionDurability;
import org.hawk.service.api.UnknownQueryLanguage;
import org.hawk.service.api.utils.APIUtils;
import org.hawk.service.api.utils.ActiveMQBufferTransport;
import org.hawk.service.artemis.consumer.Consumer;
import org.hawk.service.emf.HawkModelDescriptor;
import org.hawk.service.emf.HawkModelDescriptor.LoadingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * EMF driver that reads a remote model from a Hawk index. This is the main resource
 * for the <code>.hawkmodel</code> file: during its loading, it will create several
 * {@link HawkFileResourceImpl} instances that will contain the model elements that
 * belong to each file in the Hawk index.
 *
 * TODO: preserve URI fragments (only supported in {@link LocalHawkResourceImpl} at the moment).
 */
public class HawkResourceImpl extends ResourceImpl implements HawkResource {

	public static final String EOL_QUERY_LANG = "org.hawk.epsilon.emc.EOLQueryEngine";

	/**
	 * Internal state used only while loading a tree of {@link ModelElement}s. It's
	 * kept separate so Java can reclaim the memory as soon as we're done with that
	 * tree.
	 */
	private final class TreeLoadingState {
		public String lastTypename, lastMetamodelURI, lastRepository, lastFile;

		// Only for the initial load (allEObjects is cleared afterwards)
		public final List<EObject> allEObjects = new ArrayList<>();

		// Only until references are filled in
		public final Map<ModelElement, EObject> meToEObject = new IdentityHashMap<>();
	}

	private final class HawkResourceMessageHandler implements MessageHandler {
		private long lastSyncStart;
		private final TProtocolFactory protocolFactory;

		public HawkResourceMessageHandler(final TProtocolFactory protocolFactory) {
			this.protocolFactory = protocolFactory;
		}

		@Override
		public void onMessage(final ClientMessage message) {
			try {
				final TProtocol proto = protocolFactory.getProtocol(new ActiveMQBufferTransport(message.getBodyBuffer()));
				final HawkChangeEvent change = new HawkChangeEvent();
				try {
					change.read(proto);

					// Artemis uses a pool of threads to receive messages: we need to serialize
					// the accesses to avoid race conditions between 'model element added' and
					// 'attribute changed', for instance.
					synchronized (nodeIdToEObjectMap) {
						LOGGER.debug("Received message from Artemis at {}: {}", message.getAddress(), change);

						if (change.isSetModelElementAttributeUpdate()) {
							handle(change.getModelElementAttributeUpdate());
						}
						else if (change.isSetModelElementAttributeRemoval()) {
							handle(change.getModelElementAttributeRemoval());
						}
						else if (change.isSetModelElementAddition()) {
							handle(change.getModelElementAddition());
						}
						else if (change.isSetModelElementRemoval()) {
							handle(change.getModelElementRemoval());
						}
						else if (change.isSetReferenceAddition()) {
							handle(change.getReferenceAddition());
						}
						else if (change.isSetReferenceRemoval()) {
							handle(change.getReferenceRemoval());
						}
						else if (change.isSetSyncStart()) {
							handle(change.getSyncStart());
						}
						else if (change.isSetSyncEnd()) {
							handle(change.getSyncEnd());
						}
						else if (change.isSetFileAddition()) {
							handle(change.getFileAddition());
						}
						else if (change.isSetFileRemoval()) {
							handle(change.getFileRemoval());
						}
					}
				} catch (final Throwable e) {
					LOGGER.error("Error while handling incoming message", e);
				}

				message.acknowledge();
			} catch (final ActiveMQException e) {
				LOGGER.error("Failed to ack message", e);
			}
		}

		@SuppressWarnings("unchecked")
		private void handle(final HawkReferenceRemovalEvent ev) {
			final EObject source = nodeIdToEObjectMap.get(ev.sourceId);
			final EObject target = nodeIdToEObjectMap.get(ev.targetId);
			if (source != null) {
				final EReference ref = (EReference)source.eClass().getEStructuralFeature(ev.refName);
				if (!ref.isChangeable()) {
					// we don't want to invoke eGet on unchangeable or pending references/attributes
					return;
				}

				if (lazyResolver != null && lazyResolver.isLazy((EObject)source, ref)) {
					if (!lazyResolver.removeFromLazyReference(source, ref, ev.targetId) && target != null) {
						lazyResolver.removeFromLazyReference(source, ref, target);
					}
					if (target != null) {
						featureDeleted(source, ref, target);
					} else if (!changeListeners.isEmpty()) {
						try {
							EList<EObject> resolved = fetchNodes(Arrays.asList(ev.targetId), false);
							if (resolved.isEmpty()) {
								LOGGER.warn("Could not notify listeners about deleted reference from {} to {}", ev.sourceId, ev.targetId);
							} else {
								featureDeleted(source, ref, resolved.get(0));
							}
						} catch (TException | IOException e) {
							LOGGER.error("Could not resolve removal", e.getMessage());
						}
					}
				}
				else if (target != null) {
					if (!ref.getEType().isInstance(target)) {
						throw new IllegalArgumentException(
							String.format("The target node %s is a %s, not an instance of %s", ev.targetId,
									target.eClass().getName(), ref.getEType().getName()));
					}

					if (ref.isMany()) {
						final Collection<EObject> objs = (Collection<EObject>)source.eGet(ref);
						objs.remove(target);
					} else {
						source.eUnset(ref);
					}

					featureDeleted(source, ref, target);
				}
			}
		}

		private void handle(final HawkReferenceAdditionEvent ev) {
			final EObject source = nodeIdToEObjectMap.get(ev.sourceId);
			EObject target = nodeIdToEObjectMap.get(ev.targetId);
			if (source != null) {
				final EReference ref = (EReference)source.eClass().getEStructuralFeature(ev.refName);
				if (!ref.isChangeable()) {
					// we don't want to invoke eGet on unchangeable references/attributes.
					return;
				}

				if (lazyResolver != null && lazyResolver.isLazy((EObject) source, ref)) {
					handleLazyReferenceAddition(ev, source, target, ref);
				} else if (isLazyLoading() && changeListeners.isEmpty() && !source.eIsSet(ref)) {
					// Nobody is listening, we're in lazy loading mode and the reference wasn't set
					// yet, so we can turn it into a lazy reference for now. This is useful when we
					// add a new model element through a notification: the following notifications
					// may initialize its references to a mix of things we have and things we don't
					// have yet.
					final BasicEList<Object> newList = new BasicEList<>();
					newList.add(ev.targetId);
					getLazyResolver().putLazyReference(source, ref, newList);
				} else {
					try {
						handleNonlazyReferenceAddition(ev, source, target, ref);
					} catch (Exception ex) {
						LOGGER.error("Exception while handling non-lazy addition of reference {} from {} to {}", ref,
								ev.sourceId, ev.targetId);
					}
				}
			} else {
				LOGGER.debug("Source of reference {} from {} to {} was not available", ev.refName, ev.sourceId, ev.targetId);
			}
		}

		@SuppressWarnings("unchecked")
		private void handleNonlazyReferenceAddition(final HawkReferenceAdditionEvent ev, final EObject source,
				EObject target, final EReference ref)
						throws HawkInstanceNotFound, HawkInstanceNotRunning, TException, IOException {
			// We need the target object *now*, even if we don't have it yet.
			if (target == null) {
				EList<EObject> lResolved = fetchNodes(Arrays.asList(ev.targetId), false);
				if (lResolved.isEmpty()) {
					LOGGER.warn("Target not found for non-lazy reference {} from node {} to node {}", ref,
							ev.sourceId, ev.targetId);
					return;
				}
				target = lResolved.get(0);
			}

			if (!ref.getEType().isInstance(target)) {
				throw new IllegalArgumentException(
						String.format("The target node %s is a %s, not an instance of %s", ev.targetId,
								target.eClass().getName(), ref.getEType().getName()));
			}

			if (ref.isMany()) {
				final Collection<EObject> objs = (Collection<EObject>) source.eGet(ref);
				objs.add(target);
			} else {
				source.eSet(ref, target);
			}

			if (ref.isContainer()) {
				source.eResource().getContents().remove(source);
			} else if (ref.isContainment()) {
				target.eResource().getContents().remove(target);
			}

			LOGGER.debug("Added {} to non-lazy ref {} of {}", target, ref.getName(), source);
			featureInserted(source, ref, target);
		}

		private void handleLazyReferenceAddition(final HawkReferenceAdditionEvent ev, final EObject source,
				final EObject target, final EReference ref) {
			if (target != null) {
				// We already have the target, so might as well add it now.
				lazyResolver.addToLazyReference(source, ref, target);
				featureInserted(source, ref, target);
			} else if (!changeListeners.isEmpty()) {
				// We don't have the target, but we have someone listening, so we must resolve the ID *now* so we
				// can notify them with the right EObject.
				try {
					EList<EObject> resolved = fetchNodes(Arrays.asList(ev.targetId), false);
					if (resolved.isEmpty()) {
						lazyResolver.addToLazyReference(source, ref, ev.targetId);
						LOGGER.warn("Target node does not exist: could not notify listeners about inserted reference from {} to {}", ev.sourceId, ev.targetId);
					} else {
						final EObject firstResolved = resolved.get(0);
						lazyResolver.addToLazyReference(source, ref, firstResolved);
						featureInserted(source, ref, firstResolved);
					}
				} catch (TException | IOException e) {
					LOGGER.error("Could not resolve addition", e.getMessage());
				}
			} else {
				// We don't have the target and nobody is listening: just note it for our lazy references.
				lazyResolver.addToLazyReference(source, ref, ev.targetId);
			}
		}

		@SuppressWarnings("unchecked")
		private void handle(final HawkModelElementRemovalEvent ev) {
			final EObject eob = nodeIdToEObjectMap.remove(ev.id);
			if (eob != null) {
				synchronized (classToEObjectsMap) {
					final EList<EObject> instances = classToEObjectsMap.get(eob.eClass());
					if (instances != null) {
						instances.remove(eob);
					}
				}
				if (eob.eResource() != null) {
					eob.eResource().getContents().remove(eob);
				}

				final EObject container = eob.eContainer();
				if (container != null) {
					final EStructuralFeature containingFeature = eob.eContainingFeature();
					if (containingFeature.isMany()) {
						((Collection<EObject>)container.eGet(containingFeature)).remove(eob);
					} else {
						container.eUnset(containingFeature);
					}
				}

				instanceDeleted(eob, eob.eClass());
			}
		}

		private void handle(final HawkModelElementAdditionEvent ev) {
			final Registry registry = getResourceSet().getPackageRegistry();
			final EClass eClass = HawkResourceImpl.getEClass(ev.metamodelURI, ev.typeName, registry);
			final EObject existing = nodeIdToEObjectMap.get(ev.id);
			if (existing == null) {
				final EObject eob = createInstance(eClass);
				nodeIdToEObjectMap.put(ev.id, eob);
				synchronized(classToEObjectsMap) {
					final EList<EObject> instances = classToEObjectsMap.get(eClass);
					if (instances != null) {
						instances.add(eob);
					}
				}
				addToResource(ev.vcsItem.repoURL, ev.vcsItem.path, eob);
				LOGGER.debug("Added a new {} with ID {}", eob.eClass().getName(), ev.id);
				instanceInserted(eob, eob.eClass());
			} else {
				LOGGER.warn("We already have a {} with ID {}: cannot create a {} there", existing.eClass().getName(), ev.id, eClass.getName());
			}
		}

		private void handle(final HawkAttributeRemovalEvent ev) {
			final EObject eob = nodeIdToEObjectMap.get(ev.getId());
			if (eob != null) {
				final EStructuralFeature eAttr = eob.eClass().getEStructuralFeature(ev.attribute);
				if (!changeListeners.isEmpty()) {
					// We want to avoid the eob.eGet call if nobody is listening,
					// because it could trigger network communication in LAZY_ATTRIBUTES mode.
					final Object oldValue = eob.eGet(eAttr);
					notifyAttributeUnset(eob, eAttr, oldValue);
				}

				if (eAttr != null) {
					eob.eUnset(eAttr);
					if (!changeListeners.isEmpty() && eAttr.getDefaultValue() != null) {
						notifyAttributeSet(eob, eAttr, eAttr.getDefaultValue());
					}
				}
			}
		}

		private void handle(final HawkAttributeUpdateEvent ev) {
			final EObject eob = nodeIdToEObjectMap.get(ev.getId());
			if (eob != null) {
				final EClass eClass = eob.eClass();
				final AttributeSlot slot = new AttributeSlot(ev.attribute);
				slot.setValue(ev.value);
				try {
					final EStructuralFeature eAttr = eClass.getEStructuralFeature(ev.attribute);
					if (!changeListeners.isEmpty()) {
						// We want to avoid the eob.eGet call if nobody is listening,
						// because it could trigger network communication in LAZY_ATTRIBUTES mode.
						notifyAttributeUnset(eob, eAttr, eob.eGet(eAttr));
					}

					final Object newValue = SlotDecodingUtils.setFromSlot(eClass.getEPackage().getEFactoryInstance(), eClass, eob, slot);
					notifyAttributeSet(eob, eAttr, newValue);
				} catch (final IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			} else {
				LOGGER.debug("EObject for ID {} not found when handling attribute update", ev.getId());
			}
		}

		private void handle(final HawkSynchronizationStartEvent syncStart) {
			this.lastSyncStart = syncStart.getTimestampNanos();
			LOGGER.debug("Sync started: local timestamp is {} ns", lastSyncStart);
		}

		private void handle(final HawkSynchronizationEndEvent syncEnd) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Sync ended: local timestamp is {} ns (elapsed time: {} ms)",
						syncEnd.getTimestampNanos(),
						(syncEnd.getTimestampNanos() - lastSyncStart)/1_000_000.0);
			}

			// We commit acknowledgements after synchronization is done:
			// we only have one of these per set of changes.
			try {
				subscriber.commitSession();
			} catch (final ActiveMQException e) {
				LOGGER.error("Could not commit client session", e);
			}

			for (final Runnable r : syncEndListeners) {
				try {
					r.run();
				} catch (final Throwable t) {
					LOGGER.error("Error while executing sync end listener", t);
				}
			}
		}

		private void handle(final HawkFileAdditionEvent ev) {
			// we don't really have to do anything here - delay the
			// addition of the resource until we have something to put
			// in there.
		}

		private void handle(final HawkFileRemovalEvent ev) {
			// Remove the associated resource from the resource set, if we have it
			final String fullUrl = computeFileResourceURL(ev.vcsItem.repoURL, ev.vcsItem.path);

			synchronized (resources) {
				final Resource r = resources.remove(fullUrl);
				if (r != null) {
					r.unload();
					getResourceSet().getResources().remove(r);
				}
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(HawkResourceImpl.class);

	private static EClass getEClass(final String metamodelUri, final String typeName,
			final Registry packageRegistry) {
		final EPackage pkg = packageRegistry.getEPackage(metamodelUri);
		if (pkg == null) {
			throw new NoSuchElementException(String.format(
					"Could not find EPackage with URI '%s' in the registry",
					metamodelUri));
		}

		final EClassifier eClassifier = pkg.getEClassifier(typeName);
		if (!(eClassifier instanceof EClass)) {
			throw new NoSuchElementException(String.format(
					"Received an element of type '%s', which is not an EClass",
					eClassifier));
		}
		final EClass eClass = (EClass) eClassifier;
		return eClass;
	}

	private final BiMap<String, EObject> nodeIdToEObjectMap = HashBiMap.create();
	private final Map<String, HawkFileResourceImpl> resources = new HashMap<>();
	private HawkModelDescriptor descriptor;
	private Client client;

	/** Consumer for notifications coming from Artemis. */
	private Consumer subscriber;

	/** Resolves lazy references and attributes. */
	private LazyResolver lazyResolver;

	/** Interceptor to be reused by all CGLIB {@link Enhancer}s in lazy loading modes. */
	private LazyEObjectFactory eobFactory;

	/**
	 * Cache from class to its instances. New entries are added upon calls to
	 * {@link #fetchNodes(EClass)}, which are then kept up to date during
	 * notifications and lazy loads.
	 */
	private final Map<EClass, EList<EObject>> classToEObjectsMap = new HashMap<>();

	/** Collection of runnables that should be invoked when a synchronisation is complete. */
	private final Set<Runnable> syncEndListeners = new HashSet<>();

	/** Collection of change listeners (mostly for the IncQuery integration). */
	private final Set<HawkResourceChangeListener> changeListeners = new HashSet<>();

	public HawkResourceImpl() {}

	public HawkResourceImpl(final URI uri, final HawkModelDescriptor descriptor) {
		// Even if we're not only to load anything from the URI (as we have a descriptor),
		// we still need it for proxy resolving (hawk+http URLs won't work from CloudATL
		// otherwise: for some reason, without an URI it cannot find EString, for instance).
		this(uri);
		this.descriptor = descriptor;
	}

	public HawkResourceImpl(final URI uri) {
		super(uri);
	}

	@Override
	public void load(final Map<?, ?> options) throws IOException {
		if (descriptor != null) {
			// We already have a descriptor: no need to create an InputStream from the URI
			doLoad(descriptor, new NullProgressMonitor());
		} else {
			// Let Ecore create an InputStream from the URI and call doLoad(InputStream, Map)
			super.load(options);
		}
	}

	@Override
	public void save(Map<?, ?> options) throws IOException {
		doSave(null, null);
	}

	public HawkModelDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean hasChildren(final EObject o) {
		for (final EReference r : o.eClass().getEAllReferences()) {
			if (r.isContainment()) {
				if (lazyResolver != null) {
					final EList<Object> pending = lazyResolver.getPending(o, r);
					if (pending != null) {
						return !pending.isEmpty();
					}
				}
				final Object v = o.eGet(r);
				if (r.isMany() && !((Collection<EObject>)v).isEmpty() || !r.isMany() && v != null) {
					return true;
				}
			}
		}
		return false;
	}

	public void doLoad(final HawkModelDescriptor descriptor, IProgressMonitor monitor) throws IOException {
		prepareResourceFactoryMap();

		try {
			this.descriptor = descriptor;
			monitor.subTask("Connecting to Hawk");
			final Credentials lazyCreds = connect(descriptor);

			// TODO allow for multiple repositories
			final LoadingMode mode = descriptor.getLoadingMode();

			final HawkQueryOptions opts = new HawkQueryOptions();
			opts.setDefaultNamespaces(descriptor.getDefaultNamespaces());
			opts.setRepositoryPattern(descriptor.getHawkRepository());
			opts.setFilePatterns(Arrays.asList(descriptor.getHawkFilePatterns()));
			opts.setIncludeAttributes(mode.isGreedyAttributes() && !descriptor.isPaged());
			opts.setIncludeReferences(!descriptor.isPaged());
			opts.setIncludeNodeIDs(isIncludeNodeIDs(descriptor));
			opts.setIncludeContained(mode.isGreedyElements() && !descriptor.isPaged());

			// Send the effective metamodel if included
			// TODO: use stateful variant to keep using effective metamodel in lazy modes?
			setEffectiveMetamodelOptions(opts, descriptor.getEffectiveMetamodel());

			// STAGE ONE: either full fetch or initial fetch of IDs (for a later paged fetch)
			monitor.subTask("Performing initial fetch");
			final List<ModelElement> elems = initialFetch(descriptor, mode, opts);

			// STAGE TWO: paged fetch+decoding or just simple decoding
			resolveContents(descriptor, mode, elems, opts, monitor);

			// Subscribe to changes through Artemis
			if (descriptor.isSubscribed()) {
				monitor.subTask("Subscribing to Artemis");
				subscribeToChanges(descriptor, lazyCreds);
			}

			setLoaded(true);
		} catch (final IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		} catch (final Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new IOException(e);
		}
	}

	protected void prepareResourceFactoryMap() {
		// Needed for the virtual resources we create for EMF files, so
		// the Exeed customizations can activate.
		final Factory hawkResourceFactory = new Factory() {
			@Override
			public Resource createResource(final URI uri) {
				return new HawkFileResourceImpl(uri, HawkResourceImpl.this);
			}
		};
		final Map<String, Object> protocolToFactoryMap = getResourceSet().getResourceFactoryRegistry().getProtocolToFactoryMap();
		protocolToFactoryMap.put("hawkrepo+file", hawkResourceFactory);
		protocolToFactoryMap.put("hawkrepo+http", hawkResourceFactory);
		protocolToFactoryMap.put("hawkrepo+https", hawkResourceFactory);
		protocolToFactoryMap.put("hawkrepo+git", hawkResourceFactory);
		protocolToFactoryMap.put("hawkrepo+svn", hawkResourceFactory);
		protocolToFactoryMap.put("hawkrepo+svn+ssh", hawkResourceFactory);
	}

	protected Credentials connect(final HawkModelDescriptor descriptor) throws TTransportException, URISyntaxException {
		final String username = descriptor.getUsername();
		final String password = descriptor.getPassword();
		Credentials lazyCreds = null;
		if (username != null && password != null && username.length() > 0 && password.length() > 0) {
			this.client = APIUtils.connectTo(Hawk.Client.class, descriptor.getHawkURL(),
					descriptor.getThriftProtocol(), username, password);
		} else {
			try {
				/*
				 * If we don't have explicit username/password but the
				 * remote.thrift plugin is available, we may be able to
				 * reuse previously stored usernames/passwords.
				 */
				Class<?> lCredClass = Class
						.forName("org.hawk.service.api.dt.http.LazyCredentials");
				lazyCreds = (org.apache.http.auth.Credentials) lCredClass.getConstructor(String.class)
						.newInstance(descriptor.getHawkURL());
				this.client = APIUtils.connectTo(Hawk.Client.class, descriptor.getHawkURL(),
						descriptor.getThriftProtocol(), lazyCreds);
			} catch (Exception ex) {
				// Falling back to non-auth
				this.client = APIUtils.connectTo(Hawk.Client.class, descriptor.getHawkURL(),
						descriptor.getThriftProtocol());
			}
		}
		return lazyCreds;
	}

	protected List<ModelElement> initialFetch(final HawkModelDescriptor descriptor, final LoadingMode mode,
			final HawkQueryOptions opts) throws HawkInstanceNotFound, HawkInstanceNotRunning, UnknownQueryLanguage,
					InvalidQuery, FailedQuery, TException {
		List<ModelElement> elems;
		final String queryLanguage = descriptor.getHawkQueryLanguage();
		final String query = descriptor.getHawkQuery();
		final boolean useQuery = queryLanguage != null && queryLanguage.length() > 0 && query != null && query.length() > 0;
		if (useQuery) {
			// Make sure we have a list, even if the original query did not produce a list
			QueryResult results = client.query(descriptor.getHawkInstance(), query, queryLanguage, opts);
			if (!results.isSetVList()) {
				results = new QueryResult(_Fields.V_LIST, results);
			}

			elems = new ArrayList<>();
			for (final QueryResult result : results.getVList()) {
				if (result.isSetVModelElement()) {
					elems.add(result.getVModelElement());
				}
			}
		} else if (mode.isGreedyElements()) {
			elems = client.getModel(descriptor.getHawkInstance(), opts);
		} else {
			elems = client.getRootElements(descriptor.getHawkInstance(), opts);
		}
		return elems;
	}

	protected void resolveContents(final HawkModelDescriptor descriptor, final LoadingMode mode,
			List<ModelElement> elems, final HawkQueryOptions opts, final IProgressMonitor monitor)
					throws HawkInstanceNotFound, HawkInstanceNotRunning, TException, IOException {
		if (descriptor.isPaged()) {
			// Two-stage fetch (ids + pages): first we fetch all the objects in batches,
			// then we resolve references internally (also in batches).
			opts.setIncludeAttributes(true);
			opts.setIncludeReferences(true);

			final int nElems = elems.size();
			for (int iRangeStart = 0; iRangeStart < nElems; iRangeStart += descriptor.getPageSize()) {
				final List<String> rangeIDs = new ArrayList<>(descriptor.getPageSize());
				final int iRangeEnd = Math.min(nElems, iRangeStart + descriptor.getPageSize());
				for (ModelElement elem : elems.subList(iRangeStart, iRangeEnd)) {
					rangeIDs.add(elem.getId());
				}

				// We create a temporary per-batch state to handle position-based refs: ID-based refs
				// are resolved lazily through the node IDs.
				monitor.subTask(String.format("Fetching model elements %d-%d out of %d", iRangeStart, iRangeEnd, nElems));
				final List<ModelElement> batch = client.resolveProxies(descriptor.getHawkInstance(), rangeIDs, opts);
				final TreeLoadingState state = new TreeLoadingState();
				createEObjectTree(batch, state);
				fillInReferences(batch, state);
			}

			// If we used a greedy mode, recheck containment for the still remaining root objects
			if (mode.isGreedyElements()) {
				monitor.subTask("Revising object containment");
				for (EObject eob : getContents()) {
					eob.eContainer();
				}
			}
		} else {
			monitor.subTask("Resolving contents");

			// Single-stage fetch: the usual way
			final TreeLoadingState state = new TreeLoadingState();
			createEObjectTree(elems, state);
			fillInReferences(elems, state);
		}
	}

	protected void subscribeToChanges(final HawkModelDescriptor descriptor, Credentials lazyCreds)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, TException, Exception, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, ActiveMQException {
		final SubscriptionDurability sd = descriptor.getSubscriptionDurability();

		final Subscription subscription = client.watchModelChanges(
			descriptor.getHawkInstance(),
			descriptor.getHawkRepository(),
			Arrays.asList(descriptor.getHawkFilePatterns()),
			descriptor.getSubscriptionClientID(), sd);

		subscriber = APIUtils.connectToArtemis(subscription, sd);
		Principal fetchedUser = null;
		if (lazyCreds != null) {
			// If security is disabled for the Thrift API, we do not want to trigger the secure storage here either.
			// These methods in the LazyCredentials class do just that.
			fetchedUser = (Principal) lazyCreds.getClass().getMethod("getRawUserPrincipal").invoke(lazyCreds);
		}
		if (fetchedUser != null) {
			final String fetchedPass = (String) lazyCreds.getClass().getMethod("getRawPassword").invoke(lazyCreds);
			subscriber.openSession(fetchedUser.getName(), fetchedPass);
		} else {
			subscriber.openSession(descriptor.getUsername(), descriptor.getPassword());
		}
		subscriber.processChangesAsync(new HawkResourceMessageHandler(
			descriptor.getThriftProtocol().getProtocolFactory()));
	}

	protected void setEffectiveMetamodelOptions(final HawkQueryOptions opts, final EffectiveMetamodelRuleset emm) {
		final Table<String, String, ImmutableSet<String>> inclusionRules = emm.getInclusionRules();
		final Table<String, String, ImmutableSet<String>> exclusionRules = emm.getExclusionRules();
		if (!inclusionRules.isEmpty()) {
			// The rowMap points to ImmutableSet<String>, which is compatible with
			// Set<String>, but the Java compiler is not smart enough to accept this.
			// Here we use a cast to work around this, which is cheaper than doing a
			// full copy.
			@SuppressWarnings({ "unchecked", "rawtypes" })
			final Map<String, Map<String, Set<String>>> inclusionMap = (Map) inclusionRules.rowMap();
			opts.setEffectiveMetamodelIncludes(inclusionMap);
		}
		if (!exclusionRules.isEmpty()) {
			// See comment above.
			@SuppressWarnings({ "unchecked", "rawtypes" })
			final Map<String, Map<String, Set<String>>> exclusionMap = (Map) exclusionRules.rowMap();
			opts.setEffectiveMetamodelExcludes(exclusionMap);
		}
	}

	protected boolean isIncludeNodeIDs(final HawkModelDescriptor descriptor) {
		final LoadingMode lm = descriptor.getLoadingMode();
		return !lm.isGreedyElements() || !lm.isGreedyAttributes() || descriptor.isSubscribed() || descriptor.isPaged();
	}

	@Override
	public EList<EObject> fetchNodes(final List<String> ids, boolean mustFetchAttributes)
			throws HawkInstanceNotFound, HawkInstanceNotRunning,
			TException, IOException {
		if (!isIncludeNodeIDs(getDescriptor())) {
			throw new IllegalArgumentException("Cannot fetch by ID: loading mode is " + descriptor.getLoadingMode());
		}

		// Filter the objects that need to be retrieved
		final List<String> toBeFetched = new ArrayList<>();
		for (final String id : ids) {
			if (!nodeIdToEObjectMap.containsKey(id)) {
				toBeFetched.add(id);
			}
		}

		// Fetch the eObjects, decode them and resolve references
		if (!toBeFetched.isEmpty()) {
			final HawkQueryOptions options = new HawkQueryOptions();
			options.setIncludeAttributes(descriptor.getLoadingMode().isGreedyAttributes() || mustFetchAttributes);
			options.setIncludeReferences(true);
			setEffectiveMetamodelOptions(options, descriptor.getEffectiveMetamodel());
			final List<ModelElement> elems = client.resolveProxies(
					descriptor.getHawkInstance(), toBeFetched,
					options);
			final TreeLoadingState state = new TreeLoadingState();
			createEObjectTree(elems, state);
			fillInReferences(elems, state);
		}

		// Rebuild the real EList now
		final EList<EObject> finalList = new BasicEList<EObject>(ids.size());
		for (final String id : ids) {
			final EObject eObject = nodeIdToEObjectMap.get(id);
			if (eObject != null) {
				finalList.add(eObject);
			}
		}
		return finalList;
	}

	@Override
	public EList<EObject> fetchNodes(final EClass eClass, boolean includeAttributes)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, TException, IOException {
		synchronized (classToEObjectsMap) {
			final EList<EObject> precomputed = classToEObjectsMap.get(eClass);
			if (precomputed != null) {
				return precomputed;
			}

			if (!isIncludeNodeIDs(getDescriptor())) {
				// IDs are not available, so do a full traversal and precompute allInstances
				// for every class while we're at it.
				final EList<EObject> computed = new BasicEList<EObject>();
				for (Resource r : getResourceSet().getResources()) {
					for (final Iterator<EObject> itEob = r.getAllContents(); itEob.hasNext(); ) {
						final EObject eob = itEob.next();
						final EClass ec = eob.eClass();
						if (eClass.isSuperTypeOf(ec)) {
							computed.add(eob);
						}
					}
				}
				classToEObjectsMap.put(eClass, computed);
				return computed;
			} else {
				// TODO: add "getInstancesOfType" to Hawk API instead of Hawk EOL query?
				final HawkQueryOptions opts = new HawkQueryOptions();
				opts.setDefaultNamespaces(eClass.getEPackage().getNsURI());
				opts.setRepositoryPattern(descriptor.getHawkRepository());
				opts.setFilePatterns(Arrays.asList(descriptor.getHawkFilePatterns()));
				opts.setIncludeAttributes(includeAttributes);
				setEffectiveMetamodelOptions(opts, descriptor.getEffectiveMetamodel());

				final String query = String.format("return %s.all;", eClass.getName());
				final EList<EObject> fetched = fetchByQuery(EOL_QUERY_LANG, query, opts);
				classToEObjectsMap.put(eClass, fetched);
				return fetched;
			}
		}
	}

	@Override
	public Object performRawQuery(String queryLanguage, String query, Map<String, Object> context) throws Exception {
		HawkQueryOptions options = new HawkQueryOptions();

		final String sFilePattern = (String) context.get(IQueryEngine.PROPERTY_FILECONTEXT);
		if (sFilePattern != null) {
			options.setFilePatterns(Arrays.asList(sFilePattern.split(",")));
		}
		final String sRepoPattern = (String) context.get(IQueryEngine.PROPERTY_REPOSITORYCONTEXT);
		if (sRepoPattern != null) {
			options.setRepositoryPattern(sRepoPattern);
		}
		final String sDefaultNamespaces = (String) context.get(IQueryEngine.PROPERTY_DEFAULTNAMESPACES);
		if (sDefaultNamespaces != null) {
			options.setDefaultNamespaces(sDefaultNamespaces);
		}

		return client.query(descriptor.getHawkInstance(), query, queryLanguage, options);
	}

	public EList<EObject> fetchByQuery(final String language, final String query, final HawkQueryOptions opts) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException, IOException {
		QueryResult typeInstanceIDs = client.query(descriptor.getHawkInstance(), query, language, opts);
		if (!typeInstanceIDs.isSetVList()) {
			typeInstanceIDs = new QueryResult(_Fields.V_LIST, typeInstanceIDs);
		}

		return fetchNodesByQueryResults(typeInstanceIDs.getVList(), opts.includeAttributes);
	}

	protected EList<EObject> fetchNodesByQueryResults(final List<QueryResult> typeInstanceIDs, boolean includeAttributes)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, TException, IOException {
		final List<String> ids = new ArrayList<>();
		for (final QueryResult qr : typeInstanceIDs) {
			ids.add(qr.getVModelElement().getId());
		}
		final EList<EObject> fetched = fetchNodes(ids, includeAttributes);
		return fetched;
	}

	@Override
	public List<Object> fetchValuesByEClassifier(final EClassifier dataType) throws HawkInstanceNotFound, HawkInstanceNotRunning, UnknownQueryLanguage, InvalidQuery, FailedQuery, TException, IOException {
		final Map<EClass, List<EStructuralFeature>> candidateTypes = fetchTypesWithEClassifier(dataType);

		final List<Object> values = new ArrayList<>();
		for (final Entry<EClass, List<EStructuralFeature>> entry : candidateTypes.entrySet()) {
			final EClass eClass = entry.getKey();
			final List<EStructuralFeature> featureWithType = entry.getValue();
			for (final EObject eob : fetchNodes(eClass, true)) {
				for (final EStructuralFeature attr : featureWithType) {
					final Object o = eob.eGet(attr);
					if (o != null) {
						values.add(o);
					}
				}
			}
		}

		return values;
	}

	@Override
	public Map<EClass, List<EStructuralFeature>> fetchTypesWithEClassifier(final EClassifier dataType)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, UnknownQueryLanguage, InvalidQuery, FailedQuery, TException {
		// TODO: add "getExistingTypes" to Hawk API instead of this EOL query?
		final HawkQueryOptions opts = new HawkQueryOptions();
		opts.setRepositoryPattern(descriptor.getHawkRepository());
		opts.setFilePatterns(Arrays.asList(descriptor.getHawkFilePatterns()));
		setEffectiveMetamodelOptions(opts, descriptor.getEffectiveMetamodel());
		final QueryResult typesWithInstances = client.query(descriptor.getHawkInstance(),
				"return Model.types.select(t|not t.all.isEmpty);",
				EOL_QUERY_LANG, opts);

		final Map<EClass, List<EStructuralFeature>> candidateTypes = new IdentityHashMap<EClass, List<EStructuralFeature>>();
		for (final QueryResult qr : typesWithInstances.getVList()) {
			final ModelElementType type = qr.getVModelElementType();
			final EClass eClass = getEClass(type.metamodelUri, type.typeName, getResourceSet().getPackageRegistry());

			final List<EStructuralFeature> attrsWithType = new ArrayList<>();
			for (final EStructuralFeature attr : eClass.getEAllAttributes()) {
				if (attr.getEType() == dataType) {
					attrsWithType.add(attr);
				}
			}
			if (attrsWithType.isEmpty()) {
				candidateTypes.put(eClass, attrsWithType);
			}
		}
		return candidateTypes;
	}

	@Override
	public Map<EObject, Object> fetchValuesByEStructuralFeature(final EStructuralFeature feature) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException, IOException {
		final EClass featureEClass = feature.getEContainingClass();
		final EList<EObject> eobs = fetchNodes(featureEClass, feature instanceof EAttribute);
		LOGGER.debug("Fetched {} nodes of class {}", eobs.size(), featureEClass.getName());

		if (lazyResolver != null && feature instanceof EReference) {
			// If the feature is a reference, collect all its pending nodes in advance
			final EReference ref = (EReference)feature;

			final List<String> allPending = new ArrayList<String>();
			for (final EObject eob : eobs) {
				EList<Object> pending = lazyResolver.getPending(eob, ref);
				if (pending == null) continue;
				for (Object p : pending) {
					if (p instanceof String) {
						allPending.add((String)p);
					}
				}
			}
			fetchNodes(allPending, false);
		}

		final Map<EObject, Object> values = new IdentityHashMap<>();
		for (final EObject eob : eobs) {
			final Object value = eob.eGet(feature);
			if (value != null) {
				values.put(eob, value);
			} else {
				LOGGER.debug("Value is null for feature {} of object {}", feature.getName(), eob);
			}
		}
		return values;
	}

	public void fetchAttributes(final Map<String, EObject> objects) throws IOException, HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HawkQueryOptions options = new HawkQueryOptions();
		options.setIncludeAttributes(true);
		options.setIncludeReferences(false);
		setEffectiveMetamodelOptions(options, descriptor.getEffectiveMetamodel());

		final List<ModelElement> elems = client.resolveProxies(
			descriptor.getHawkInstance(), new ArrayList<>(objects.keySet()),
			options);

		for (ModelElement me : elems) {
			final EObject object = objects.get(me.id);
			final EFactory eFactory = getResourceSet().getPackageRegistry().getEFactory(me.getMetamodelUri());
			final EClass eClass = getEClass(
					me.getMetamodelUri(), me.getTypeName(),
					getResourceSet().getPackageRegistry());
			for (final AttributeSlot s : me.attributes) {
				SlotDecodingUtils.setFromSlot(eFactory, eClass, object, s);
			}
		}
	}

	@Override
	public boolean addSyncEndListener(final Runnable r) {
		return syncEndListeners.add(r);
	}

	@Override
	public boolean removeSyncEndListener(final Runnable r) {
		return syncEndListeners.remove(r);
	}

	@Override
	public boolean addChangeListener(final HawkResourceChangeListener l) {
		return changeListeners.add(l);
	}

	@Override
	public boolean removeChangeListener(final HawkResourceChangeListener l) {
		return changeListeners.remove(l);
	}

	private void addToResource(final String repoURL, final String path, final EObject eob) {
		if (descriptor.isSplit()) {
			final String fullURL = computeFileResourceURL(repoURL, path);
			synchronized (resources) {
				HawkFileResourceImpl resource = resources.get(fullURL);
				if (resource == null) {
					resource = new HawkFileResourceImpl(URI.createURI(fullURL), this);
					getResourceSet().getResources().add(resource);
					resources.put(fullURL, resource);
				}
				resource.getContents().add(eob);
			}
		} else {
			getContents().add(eob);
		}
	}

	private String computeFileResourceURL(final String repoURL, final String path) {
		final String repoSep = repoURL.endsWith("/") ? "!!" : "/!!";
		final String pathSep = path.startsWith("/") ? "" : "/";
		return "hawkrepo+" + repoURL + repoSep + pathSep + path;
	}

	private EObject createEObject(final ModelElement me) throws IOException {
		final Registry registry = getResourceSet().getPackageRegistry();
		final EClass eClass = getEClass(me.metamodelUri, me.typeName, registry);
		final EObject obj = createInstance(eClass);

		if (me.isSetId()) {
			nodeIdToEObjectMap.put(me.id, obj);

			synchronized(classToEObjectsMap) {
				final EList<EObject> instances = classToEObjectsMap.get(eClass);
				if (instances != null) {
					instances.add(obj);
				}
			}
		}

		if (me.isSetAttributes()) {
			final EFactory factory = registry.getEFactory(eClass.getEPackage().getNsURI());
			for (final AttributeSlot s : me.attributes) {
				SlotDecodingUtils.setFromSlot(factory, eClass, obj, s);
			}
		} else if (!descriptor.getLoadingMode().isGreedyAttributes()) {
			getLazyResolver().putLazyAttributes(me.id, obj);
		}

		return obj;
	}

	private EObject createInstance(final EClass eClass) {
		final Registry packageRegistry = getResourceSet().getPackageRegistry();
		final EFactory factory = packageRegistry.getEFactory(eClass.getEPackage().getNsURI());
		final EObject obj = factory.create(eClass);
		if (isLazyLoading()) {
			if (eobFactory == null) {
				eobFactory = new LazyEObjectFactory(getResourceSet().getPackageRegistry(), new MethodInterceptor() {
					@Override
					public Object intercept(final Object o, final Method m, final Object[] args, final MethodProxy proxy) throws Throwable {
						final EObject eob = (EObject)o;
						switch (m.getName()) {
						case "eIsSet":
							final EStructuralFeature sfEIsSet = (EStructuralFeature)args[0];
							return (Boolean)proxy.invokeSuper(o, args) || getLazyResolver().isLazy(eob, sfEIsSet);
						case "eContainingFeature":
						case "eContainmentFeature":
							// Most implementations use eContainerFeatureID, but if they don't we fall back to the lazy resolver
							EReference sfContainingF = lazyResolver.getContainingFeature(eob);
							return sfContainingF != null ? sfContainingF : proxy.invokeSuper(o, args);
						case "eContainerFeatureID":
								EReference sfContaining = lazyResolver.getContainingFeature(eob);
								assert sfContaining.isContainment() : "containing feature should be containment";
								if (sfContaining != null) {
									if (sfContaining.getEOpposite() != null) {
										return sfContaining.getEOpposite().getFeatureID();
									} else {
										return InternalEObject.EOPPOSITE_FEATURE_BASE - sfContaining.getFeatureID();
									}
								} else {
									return proxy.invokeSuper(o, args);
								}
						case "eInternalContainer":
						case "eContainer":
							final EObject rawContainer = (EObject) proxy.invokeSuper(o, args);
							return rawContainer != null ? rawContainer : (lazyResolver != null ? lazyResolver.getContainer(eob) : null);
						case "eResource":
							final Object rawResource = proxy.invokeSuper(o, args);
							return rawResource != null ? rawResource : (lazyResolver != null ? lazyResolver.getContainer(eob) : null);
						case "eGet":
							final EStructuralFeature sfEGet = (EStructuralFeature)args[0];
							return interceptEGet(eob, args, proxy, sfEGet);
						case "eContents":
							// eContents requires resolving all containment references from the object
							final LoadingMode loadingMode = getDescriptor().getLoadingMode();
							synchronized(nodeIdToEObjectMap) {
								for (EReference ref : eob.eClass().getEAllReferences()) {
									if (ref.isContainment()) {
										getLazyResolver().resolve(eob, (EStructuralFeature)ref, loadingMode.isGreedyReferences(), loadingMode.isGreedyAttributes());
									}
								}
							}
							break;
						default:
							if (m.getName().startsWith("get")) {
								// Reuse the regular eGet
								EReference eRef = eobFactory.guessEReferenceFromGetter(eob.eClass(), m.getName());
								if (eRef != null) {
									return interceptEGet(eob, args, proxy, eRef);
								}
							}
							break;
						}
						return proxy.invokeSuper(o, args);
					}

					protected Object interceptEGet(final EObject eob, final Object[] args, final MethodProxy proxy, final EStructuralFeature sf) throws Throwable {
						// We need to serialize modifications from lazy loading + change notifications,
						// for consistency and for the ability to signal if an EMF notification comes
						// from lazy loading or not.
						synchronized(nodeIdToEObjectMap) {
							final LoadingMode loadingMode = getDescriptor().getLoadingMode();
							Object value = getLazyResolver().resolve(eob, sf, loadingMode.isGreedyReferences(), loadingMode.isGreedyAttributes());
							if (value != null) {
								return value;
							}
						}
						return proxy.invokeSuper(eob, args);
					}
				});
			}
			return eobFactory.createInstance(eClass);
		} else {
			return obj;
		}
	}

	public boolean isLazyLoading() {
		final LoadingMode mode = descriptor.getLoadingMode();
		return !mode.isGreedyAttributes() || !mode.isGreedyElements();
	}

	private List<EObject> createEObjectTree(final List<ModelElement> elems, final TreeLoadingState state) throws IOException {
		final List<EObject> eObjects = new ArrayList<>();
		for (final ModelElement me : elems) {
			if (me.isSetMetamodelUri()) {
				state.lastMetamodelURI = me.getMetamodelUri();
			} else {
				me.setMetamodelUri(state.lastMetamodelURI);
			}
			
			if (me.isSetTypeName()) {
				state.lastTypename = me.getTypeName();
			} else {
				me.setTypeName(state.lastTypename);
			}

			if (me.isSetRepositoryURL()) {
				state.lastRepository = me.getRepositoryURL();
			} else {
				me.setRepositoryURL(state.lastRepository);
			}

			if (me.isSetFile()) {
				state.lastFile = me.getFile();
			} else {
				me.setFile(state.lastFile);
			}

			final EObject obj = createEObject(me);
			state.allEObjects.add(obj);
			state.meToEObject.put(me, obj);
			eObjects.add(obj);

			if (me.isSetContainers()) {
				for (final ContainerSlot s : me.containers) {
					final EStructuralFeature sf = obj.eClass().getEStructuralFeature(s.name);
					final List<EObject> children = createEObjectTree(s.elements, state);
					if (sf.isMany()) {
						obj.eSet(sf, ECollections.toEList(children));
					} else if (!children.isEmpty()) {
						obj.eSet(sf, children.get(0));
					}
					for (final EObject child : children) {
						if (child.eResource() != null) {
							child.eResource().getContents().remove(child);
						}
					}
				}
			}
		}
		return eObjects;
	}

	private void fillInReferences(final List<ModelElement> elems, final TreeLoadingState state) throws IOException {
		final Registry packageRegistry = getResourceSet().getPackageRegistry();

		for (final ModelElement me : elems) {
			final EObject sourceObj = state.meToEObject.remove(me);
			fillInReferences(packageRegistry, me, sourceObj, state);
		}
	}

	private void fillInReferences(final Registry packageRegistry, final ModelElement me, final EObject sourceObj, final TreeLoadingState state) throws IOException {
		if (me.isSetReferences()) {
			for (final ReferenceSlot s : me.references) {
				final EClass eClass = getEClass(me.getMetamodelUri(), me.getTypeName(), packageRegistry);
				final EReference feature = (EReference) eClass.getEStructuralFeature(s.name);

				if (feature.isContainer() && sourceObj.eResource() != null) {
					sourceObj.eResource().getContents().remove(sourceObj);
				}
				fillInReference(sourceObj, s, feature, state);
			}
		}

		if (me.isSetContainers()) {
			for (final ContainerSlot s : me.getContainers()) {
				fillInReferences(s.elements, state);
			}
		}

		if (sourceObj.eContainer() == null) {
			// This is a root element for the moment: add it to the appropriate resource
			addToResource(me.getRepositoryURL(), me.getFile(), sourceObj);
		}
	}

	private void fillInReference(final EObject sourceObj, final ReferenceSlot s, final EReference feature, final TreeLoadingState state) {
		if (!feature.isChangeable() || feature.isDerived() && !(sourceObj instanceof DynamicEStoreEObjectImpl)) {
			// we don't set unchangeable features, and we don't derived references on real objects
			return;
		}

		// True if we can count on having all the relevant model elements now, false if we need to use lazy loading
		// or lazy resolving (for paged greedy loads).
		final boolean allAvailable = descriptor.getLoadingMode().isGreedyElements() && !descriptor.isPaged();

		// This variable will be set to a non-null value if we need to call eSet
		EList<Object> eSetValues = null;

		/*
		 * When using a query in combination with a lazy mode, the query might
		 * have retrieved the value we wanted straight away (e.g. LAZY_CHILDREN
		 * + "return Tree.all;"). Therefore, it is better to simply check if we
		 * already have the value and then add things to the resolver if we
		 * don't have it *and* we're on a lazy mode.
		 */

		if (s.isSetId()) {
			final EObject eObject = nodeIdToEObjectMap.get(s.id);
			if (eObject != null) {
				eSetValues = createEList(eObject);
			} else if (!allAvailable) {
				final EList<Object> value = new BasicEList<Object>();
				value.add(s.id);
				getLazyResolver().putLazyReference(sourceObj, feature, value);
			}
		}
		else if (s.isSetIds()) {
			eSetValues = createEList();
			for (final String targetId : s.ids) {
				final EObject eob = nodeIdToEObjectMap.get(targetId);
				if (eob != null) {
					eSetValues.add(eob);
					if (feature.isContainment() && eob.eResource() != null) {
						eob.eResource().getContents().remove(eob);
					}
				}
			}

			if (!allAvailable && eSetValues.size() != s.ids.size()) {
				eSetValues = null;
				final EList<Object> lazyIds = new BasicEList<>();
				lazyIds.addAll(s.ids);
				getLazyResolver().putLazyReference(sourceObj, feature, lazyIds);
			}
		}
		else if (s.isSetPosition()) {
			eSetValues = createEList(state.allEObjects.get(s.position));
		}
		else if (s.isSetPositions()) {
			eSetValues = createEList();
			for (final Integer position : s.positions) {
				eSetValues.add(state.allEObjects.get(position));
			}
		}
		else if (s.isSetMixed()) {
			final EList<Object> value = createEList();

			boolean allFetched = true;
			for (final MixedReference mixed : s.mixed) {
				if (mixed.isSetId()) {
					final EObject eob = nodeIdToEObjectMap.get(mixed.getId());
					if (eob != null) {
						value.add(eob);
						if (feature.isContainment() && eob.eResource() != null) {
							eob.eResource().getContents().remove(eob);
						}
					} else if (!allAvailable) {
						allFetched = false;
						value.add(mixed.getId());
					}
				} else if (mixed.isSetPosition()) {
					value.add(state.allEObjects.get(mixed.getPosition()));
				} else {
					LOGGER.warn("Unknown mixed reference in {}", mixed);
				}
			}
			if (allFetched) {
				eSetValues = value;
			} else {
				getLazyResolver().putLazyReference(sourceObj, feature, value);
			}
		}
		else {
			LOGGER.warn("No known reference field was set in {}", s);
		}

		if (eSetValues != null) {
			if (feature.isMany()) {
				sourceObj.eSet(feature, eSetValues);
			} else if (!eSetValues.isEmpty()) {
				sourceObj.eSet(feature, eSetValues.get(0));
			}
			if (feature.isContainment()) {
				for (final Object o : eSetValues) {
					final EObject contained = (EObject)o;
					if (contained.eResource() != null) {
						contained.eResource().getContents().remove(contained);
					}
				}
			}
		}
	}

	private EList<Object> createEList(final EObject... objects) {
		final EList<Object> values = new BasicEList<Object>();
		values.addAll(Arrays.asList(objects));
		return values;
	}

	private LazyResolver getLazyResolver() {
		if (lazyResolver == null) {
			lazyResolver = new LazyResolver(this);
		}
		return lazyResolver;
	}

	@Override
	protected void doLoad(final InputStream inputStream, final Map<?, ?> options) throws IOException {
		final HawkModelDescriptor descriptor = new HawkModelDescriptor();
		descriptor.load(inputStream);
		doLoad(descriptor, new NullProgressMonitor());
	}

	@Override
	protected void doUnload() {
		if (!getContents().isEmpty()) {
			getContents().clear();
		}
	    getErrors().clear();
	    getWarnings().clear();

		resources.clear();
		nodeIdToEObjectMap.clear();
		classToEObjectsMap.clear();

		if (client != null) {
			client.getInputProtocol().getTransport().close();
			client = null;
		}

		if (subscriber != null) {
			try {
				subscriber.closeSession();
			} catch (final ActiveMQException e) {
				LOGGER.error("Could not close the subscriber session", e);
			}
			subscriber = null;
		}

		lazyResolver = null;
	}

	@Override
	protected void doSave(final OutputStream outputStream, final Map<?, ?> options) throws IOException {
		LOGGER.warn("Hawk views are read-only: ignoring request to save");
	}

	private void notifyAttributeUnset(final EObject eob, final EStructuralFeature eAttr, final Object oldValue) {
		if (oldValue instanceof Iterable) {
			for (final Object o : (Iterable<?>)oldValue) {
				dataTypeDeleted(eAttr.getEType(), o);
				featureDeleted(eob, eAttr, o);
			}
		} else {
			dataTypeDeleted(eAttr.getEType(), oldValue);
			featureDeleted(eob, eAttr, oldValue);
		}
	}

	private void notifyAttributeSet(final EObject eob, final EStructuralFeature eAttr, final Object newValue) {
		if (newValue instanceof Iterable) {
			for (final Object o : (Iterable<?>)newValue) {
				dataTypeInserted(eAttr.getEType(), o);
				featureInserted(eob, eAttr, o);
			}
		} else {
			dataTypeInserted(eAttr.getEType(), newValue);
			featureInserted(eob, eAttr, newValue);
		}
	}

	private void featureInserted(final EObject source, final EStructuralFeature eAttr, final Object o) {
		for (final HawkResourceChangeListener l : changeListeners) {
			l.featureInserted(source, eAttr, o);
		}
	}

	private void featureDeleted(final EObject eob, final EStructuralFeature eAttr, final Object oldValue) {
		for (final HawkResourceChangeListener l : changeListeners) {
			l.featureDeleted(eob, eAttr, oldValue);
		}
	}

	private void instanceDeleted(final EObject eob, final EClass eClass) {
		for (final HawkResourceChangeListener l : changeListeners) {
			l.instanceDeleted(eClass, eob);
		}
	}

	private void instanceInserted(final EObject eob, final EClass eClass) {
		for (final HawkResourceChangeListener l : changeListeners) {
			l.instanceInserted(eClass, eob);
		}
	}

	private void dataTypeDeleted(final EClassifier eType, final Object oldValue) {
		for (final HawkResourceChangeListener l : changeListeners) {
			l.dataTypeDeleted(eType, oldValue);
		}
	}

	private void dataTypeInserted(final EClassifier eType, final Object newValue) {
		for (final HawkResourceChangeListener l : changeListeners) {
			l.dataTypeInserted(eType, newValue);
		}
	}

	@Override
	public EObject fetchNode(HawkResource containerResource, String uriFragment, boolean mustFetchAttributes) throws Exception {
		// TODO We need to extend the Thrift API to support this, and it'd be an inefficient operation right now.
		throw new UnsupportedOperationException();
	}

	@Override
	public EObject fetchNode(String id, boolean mustFetchAttributes) throws Exception {
		if (!isIncludeNodeIDs(getDescriptor())) {
			throw new IllegalArgumentException("Cannot fetch by ID: loading mode is " + descriptor.getLoadingMode());
		}

		final EObject eob = nodeIdToEObjectMap.get(id);
		if (eob != null) {
			return eob;
		}

		final EList<EObject> resolved = fetchNodes(Arrays.asList(id), mustFetchAttributes);
		if (resolved.isEmpty()) {
			return null;
		} else {
			return resolved.get(0);
		}
	}

	@Override
	public String getEObjectNodeID(EObject obj) {
		return nodeIdToEObjectMap.inverse().get(obj);
	}

	@Override
	public List<String> getRegisteredMetamodels() throws Exception {
		return client.listMetamodels(descriptor.getHawkInstance());
	}

	@Override
	public List<String> getRegisteredTypes(String metamodelURI) throws Exception {
		HawkQueryOptions opts = new HawkQueryOptions();
		opts.setRepositoryPattern(descriptor.getHawkRepository());
		opts.setFilePatterns(Arrays.asList(descriptor.getHawkFilePatterns()));
		QueryResult queryResults = client.query(descriptor.getHawkInstance(),
				"return Model.metamodels.selectOne(p|p.uri='%s').types.name;", EOL_QUERY_LANG, opts);

		final List<String> types = new ArrayList<>();
		for (QueryResult qr : queryResults.getVList()) {
			if (qr.isSetVString()) {
				types.add(qr.getVString());
			}
		}
		return types;
	}

	@Override
	public void markChanged(EObject eob) {
		// TODO Auto-generated method stub
		
	}

}
