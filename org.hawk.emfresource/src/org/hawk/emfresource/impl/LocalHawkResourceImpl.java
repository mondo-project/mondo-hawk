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

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.BasicEList;
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
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.IQueryEngine;
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * <p>EMF driver that reads a local model from a Hawk index. Available options:</p>
 * <ul>
 * <li>{@link #OPTION_SPLIT}</li>
 * </ul>
 */
public class LocalHawkResourceImpl extends ResourceImpl implements HawkResource {

	/**
	 * Option key for {@link #load(Map)}: the value should be a <code>Boolean</code>.
	 * If the value is unset or not <code>false</code>, the contents of the resource
	 * will be split across several surrogate {@link HawkFileResourceImpl} instances
	 * that have the same URI as the originally indexed models. If the value is set
	 * to <code>true</code>, this behavior will be disabled.
	 */
	public static final String OPTION_SPLIT = "split";

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

			switch (m.getName()) {
			case "eIsSet":
				return (Boolean)proxy.invokeSuper(o, args) || lazyResolver.isLazy(eob, (EStructuralFeature) args[0]);
			case "eContainmentFeature":
				final Object rawCF = proxy.invokeSuper(o, args);
				return rawCF != null ? rawCF : lazyResolver.getContainingFeature(eob);
			case "eContainer":
				final EObject rawContainer = (EObject) proxy.invokeSuper(o, args);
				return rawContainer != null ? rawContainer : lazyResolver.getContainer(eob);
			case "eResource":
				final Object rawResource = proxy.invokeSuper(o, args);
				return rawResource != null ? rawResource : lazyResolver.getResource(eob);
			case "eGet":
				final EStructuralFeature sf = (EStructuralFeature)args[0];
				if (sf instanceof EReference && lazyResolver.isLazy(eob, sf)) {
					final EReference ref = (EReference) sf;
					Object value;
					synchronized(nodeIdToEObjectCache) {
						value = lazyResolver.resolve(eob, sf, false, true);
					}

					/*
					 * When we resolve a reference, it may be a containment or
					 * container reference: need to adjust the list of root elements
					 * then.
					 */
					if (value != null) {
						if (ref.isContainer()) {
							removeRedundantRoot(eob);
						} else if (ref.isContainment()) {
							if (ref.isMany()) {
								for (EObject child : (Iterable<EObject>) value) {
									removeRedundantRoot(child);
								}
							} else {
								removeRedundantRoot((EObject) value);
							}
						}
						return value;
					} else {
						return proxy.invokeSuper(o, args);
					}
				}
				break;
			case "eContents":
			default:
				// Resolve all containment references for an eContents call
				synchronized(nodeIdToEObjectCache) {
					for (EReference ref : eob.eClass().getEAllReferences()) {
						if (ref.isContainment()) {
							lazyResolver.resolve(eob, (EStructuralFeature)ref, false, true);
						}
					}
				}
				break;
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

	private final Cache<String, EObject> nodeIdToEObjectCache = CacheBuilder.newBuilder().softValues().build();
	private final Cache<EObject, String> eObjectToNodeIdCache = CacheBuilder.newBuilder().weakKeys().build();

	/**
	 * Contains strong references to all the objects that have been changed, to
	 * prevent these changes from being lost during GC.
	 */
	private final Map<EObject, Object> dirtyObjects = new IdentityHashMap<>();

	private final Map<String, HawkFileResourceImpl> uriToResource = new HashMap<>();
	private final Map<String, FileNode> uriToFileNode = new HashMap<>();

	private LazyResolver lazyResolver;
	private IGraphChangeListener changeListener;
	private LazyEObjectFactory eobFactory;

	private IModelIndexer indexer;
	private Set<Runnable> syncEndListeners = new HashSet<>();

	private final boolean isSplit;

	private List<String> repositoryPatterns;
	private List<String> filePatterns;

	public LocalHawkResourceImpl() {
		// for Exeed
		this.isSplit = true;
	}

	public LocalHawkResourceImpl(final URI uri, final IModelIndexer indexer, boolean isSplit, final List<String> repoPatterns, final List<String> filePatterns) {
		super(uri);

		if (indexer == null) {
			throw new NullPointerException("indexer cannot be null");
		}

		this.indexer = indexer;
		this.isSplit = isSplit;
		this.repositoryPatterns = repoPatterns;
		this.filePatterns = filePatterns;
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
	public String getEObjectNodeID(EObject obj) {
		return eObjectToNodeIdCache.getIfPresent(obj);
	}

	@Override
	public boolean hasChildren(final EObject o) {
		final String nodeId = eObjectToNodeIdCache.getIfPresent(o);

		final GraphWrapper gW = new GraphWrapper(this.indexer.getGraph());
		try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
			ModelElementNode node = gW.getModelElementNodeById(nodeId);
			final boolean ret = node.hasChildren();
			tx.success();
			return ret;
		} catch (Throwable t) {
			LOGGER.error(t.getMessage(), t);
			return false;
		}
	}

	@Override
	public void fetchAttributes(Map<String, EObject> idToEObject) {
		// do nothing - we fetch attributes by default already
	}

	public EList<EObject> fetchByQuery(final String language, final String query, final Map<String, String> context) throws Exception {
		final Map<String, IQueryEngine> knownQL = indexer.getKnownQueryLanguages();
		final IQueryEngine queryEngine = knownQL.get(language);
		if (queryEngine == null) {
			throw new IllegalArgumentException(String.format("Unknown query langue %s: known query languages are %s", language, knownQL.keySet()));
		}

		final List<String> ids = new ArrayList<>();
		Object rawResult = queryEngine.query(indexer, query, context);
		addAllResults(rawResult, ids);

		return fetchNodes(ids, true);
	}

	private List<String> addAllResults(Object rawResult, List<String> ids) {
		if (rawResult instanceof Iterable) {
			for (Object rawElem : (Iterable<?>)rawResult) {
				addAllResults(rawElem, ids);
			}
		} else if (rawResult instanceof IGraphNodeReference) {
			IGraphNodeReference ref = (IGraphNodeReference)rawResult;
			ids.add(ref.getId());
		}
		return ids;
	}

	@Override
	public EList<EObject> fetchNodes(final List<String> ids, boolean fetchAttributes) throws Exception {
		// Split IDs into cached (keeping a strong ref so we don't lose them) and to be fetched
		final List<EObject> allCached = new ArrayList<>();
		final List<String> toBeFetched = new ArrayList<>();
		for (final String id : ids) {
			EObject cached = nodeIdToEObjectCache.getIfPresent(id);
			if (cached != null) {
				allCached.add(cached);
			} else {
				toBeFetched.add(id);
			}
		}

		// Fetch the pending eObjects, decode them and resolve references. Keep strong refs to them as well.
		@SuppressWarnings("unused") EList<EObject> fetched = null;
		if (!toBeFetched.isEmpty()) {
			try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
				final GraphWrapper gw = new GraphWrapper(indexer.getGraph());

				final List<ModelElementNode> elems = new ArrayList<>();
				for (String id : toBeFetched) {
					elems.add(gw.getModelElementNodeById(id));
				}
				fetched = createOrUpdateEObjects(elems);

				tx.success();
			}
		}

		// Rebuild the real EList now
		final EList<EObject> finalList = new BasicEList<EObject>(ids.size());
		for (final String id : ids) {
			final EObject eObject = nodeIdToEObjectCache.getIfPresent(id);
			finalList.add(eObject);
		}
		return finalList;
	}

	@Override
	public EList<EObject> fetchNodes(final EClass eClass, boolean fetchAttributes) throws Exception {
		return fetchNodesByContainerFragment(eClass, null, null);
	}

	/**
	 * Fetches all the instances of a certain {@link EClass} that are contained within the specified file.
	 */
	public EList<EObject> fetchNodesByContainerFragment(EClass eClass, String location, String path) throws Exception {
		try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(indexer.getGraph());
			final MetamodelNode mn = gw.getMetamodelNodeByNsURI(eClass.getEPackage().getNsURI());
			for (TypeNode tn : mn.getTypes()) {
				if (eClass.getName().equals(tn.getTypeName())) {
					Iterable<ModelElementNode> instances = tn.getAll();

					if (location != null && path != null) {
						Set<FileNode> fileNodes = gw.getFileNodes(Arrays.asList(location), Arrays.asList(path));

						final List<ModelElementNode> filtered = new ArrayList<>();
						for (ModelElementNode men : instances) {
							/*
							 * In order for men to be added, either itself or
							 * one of its containers must be contained within
							 * the specified file.
							 */
							ModelElementNode contained = men;
							while (contained != null && !fileNodes.contains(contained.getFileNode())) {
								contained = contained.getContainer();
							}
							if (contained != null) {
								filtered.add(men);
							}
						}

						instances = filtered;
					}

					return createOrUpdateEObjects(instances);
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
		final List<ModelElementNode> nodes = new ArrayList<>();
		for (Object r : queryResults) {
			if (r instanceof IGraphNodeReference) {
				final IGraphNodeReference ref = (IGraphNodeReference)r;
				final IGraphNode node = ref.getNode();
				final ModelElementNode men = new ModelElementNode(node);
				nodes.add(men);
			}
		}
		return createOrUpdateEObjects(nodes);
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
			if (!indexer.isRunning()) {
				// We need an IOException so EMF will display it properly
				throw new IOException(String.format("The Hawk instance with name '%s' is not running: please start it first", indexer.getName()));
			}

			lazyResolver = new LazyResolver(this);
			eobFactory = new LazyEObjectFactory(getResourceSet().getPackageRegistry(), new LazyReferenceResolver());

			final GraphWrapper gw = new GraphWrapper(indexer.getGraph());
			try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
				for (FileNode fileNode : gw.getFileNodes(repositoryPatterns, filePatterns)) {
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
			throw e;
		} catch (final Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	protected void doUnload() {
	    // This guard is needed to ensure that clear doesn't make the resource become loaded.
	    if (!getContents().isEmpty())
	    {
	      getContents().clear();
	    }
	    getErrors().clear();
	    getWarnings().clear();

		for (Resource r : uriToResource.values()) {
			r.unload();
		}
		uriToResource.clear();
		uriToFileNode.clear();

		if (indexer != null) {
			indexer.removeGraphChangeListener(changeListener);
		}

		nodeIdToEObjectCache.invalidateAll();
		eObjectToNodeIdCache.invalidateAll();
		lazyResolver = null;
		changeListener = null;
	}

	@Override
	protected void doSave(final OutputStream outputStream, final Map<?, ?> options) throws IOException {
		LOGGER.warn("Hawk views are read-only: ignoring request to save");
	}

	protected EList<EObject> createOrUpdateEObjects(final Iterable<ModelElementNode> elems) throws Exception {
		synchronized (nodeIdToEObjectCache) {
			final EList<EObject> eObjects = new BasicEList<>();
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
		return nodeIdToEObjectCache.getIfPresent(id);
	}

	@SuppressWarnings("unchecked")
	protected void removeNode(String id) {
		synchronized (nodeIdToEObjectCache) {
			final EObject eob = nodeIdToEObjectCache.getIfPresent(id);
			if (eob == null) {
				return;
			} else {
				nodeIdToEObjectCache.invalidate(id);
				eObjectToNodeIdCache.invalidate(eob);
			}
	
			final EObject container = eob.eContainer();
			final Resource r = eob.eResource();
			if (r instanceof HawkFileResourceImpl) {
				((HawkFileResourceImpl)r).removeFragment(id);
			}

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

		final EObject existing = nodeIdToEObjectCache.getIfPresent(me.getNodeId());
		final EObject eob = existing != null ? existing : eobFactory.createInstance(eClass);
		if (existing == null) {
			nodeIdToEObjectCache.put(me.getNodeId(), eob);
			eObjectToNodeIdCache.put(eob, me.getNodeId());
			eob.eAdapters().add(new AdapterImpl(){
				@Override
				public void notifyChanged(Notification msg) {
					markChanged(eob);
				}
			});
		}

		final Map<String, Object> attributeValues = new HashMap<>();
		final Map<String, Object> referenceValues = new HashMap<>();
		final Map<String, Object> mixedValues = new HashMap<>();
		me.getSlotValues(attributeValues, referenceValues, mixedValues);

		// Set or update attributes
		final EFactory factory = registry.getEFactory(nsURI);
		if (existing != null) {
			// Unset attributes that do not have a value anymore
			for (EAttribute attr : eClass.getEAllAttributes()) {
				if (attr.isDerived() || !attr.isChangeable()) continue;

				if (!attributeValues.containsKey(attr.getName()) && !mixedValues.containsKey(attr.getName())) {
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
		for (Map.Entry<String, Object> entry : mixedValues.entrySet()) {
			final String attrName = entry.getKey();
			final Object attrValue = entry.getValue();

			final EAttribute eAttr = (EAttribute)eClass.getEStructuralFeature(attrName);
			final boolean isMixed = ExtendedMetaData.INSTANCE.getMixedFeature(eClass) == eAttr;
			final FeatureMap fmap = (FeatureMap)eob.eGet(eAttr);

			// For now, we assume that feature maps have disjoint subsets of elements
			// and that we can use the other references to know where each of their
			// elements come from. We're only interested in their references right now,
			// so we can reconstruct the structure of a BPMN model from the graph.
			for (Object o : (Collection<?>)attrValue) {
				// If it's a node, try to fetch it
				EObject node = fetchNode(o.toString(), true);
				if (node == null) continue;

				// Find out where it belongs to
				for (Entry<String, Object> refEntry : referenceValues.entrySet()) {
					final EReference ref = (EReference)eClass.getEStructuralFeature(refEntry.getKey());
					if (!isMixed && ExtendedMetaData.INSTANCE.getGroup(ref) != eAttr) continue;
 
					if (o.equals(refEntry.getValue())) {
						fmap.add(ref, node); 	
					} else if (refEntry.getValue() instanceof Collection && ((Collection<?>)refEntry.getValue()).contains(o)) {
						fmap.add(ref, node);
					}
				}
			}
		}

		// Set or update references
		if (existing != null) {
			// Unset references that do not have a value anymore
			for (EReference ref : eClass.getEAllReferences()) {
				if (ref.isDerived() || !ref.isChangeable()) continue;

				if (!referenceValues.containsKey(ref.getName())) {
					lazyResolver.removeLazyReference(existing, ref);
				}
			}
		}
		for (Map.Entry<String, Object> entry : referenceValues.entrySet()) {
			final EReference ref = (EReference) eClass.getEStructuralFeature(entry.getKey());
			if (ref.isDerived() || !ref.isChangeable()) continue;

			final EList<Object> ids = new BasicEList<>();
			if (entry.getValue() instanceof Collection) {
				for (Object o : (Collection<?>)entry.getValue()) {
					final String id = o.toString();
					ids.add(id);
				}
			} else {
				final String id = entry.getValue().toString();
				ids.add(id);
			}

			lazyResolver.putLazyReference(eob, ref, ids);
		}

		if (existing == null) {
			addToResource(me, eob);
		}
		return eob;
	}

	private void addToResource(final ModelElementNode modelElementNode, final EObject eob) {
		final FileNode fileNode = modelElementNode.getFileNode();
		final String repoURL = fileNode.getRepositoryURL();
		final String path = fileNode.getFilePath();
		final String fullURL = repoURL + (repoURL.endsWith("/") || path.startsWith("/") ? "" : "/") + path;

		if (isSplit) {
			synchronized (uriToResource) {
				HawkFileResourceImpl resource = uriToResource.get(fullURL);
				if (resource == null) {
					/*
					 * We can't use the createResource method in the resource
					 * set, as that might invoke another factory, and we need
					 * the new resource to be a Hawk file resource.
					 */
					resource = new HawkFileResourceImpl(URI.createURI(fullURL), this);
					getResourceSet().getResources().add(resource);
					uriToResource.put(fullURL, resource);
					uriToFileNode.put(fullURL, fileNode);
				}

				if (modelElementNode.getContainer() == null) {
					resource.getContents().add(eob);
				}
				resource.addFragment(modelElementNode.getNodeId(), modelElementNode.getElementId());
			}
		} else {
			if (modelElementNode.getContainer() == null) {
				getContents().add(eob);
			}
		}
	}

	private void removeRedundantRoot(EObject child) {
		final Resource r = child.eResource();
		if (r != null && child.eContainer() != null && child.eResource() == child.eContainer().eResource() && child.eResource().getContents().contains(child)) {
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

	public IModelIndexer getIndexer() {
		return indexer;
	}

	@Override
	public void markChanged(final EObject eob) {
		dirtyObjects.put(eob, 1);
	}
}
