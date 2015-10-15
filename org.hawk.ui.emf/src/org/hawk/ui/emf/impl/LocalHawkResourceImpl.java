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
package org.hawk.ui.emf.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.ui.emf.Activator;
import org.hawk.ui2.util.HUIManager;

import net.sf.cglib.proxy.CallbackHelper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;

/**
 * EMF driver that reads a local model from a Hawk index.
 *
 * TODO: update on the fly.
 */
public class LocalHawkResourceImpl extends ResourceImpl implements IGraphChangeListener {

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

	private LazyResolver lazyResolver = null;

	private final Map<String, EObject> nodeIdToEObjectMap = new HashMap<>();

	/** Map from classes to factories of instances instrumented by CGLIB for lazy loading. */
	private Map<Class<?>, net.sf.cglib.proxy.Factory> factories = null;

	/** Interceptor to be reused by all CGLIB {@link Enhancer}s in lazy loading modes. */
	private final MethodInterceptor methodInterceptor = new MethodInterceptor() {
		@SuppressWarnings("unchecked")
		@Override
		public Object intercept(final Object o, final Method m, final Object[] args, final MethodProxy proxy) throws Throwable {
			/*
			 * We need to serialize modifications from lazy loading + change
			 * notifications, for consistency and for the ability to signal if
			 * an EMF notification comes from lazy loading or not.
			 */
			final EStructuralFeature sf = (EStructuralFeature)args[0];
			if (sf instanceof EReference) {
				synchronized(nodeIdToEObjectMap) {
					final EReference ref = (EReference)sf;
					final EObject eob = (EObject)o;

					/*
					 * When we resolve a reference, it may be a containment or
					 * container reference: need to adjust the list of root
					 * elements then.
					 */
					getLazyResolver().resolve(eob, sf);
					Object superValue = proxy.invokeSuper(o, args);
					if (superValue != null) {
						if (ref.isContainer()) {
							getContents().remove(eob);
						} else if (ref.isContainment()) {
							if (ref.isMany()) {
								for (EObject child : (Iterable<EObject>) superValue) {
									getContents().remove(child);
								}
							} else {
								getContents().remove((EObject) superValue);
							}
						}
					}

					return superValue;
				}
			}
			return proxy.invokeSuper(o, args);
		}
	};

	private HModel hawkModel;

	public LocalHawkResourceImpl() {}

	public LocalHawkResourceImpl(final URI uri) {
		super(uri);
	}

	@Override
	public void load(final Map<?, ?> options) throws IOException {
		if ("hawk+local".equals(uri.scheme())) {
			// We already have an instance name: no need to create an InputStream from the URI
			doLoad(uri.host());
		} else {
			// Let Ecore create an InputStream from the URI and call doLoad(InputStream, Map)
			super.load(options);
		}
	}

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

	public void doLoad(final String hawkInstance) throws IOException {
		try {
			final HManager manager = HUIManager.getInstance();
			hawkModel = manager.getHawkByName(hawkInstance);
			if (hawkModel == null) {
				throw new NoSuchElementException(String.format("No Hawk instance exists with name '%s'", hawkInstance));
			}
			if (!hawkModel.isRunning()) {
				// We need an IOException so EMF will display it properly
				throw new IOException(String.format("The Hawk instance with name '%s' is not running: please start it first", hawkInstance));
			}

			final GraphWrapper gw = new GraphWrapper(hawkModel.getGraph());
			try (IGraphTransaction tx = hawkModel.getGraph().beginTransaction()) {
				// TODO add back ability for filtering repos/files
				final List<String> all = Arrays.asList("*");
				for (FileNode fileNode : gw.getFileNodes(all, all)) {
					final Iterable<ModelElementNode> elems = fileNode.getRootModelElements();
					createEObjectTree(elems);
				}
				tx.success();
			}

			hawkModel.addGraphChangeListener(this);
			setLoaded(true);
		} catch (final IOException e) {
			Activator.logError("I/O exception while opening model", e);
			throw e;
		} catch (final Exception e) {
			Activator.logError("Exception while loading model", e);
		}
	}

	public EList<EObject> fetchNodes(final List<String> ids) throws Exception {
		// Filter the objects that need to be retrieved
		final List<String> toBeFetched = new ArrayList<>();
		for (final String id : ids) {
			if (!nodeIdToEObjectMap.containsKey(id)) {
				toBeFetched.add(id);
			}
		}

		// Fetch the eObjects, decode them and resolve references
		if (!toBeFetched.isEmpty()) {
			try (IGraphTransaction tx = hawkModel.getGraph().beginTransaction()) {
				final GraphWrapper gw = new GraphWrapper(hawkModel.getGraph());

				final List<ModelElementNode> elems = new ArrayList<>();
				for (String id : toBeFetched) {
					elems.add(gw.getModelElementNodeById(id));
				}
				createEObjectTree(elems);

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

	public EList<EObject> fetchNodes(final EClass eClass) throws Exception {
		try (IGraphTransaction tx = hawkModel.getGraph().beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(hawkModel.getGraph());
			final MetamodelNode mn = gw.getMetamodelNodeByNsURI(eClass.getEPackage().getNsURI());
			for (TypeNode tn : mn.getTypes()) {
				if (eClass.getName().equals(tn.getTypeName())) {
					Iterable<ModelElementNode> instances = tn.getAll();
					createEObjectTree(instances);

					final EList<EObject> l = new BasicEList<EObject>();
					for (ModelElementNode en : instances) {
						l.add(nodeIdToEObjectMap.get(en.getId()));
					}
					return l;
				}
			}
			tx.success();
		}

		Activator.logWarn(String.format("Could not find a type node for EClass %s:%s", eClass.getEPackage().getNsURI(), eClass.getName()));
		return new BasicEList<EObject>();
	}

	protected EList<EObject> fetchNodesByQueryResults(final List<Object> queryResults) throws Exception {
		final List<String> ids = new ArrayList<>();
		for (Object r : queryResults) {
			if (r instanceof IGraphNodeReference) {
				final IGraphNodeReference men = (IGraphNodeReference)r;
				ids.add(men.getId());
			}
		}
		return fetchNodes(ids);
	}

	public List<Object> fetchValuesByEClassifier(final EClassifier dataType) throws Exception {
		final Map<EClass, List<EStructuralFeature>> candidateTypes = fetchTypesWithEClassifier(dataType);

		final List<Object> values = new ArrayList<>();
		for (final Entry<EClass, List<EStructuralFeature>> entry : candidateTypes.entrySet()) {
			final EClass eClass = entry.getKey();
			final List<EStructuralFeature> attrsWithType = entry.getValue();
			for (final EObject eob : fetchNodes(eClass)) {
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

	public Map<EClass, List<EStructuralFeature>> fetchTypesWithEClassifier(final EClassifier dataType) throws Exception {
		try (IGraphTransaction tx = hawkModel.getGraph().beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(hawkModel.getGraph());

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
					Activator.logWarn(String.format("We do not have the '%s' EPackage in the registry, skipping", nsURI));
				}
			}

			tx.success();
			return candidateTypes;
		}
	}

	public Map<EObject, Object> fetchValuesByEStructuralFeature(final EStructuralFeature feature) throws Exception {
		final EClass featureEClass = feature.getEContainingClass();
		final EList<EObject> eobs = fetchNodes(featureEClass);

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
			fetchNodes(allPending);
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

	private EObject createEObject(final ModelElementNode me) {
		final Registry registry = getResourceSet().getPackageRegistry();
		final TypeNode typeNode = me.getTypeNode();
		final String nsURI = typeNode.getMetamodelURI();
		final EClass eClass = getEClass(nsURI, typeNode.getTypeName(), registry);
		final EObject obj = createInstance(eClass);

		nodeIdToEObjectMap.put(me.getId(), obj);

		Map<String, Object> attributeValues = new HashMap<>();
		Map<String, Object> referenceValues = new HashMap<>();
		me.getSlotValues(attributeValues, referenceValues);

		final EFactory factory = registry.getEFactory(nsURI);
		for (Map.Entry<String, Object> entry : attributeValues.entrySet()) {
			final String attrName = entry.getKey();
			final Object attrValue = entry.getValue();
			AttributeUtils.setAttribute(factory, eClass, obj, attrName, attrValue); 
		}
		for (Map.Entry<String, Object> entry : referenceValues.entrySet()) {
			final EReference ref = (EReference) eClass.getEStructuralFeature(entry.getKey());

			final EList<Object> referenced = new BasicEList<>();
			boolean hasLazy = false;
			if (entry.getValue() instanceof Collection) {
				for (Object o : (Collection<?>)entry.getValue()) {
					final String id = o.toString();
					final EObject existing = nodeIdToEObjectMap.get(id);
					if (existing != null) {
						referenced.add(existing);
					} else {
						referenced.add(id);
						hasLazy = true;
					}
				}
			} else {
				final String id = entry.getValue().toString();
				final EObject existing = nodeIdToEObjectMap.get(id);
				if (existing != null) {
					referenced.add(existing);
				} else {
					referenced.add(id);
					hasLazy = true;
				}
			}
			if (hasLazy) {
				getLazyResolver().markLazyReferences(obj, ref, referenced);
			} else if (ref.isMany()) {
				obj.eSet(ref, referenced);
			} else if (!referenced.isEmpty()) {
				obj.eSet(ref, referenced.get(0));
			}
		}

		return obj;
	}

	private EObject createInstance(final EClass eClass) {
		final Registry packageRegistry = getResourceSet().getPackageRegistry();
		final EFactory factory = packageRegistry.getEFactory(eClass.getEPackage().getNsURI());
		final EObject obj = factory.create(eClass);
		return createLazyLoadingInstance(eClass, obj.getClass());
	}

	private EObject createLazyLoadingInstance(final EClass eClass, final Class<?> klass) {
		/*
		 * We need to create a proxy to intercept eGet calls for lazy loading,
		 * but we need to use a subclass of the *real* implementation, or we'll
		 * have all kinds of issues with static metamodels (e.g. not using
		 * DynamicEObjectImpl).
		 */
		if (factories == null) {
			factories = new HashMap<>();
		}

		final net.sf.cglib.proxy.Factory factory = factories.get(klass);
		EObject o;
		if (factory == null) {
			
			final Enhancer enh = new Enhancer();
			final CallbackHelper helper = new CallbackHelper(klass, new Class[0]) {
				@Override
				protected Object getCallback(final Method m) {
					if ("eGet".equals(m.getName())
							&& m.getParameterTypes().length > 0
							&& EStructuralFeature.class.isAssignableFrom(m.getParameterTypes()[0])) {
						return methodInterceptor;
					} else {
						return NoOp.INSTANCE;
					}
				}
			};
			enh.setSuperclass(klass);

			/*
			 * We need both classloaders: the classloader of the class to be
			 * enhanced, and the classloader of this plugin (which includes
			 * CGLIB). We want the CGLIB classes to always resolve to the same
			 * Class objects, so this plugin's classloader *has* to go first.
			 */
			enh.setClassLoader(new BridgeClassLoader(
				this.getClass().getClassLoader(),
				klass.getClassLoader()));

			/*
			 * The objects created by the Enhancer implicitly implement the
			 * CGLIB Factory interface as well. According to CGLIB, going
			 * through the Factory is faster than recreating or reusing the
			 * Enhancer.
			 */
			enh.setCallbackFilter(helper);
			enh.setCallbacks(helper.getCallbacks());
			o = (EObject)enh.create();
			factories.put(klass, (net.sf.cglib.proxy.Factory)o);
		} else {
			o = (EObject) factory.newInstance(factory.getCallbacks());
		}

		/*
		 * A newly created and instrumented DynamicEObjectImpl won't have the
		 * eClass set. We need to redo that here.
		 */
		if (o instanceof DynamicEObjectImpl) {
			((DynamicEObjectImpl)o).eSetClass(eClass);
		}
		return o;
	}

	private List<EObject> createEObjectTree(final Iterable<ModelElementNode> elems) {
		final List<EObject> eObjects = new ArrayList<>();
		for (final ModelElementNode me : elems) {
			EObject eob = nodeIdToEObjectMap.get(me.getId());
			if (eob == null) {
				eob = createEObject(me);
				nodeIdToEObjectMap.put(me.getId(), eob);
				if (eob.eContainer() == null) {
					getContents().add(eob);
				}
			}
			eObjects.add(eob);
		}
		return eObjects;
	}

	private LazyResolver getLazyResolver() {
		if (lazyResolver == null) {
			lazyResolver = new LazyResolver(this);
		}
		return lazyResolver;
	}

	@Override
	protected void doLoad(final InputStream inputStream, final Map<?, ?> options) throws IOException {
		try (final BufferedReader bR = new BufferedReader(new InputStreamReader(inputStream))) {
			final String hawkInstance = bR.readLine();
			doLoad(hawkInstance);
		}
	}

	@Override
	protected void doUnload() {
		super.doUnload();

		if (hawkModel != null) {
			hawkModel.removeGraphChangeListener(this);
		}
		nodeIdToEObjectMap.clear();
		lazyResolver = null;
	}

	@Override
	protected void doSave(final OutputStream outputStream, final Map<?, ?> options) throws IOException {
		throw new UnsupportedOperationException("Hawk views are read-only");
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setModelIndexer(IModelIndexer m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void synchroniseStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void synchroniseEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changeStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changeSuccess() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changeFailure() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void modelElementAddition(VcsCommitItem s, IHawkObject element, IGraphNode elementNode,
			boolean isTransient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode, boolean isTransient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s, IHawkObject eObject, String attrName, Object oldValue,
			Object newValue, IGraphNode elementNode, boolean isTransient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s, IHawkObject eObject, String attrName,
			IGraphNode elementNode, boolean isTransient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source, IGraphNode destination, String edgelabel,
			boolean isTransient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source, IGraphNode destination, String edgelabel,
			boolean isTransient) {
		// TODO Auto-generated method stub
		
	}

}
