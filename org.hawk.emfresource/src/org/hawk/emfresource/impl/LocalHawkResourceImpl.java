/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.emfresource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
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
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.emfresource.HawkResource;
import org.hawk.emfresource.HawkResourceChangeListener;
import org.hawk.emfresource.util.AttributeUtils;
import org.hawk.emfresource.util.LazyEObjectFactory;
import org.hawk.emfresource.util.LazyResolver;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * EMF driver that reads a local model from a Hawk index. This resource will
 * always be empty: all fetched EObjects are placed on surrogate
 * {@link HawkFileResourceImpl}, which have the same URI as the originally
 * indexed models.
 */
public class LocalHawkResourceImpl extends ResourceImpl implements HawkResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalHawkResourceImpl.class);

	private final class LazyReferenceResolver implements MethodInterceptor {
		@SuppressWarnings("unchecked")
		@Override
		public Object intercept(final Object o, final Method m, final Object[] args, final MethodProxy proxy) throws Throwable {
			/*
			 * We need to serialize modifications from lazy loading + change
			 * notifications, for consistency and for the ability to signal if
			 * an EMF notification comes from lazy loading or not.
			 */
			final EObject eob = (EObject) o;
			final EStructuralFeature sf = (EStructuralFeature)args[0];
			if (lazyResolver != null && sf instanceof EReference && lazyResolver.isPending(eob, sf)) {
				final EReference ref = (EReference) sf;
				synchronized(nodeIdToEObjectMap) {
					lazyResolver.resolve(eob, sf);
				}

				/*
				 * When we resolve a reference, it may be a containment or
				 * container reference: need to adjust the list of root elements
				 * then.
				 */
				Object superValue = proxy.invokeSuper(o, args);
				if (superValue != null) {
					if (ref.isContainer()) {
						removeRedundantRoot(eob);
					} else if (ref.isContainment()) {
						if (ref.isMany()) {
							for (EObject child : (Iterable<EObject>) superValue) {
								removeRedundantRoot(child);
							}
						} else {
							removeRedundantRoot((EObject) superValue);
						}
					}
				}
				return superValue;
			}
			return proxy.invokeSuper(o, args);
		}
	}

	private static EClass getEClass(final String metamodelUri, final String typeName,
			final Registry packageRegistry) {
		final EPackage pkg = packageRegistry.getEPackage(metamodelUri);
		if (pkg == null) {
			throw new NoSuchElementException(String.format(
					"Could not find EPackage with URI '%s' in the registry %s",
					metamodelUri, packageRegistry));
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
	private final Map<String, HawkFileResourceImpl> uriToResource = new HashMap<>();
	private final Map<String, FileNode> uriToFileNode = new HashMap<>();

	private LazyResolver lazyResolver;
	private IGraphChangeListener changeListener;
	private LazyEObjectFactory eobFactory;

	private IModelIndexer indexer;
	private Set<Runnable> syncEndListeners = new HashSet<>();

	public LocalHawkResourceImpl() {
		// for Exeed
	}

	public LocalHawkResourceImpl(final URI uri, final IModelIndexer indexer) {
		super(uri);
		this.indexer = indexer;
	}

	@Override
	public void load(final Map<?, ?> options) throws IOException {
		doLoad();
	}

	@Override
	public void save(Map<?, ?> options) throws IOException {
		doSave(null, null);
	}

	@Override
	public TreeIterator<EObject> getAllContents() {
		LOGGER.warn("getAllContents() being called on a Hawk resource: inefficient!");
		return super.getAllContents();
	}

	@Override
	public String getEObjectNodeID(EObject obj) {
		return nodeIdToEObjectMap.inverse().get(obj);
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

	@Override
	public void fetchAttributes(Map<String, EObject> idToEObject) {
		// do nothing - we fetch attributes by default already
	}

	@Override
	public EList<EObject> fetchNodes(final List<String> ids, boolean fetchAttributes) throws Exception {
		// Filter the objects that need to be retrieved
		final List<String> toBeFetched = new ArrayList<>();
		for (final String id : ids) {
			if (!nodeIdToEObjectMap.containsKey(id)) {
				toBeFetched.add(id);
			}
		}

		// Fetch the eObjects, decode them and resolve references
		if (!toBeFetched.isEmpty()) {
			try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
				final GraphWrapper gw = new GraphWrapper(indexer.getGraph());

				final List<ModelElementNode> elems = new ArrayList<>();
				for (String id : toBeFetched) {
					elems.add(gw.getModelElementNodeById(id));
				}
				createOrUpdateEObjects(elems);

				tx.success();
			}
		}

		// Rebuild the real EList now
		final EList<EObject> finalList = new BasicEList<EObject>(ids.size());
		for (final String id : ids) {
			final EObject eObject = nodeIdToEObjectMap.get(id);
			finalList.add(eObject);
		}
		return finalList;
	}

	@Override
	public EList<EObject> fetchNodes(final EClass eClass, boolean fetchAttributes) throws Exception {
		try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(indexer.getGraph());
			final MetamodelNode mn = gw.getMetamodelNodeByNsURI(eClass.getEPackage().getNsURI());
			for (TypeNode tn : mn.getTypes()) {
				if (eClass.getName().equals(tn.getTypeName())) {
					Iterable<ModelElementNode> instances = tn.getAll();
					createOrUpdateEObjects(instances);

					final EList<EObject> l = new BasicEList<EObject>();
					for (ModelElementNode en : instances) {
						l.add(nodeIdToEObjectMap.get(en.getNodeId()));
					}
					return l;
				}
			}
			tx.success();
		}

		LOGGER.warn("Could not find a type node for EClass {}:{}", eClass.getEPackage().getNsURI(), eClass.getName());
		return new BasicEList<EObject>();
	}

	@Override
	public EObject fetchNode(String id, boolean fetchAttributes) throws Exception {
		EList<EObject> fetched = fetchNodes(Arrays.asList(id), fetchAttributes);
		if (!fetched.isEmpty()) {
			return fetched.get(0);
		} else {
			return null;
		}
	}

	@Override
	public EObject fetchNode(HawkResource containerResource, String uriFragment, boolean fetchAttributes) throws Exception {
		if (!(containerResource instanceof HawkFileResourceImpl)) {
			return null;
		}

		final HawkFileResourceImpl r = (HawkFileResourceImpl)containerResource;
		final FileNode fileNode = uriToFileNode.get(r.getURI().toString());
		LOGGER.warn("Iterating over the contents of {}{} to find fragment {}: inefficient!",
				fileNode.getRepositoryURL(), fileNode.getFilePath(), uriFragment);

		String nodeId = null;
		try (IGraphTransaction tx = fileNode.getNode().getGraph().beginTransaction()) {
			for (ModelElementNode me : fileNode.getModelElements()) {
				final String fragment = me.getElementId();
				final String currentNodeId = me.getNodeId();
				if (uriFragment.equals(fragment)) {
					nodeId = currentNodeId;
				}
			}
		}

		if (nodeId != null) {
			return fetchNode(nodeId, fetchAttributes);
		} else {
			return null;
		}
	}

	protected EList<EObject> fetchNodesByQueryResults(final List<Object> queryResults) throws Exception {
		final List<String> ids = new ArrayList<>();
		for (Object r : queryResults) {
			if (r instanceof IGraphNodeReference) {
				final IGraphNodeReference men = (IGraphNodeReference)r;
				ids.add(men.getId());
			}
		}
		return fetchNodes(ids, false);
	}

	@Override
	public List<Object> fetchValuesByEClassifier(final EClassifier dataType) throws Exception {
		final Map<EClass, List<EStructuralFeature>> candidateTypes = fetchTypesWithEClassifier(dataType);

		final List<Object> values = new ArrayList<>();
		for (final Entry<EClass, List<EStructuralFeature>> entry : candidateTypes.entrySet()) {
			final EClass eClass = entry.getKey();
			final List<EStructuralFeature> attrsWithType = entry.getValue();
			for (final EObject eob : fetchNodes(eClass, true)) {
				for (final EStructuralFeature attr : attrsWithType) {
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
	public Map<EClass, List<EStructuralFeature>> fetchTypesWithEClassifier(final EClassifier dataType) throws Exception {
		try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(indexer.getGraph());

			final Map<EClass, List<EStructuralFeature>> candidateTypes = new IdentityHashMap<>();
			for (MetamodelNode mn : gw.getMetamodelNodes()) {
				final String nsURI = mn.getUri();
				EPackage pkg = getResourceSet().getPackageRegistry().getEPackage(nsURI);
				if (pkg != null) {

				for (TypeNode tn : mn.getTypes()) {
					// type has instances?
					if (tn.getAll().iterator().hasNext()) {
						final String name = tn.getTypeName();

						// type has desired dataType somewhere?
						if (pkg != null) {
							EClassifier classifier = pkg.getEClassifier(name);
							if (classifier instanceof EClass) {
								final EClass eClass = (EClass)classifier;
								for (EStructuralFeature sf : eClass.getEAllStructuralFeatures()) {
									if (sf.getEType() == dataType) {
										List<EStructuralFeature> features = candidateTypes.get(eClass);
										if (features == null) {
											features = new ArrayList<>();
											candidateTypes.put(eClass, features);
										}
										features.add(sf);
									}
								}
							}
						}
					}
				}

				} else {
					LOGGER.warn("We do not have the '{}' EPackage in the registry, skipping", nsURI);
				}
			}

			tx.success();
			return candidateTypes;
		}
	}

	@Override
	public Map<EObject, Object> fetchValuesByEStructuralFeature(final EStructuralFeature feature) throws Exception {
		final EClass featureEClass = feature.getEContainingClass();
		final EList<EObject> eobs = fetchNodes(featureEClass, true);

		if (feature instanceof EReference) {
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
			}
		}
		return values;
	}

	@Override
	protected void doLoad(final InputStream inputStream, final Map<?, ?> options) throws IOException {
		doLoad();
	}

	protected void doLoad() throws IOException {
		try {
			if (indexer == null) {
				return;
			}
			else if (!indexer.isRunning()) {
				// We need an IOException so EMF will display it properly
				throw new IOException(String.format("The Hawk instance with name '%s' is not running: please start it first", indexer.getName()));
			}
	
			lazyResolver = new LazyResolver(this);
			eobFactory = new LazyEObjectFactory(getResourceSet().getPackageRegistry(), new LazyReferenceResolver());
	
			final GraphWrapper gw = new GraphWrapper(indexer.getGraph());
			try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
				// TODO add back ability for filtering repos/files?
				final List<String> all = Arrays.asList("*");
				for (FileNode fileNode : gw.getFileNodes(all, all)) {
					for (ModelElementNode elem : fileNode.getRootModelElements()) {
						if (!elem.isContained()) {
							createOrUpdateEObject(elem);
						}
					}
				}
				tx.success();
			}
	
			changeListener = new LocalHawkResourceUpdater(this);
			indexer.addGraphChangeListener(changeListener);
			setLoaded(true);
		} catch (final IOException e) {
			LOGGER.error("I/O exception while opening model", e);
			throw e;
		} catch (final Exception e) {
			LOGGER.error("Exception while loading model", e);
		}
	}

	@Override
	protected void doUnload() {
		super.doUnload();

		for (Resource r : uriToResource.values()) {
			r.unload();
		}
		uriToResource.clear();
		uriToFileNode.clear();

		if (indexer != null) {
			indexer.removeGraphChangeListener(changeListener);
		}

		nodeIdToEObjectMap.clear();
		lazyResolver = null;
		changeListener = null;
		eobFactory = null;
	}

	@Override
	protected void doSave(final OutputStream outputStream, final Map<?, ?> options) throws IOException {
		LOGGER.warn("Hawk views are read-only: ignoring request to save");
	}

	protected List<EObject> createOrUpdateEObjects(final Iterable<ModelElementNode> elems) throws Exception {
		synchronized (nodeIdToEObjectMap) {
			final List<EObject> eObjects = new ArrayList<>();
			for (final ModelElementNode me : elems) {
				EObject eob = createOrUpdateEObject(me);
				eObjects.add(eob);
			}
			return eObjects;
		}
	}

	/**
	 * If we already have it loaded, returns the EObject created from the graph
	 * node with the provided <code>id</code>. Otherwise, returns
	 * <code>null</code>.
	 */
	protected EObject getNodeEObject(String id) {
		return nodeIdToEObjectMap.get(id);
	}

	@SuppressWarnings("unchecked")
	protected void removeNode(String id) {
		synchronized (nodeIdToEObjectMap) {
			final EObject eob = nodeIdToEObjectMap.remove(id);
			if (eob == null) return;
	
			final EObject container = eob.eContainer();
			final HawkFileResourceImpl r = (HawkFileResourceImpl)eob.eResource();
			r.removeFragment(id);

			if (container == null) {
				if (r != null) {
					r.getContents().remove(eob);
				}
			} else {
				final EStructuralFeature containingFeature = eob.eContainingFeature();
				if (containingFeature.isMany()) {
					((Collection<EObject>) container.eGet(containingFeature)).remove(eob);
				} else {
					container.eUnset(containingFeature);
				}
			}
		}
	}

	protected IGraphTransaction beginGraphTransaction() throws Exception {
		return indexer.getGraph().beginTransaction();
	}

	private EObject createOrUpdateEObject(final ModelElementNode me) throws Exception {
		final Registry registry = getResourceSet().getPackageRegistry();
		final TypeNode typeNode = me.getTypeNode();
		final String nsURI = typeNode.getMetamodelURI();
		final EClass eClass = getEClass(nsURI, typeNode.getTypeName(), registry);

		final EObject existing = nodeIdToEObjectMap.get(me.getNodeId());
		final EObject eob = existing != null ? existing : eobFactory.createInstance(eClass);
		if (existing == null) {
			nodeIdToEObjectMap.put(me.getNodeId(), eob);
		}

		final Map<String, Object> attributeValues = new HashMap<>();
		final Map<String, Object> referenceValues = new HashMap<>();
		me.getSlotValues(attributeValues, referenceValues);

		// Set or update attributes
		final EFactory factory = registry.getEFactory(nsURI);
		if (existing != null) {
			// Unset attributes that do not have a value anymore
			for (EAttribute attr : eClass.getEAllAttributes()) {
				if (attr.isDerived() || !attr.isChangeable()) continue;

				if (!attributeValues.containsKey(attr.getName())) {
					if (existing.eIsSet(attr)) {
						existing.eUnset(attr);
					}
				}
			}
		}
		for (Map.Entry<String, Object> entry : attributeValues.entrySet()) {
			final String attrName = entry.getKey();
			final Object attrValue = entry.getValue();
			AttributeUtils.setAttribute(factory, eClass, eob, attrName, attrValue);
		}

		// Set or update references
		if (existing != null) {
			// Unset references that do not have a value anymore
			for (EReference ref : eClass.getEAllReferences()) {
				if (ref.isDerived() || !ref.isChangeable()) continue;

				if (!referenceValues.containsKey(ref.getName())) {
					if (lazyResolver.isPending(existing, ref)) {
						lazyResolver.removeLazyReference(existing, ref);
					} else if (existing.eIsSet(ref)) {
						existing.eUnset(ref);
					}
				}
			}
		}
		for (Map.Entry<String, Object> entry : referenceValues.entrySet()) {
			final EReference ref = (EReference) eClass.getEStructuralFeature(entry.getKey());

			// If this reference was resolved before, it needs to stay resolved.
			final boolean existingNonLazy = existing != null
					&& !lazyResolver.isPending(existing, ref)
					&& existing.eIsSet(ref);

			final EList<Object> referenced = new BasicEList<>();
			boolean hasLazy = false;
			if (entry.getValue() instanceof Collection) {
				for (Object o : (Collection<?>)entry.getValue()) {
					final String id = o.toString();
					hasLazy = addToReferenced(id, referenced, existingNonLazy) || hasLazy;
				}
			} else {
				final String id = entry.getValue().toString();
				hasLazy = addToReferenced(id, referenced, existingNonLazy) || hasLazy;
			}

			if (hasLazy) {
				lazyResolver.putLazyReference(eob, ref, referenced);
			} else if (ref.isMany()) {
				eob.eSet(ref, referenced);
			} else if (!referenced.isEmpty()) {
				eob.eSet(ref, referenced.get(0));
			}
		}

		if (existing == null) {
			addToResource(me, eob);
		}
		return eob;
	}

	private boolean addToReferenced(final String id, final EList<Object> referenced, final boolean resolveMissing) throws Exception {
		EObject refExisting = nodeIdToEObjectMap.get(id);
		if (refExisting == null && resolveMissing) {
			EList<EObject> refExistingResolved = fetchNodes(Arrays.asList(id), false);
			if (!refExistingResolved.isEmpty()) {
				refExisting = refExistingResolved.get(0);
			}
		}

		if (refExisting != null) {
			referenced.add(refExisting);
			return false;
		} else {
			referenced.add(id);
			return true;
		}
	}

	private void addToResource(final ModelElementNode modelElementNode, final EObject eob) {
		final FileNode fileNode = modelElementNode.getFileNode();
		final String repoURL = fileNode.getRepositoryURL();
		final String path = fileNode.getFilePath();
		final String fullURL = repoURL + path;
		synchronized(uriToResource) {
			HawkFileResourceImpl resource = uriToResource.get(fullURL);
			if (resource == null) {
				/*
				 * We can't use the createResource method in the resource set,
				 * as that might invoke another factory, and we need the new
				 * resource to be a Hawk file resource.
				 */
				resource = new HawkFileResourceImpl(URI.createURI(fullURL), this);
				getResourceSet().getResources().add(resource);
				uriToResource.put(fullURL, resource);
				uriToFileNode.put(fullURL, fileNode);
			}
			if (eob.eContainer() == null) {
				resource.getContents().add(eob);
			}
			resource.addFragment(modelElementNode.getNodeId(), modelElementNode.getElementId());
		}
	}

	private void removeRedundantRoot(EObject child) {
		final Resource r = child.eResource();
		if (r != null && child.eContainer() != null && child.eResource() == child.eContainer().eResource()) {
			// We only remove when it won't affect the results of the child.eResource() call
			// (it's contained within something that is in the same resource).
			r.getContents().remove(child);
		}
	}

	@Override
	public boolean addSyncEndListener(Runnable r) {
		return syncEndListeners.add(r);
	}

	@Override
	public boolean removeSyncEndListener(Runnable r) {
		return syncEndListeners.remove(r);
	}

	public Set<Runnable> getSyncEndListeners() {
		return Collections.unmodifiableSet(syncEndListeners);
	}

	@Override
	public boolean addChangeListener(HawkResourceChangeListener l) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeChangeListener(HawkResourceChangeListener l) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getRegisteredMetamodels() throws Exception {
		try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
			IGraphNodeIndex idx = indexer.getGraph().getMetamodelIndex();
			final List<String> metamodels = new ArrayList<>();
			for (IGraphNode mmNode : idx.query("*", "*")) {
				metamodels.add(new MetamodelNode(mmNode).getUri());
			}
			return metamodels;
		}
	}

	@Override
	public List<String> getRegisteredTypes(String metamodelURI) throws Exception {
		try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
			IGraphNodeIndex idx = indexer.getGraph().getMetamodelIndex();
			final List<String> types = new ArrayList<>();
			for (IGraphNode mmNode : idx.get("id", metamodelURI)) {
				final MetamodelNode metamodel = new MetamodelNode(mmNode);
				for (TypeNode type : metamodel.getTypes()) {
					types.add(type.getTypeName());
				}
			}
			return types;
		}
	}

}
