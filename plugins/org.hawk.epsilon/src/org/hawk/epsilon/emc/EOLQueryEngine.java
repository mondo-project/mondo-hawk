/*******************************************************************************
 * Copyright (c) 2011-2017 The University of York, Aston University.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - cleanup and bug fixes, refactor into two levels
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.exceptions.EolInternalException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.EolUndefinedVariableException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.execute.control.DefaultExecutionController;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.eclipse.epsilon.eol.execute.introspection.IPropertySetter;
import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolSequence;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.core.util.Utils;
import org.hawk.epsilon.emc.contextful.CEOLQueryEngine;
import org.hawk.epsilon.emc.optimisation.OptimisableCollection;
import org.hawk.epsilon.emc.pgetters.GraphPropertyGetter;
import org.hawk.epsilon.emc.tracking.AccessListener;
import org.hawk.epsilon.emc.wrappers.FileNodeWrapper;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.epsilon.emc.wrappers.MetamodelNodeWrapper;
import org.hawk.epsilon.emc.wrappers.TypeNodeWrapper;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;
import org.hawk.graph.updater.DirtyDerivedFeaturesListener;
import org.hawk.graph.updater.GraphModelInserter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes a Hawk instance as an Epsilon model and adds support for EOL queries to Hawk.
 * Other Epsilon languages may inherit from this class, redefining the {@link #createModule()}
 * method and being careful with derived attributes (if they support them at all).
 *
 * TODO: investigate how this class could be broken up into model + proper query engine.
 * Right now the two seem to be deeply intertwined and separating them would break backwards
 * compatibility.
 */
public class EOLQueryEngine extends AbstractHawkModel implements IQueryEngine {

	protected class IGraphIterableCollection implements Collection<GraphNodeWrapper> {
		private final IGraphIterable<? extends IGraphNode> iterableNodes;

		protected IGraphIterableCollection(IGraphIterable<? extends IGraphNode> iterableNodes) {
			this.iterableNodes = iterableNodes;
		}

		@Override
		public int size() {
			return iterableNodes.size();
		}

		@Override
		public boolean isEmpty() {
			return iterableNodes.size() > 0;
		}

		@Override
		public boolean contains(Object o) {
			for (final Iterator<GraphNodeWrapper> itN = iterator(); itN.hasNext(); ) {
				if (itN.next().equals(o)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Iterator<GraphNodeWrapper> iterator() {
			final Iterator<? extends IGraphNode> itNodes = iterableNodes.iterator();
			return new Iterator<GraphNodeWrapper>() {
				@Override
				public boolean hasNext() {
					return itNodes.hasNext();
				}

				@Override
				public GraphNodeWrapper next() {
					return new GraphNodeWrapper(itNodes.next(), EOLQueryEngine.this);
				}
			};
		}

		@Override
		public Object[] toArray() {
			return toArray(new Object[size()]);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			if (a.length < size()) {
				final T[] result = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
				return toArray(result);
			}

			int i = 0;
			for (final Iterator<GraphNodeWrapper> itN = iterator(); itN.hasNext(); i++) {
				final GraphNodeWrapper n = itN.next();
				a[i] = (T) n;
			}
			while (i < a.length) {
				a[i++] = null;
			}

			return a;
		}

		@Override
		public boolean add(GraphNodeWrapper e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object e : c) {
				if (!contains(e)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends GraphNodeWrapper> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Allows a query to be cancelled by the user according to an arbitrary binary flag.
	 * This could be used e.g. from an Eclipse job listener.
	 */
	public static class SettableExecutionController extends DefaultExecutionController {
		private volatile boolean isTerminated = false;

		@Override
		public boolean isTerminated() {
			return isTerminated;
		}

		public void setTerminated(boolean terminated) {
			this.isTerminated = terminated;
		}
	}

	interface Function3<T, U, V> {
		V apply(T t, U u);
	}

	public static final String TYPE = "org.hawk.epsilon.emc.EOLQueryEngine";

	private static final Logger LOGGER = LoggerFactory.getLogger(EOLQueryEngine.class);
	private static final String MMURI_TYPE_SEPARATOR = "::";
	private static final String ANY_TYPE = new EolAnyType().getName();
	
	/* TODO: these two should not have to be static.*/
	protected IModelIndexer indexer = null;
	protected IGraphDatabase graph = null;

	protected IGraphNodeIndex metamodeldictionary;
	protected Set<String> defaultNamespaces = null;
	protected GraphPropertyGetter propertyGetter;

	/** Speeds up repeated queries for type nodes. */
	private Map<String, List<IGraphNode>> typeNodesCache = new HashMap<>();

	/** Do not use OptimisedCollection unless we can benefit from it. */
	private boolean useOptimisableCollection;
	
	/**
	 * Returns all of the contents of the database in the form of lightweight
	 * {@link GraphNodeWrapper} objects.
	 */
	@Override
	public Collection<?> allContents() {
		final IGraphIterable<? extends IGraphNode> iterableNodes = graph.allNodes(ModelElementNode.OBJECT_VERTEX_LABEL);
		final Collection<GraphNodeWrapper> allContents = new IGraphIterableCollection(iterableNodes);
		broadcastAllOfXAccess(allContents);
		return allContents;
	}

	@Override
	public Object createInstance(String metaClassName)
			throws EolModelElementTypeNotFoundException, EolNotInstantiableModelElementTypeException {
		throw new EolNotInstantiableModelElementTypeException("Hawk Model Index", metaClassName);
	}

	@Override
	public void deleteElement(Object arg0) throws EolRuntimeException {
		throw new EolRuntimeException("Hawk Model Index: this type of model cannot create/delete elements");
	}

	public Collection<Object> getAllOf(String typeName, final String typeorkind) throws EolModelElementTypeNotFoundException, EolInternalException {
		try {
			final List<IGraphNode> typeNodes = getTypeNodes(typeName);
			if (typeNodes.size() == 1) {
				return getAllOf(typeNodes.get(0), typeorkind);
			}
		} catch (Exception e) {
			throw new EolInternalException(e);
		}

		throw new EolModelElementTypeNotFoundException(this.getName(), typeName);
	}

	/**
	 * Convenience version of
	 * {@link #getAllOf(String, String, String, String, String)} that looks in all
	 * repositories.
	 */
	public Collection<GraphNodeWrapper> getAllOf(final String metamodelURI, final String typeName, final String filePatterns)
			throws EolInternalException, EolModelElementTypeNotFoundException {
		return getAllOf(metamodelURI, typeName, "*", filePatterns);
	}

	/**
	 * Finds all the instances of a certain type (including subtypes) contained in
	 * the specified repository and file patterns. This version assumes we will provide
	 * a limited number of files and that these will be small - we start from the files.
	 *
	 * @param metamodelURI
	 *            URI of the metamodel, e.g.
	 *            <code>http://eclipse.org/example</code>.
	 * @param typeName
	 *            Name of the type within the metamodel, e.g.
	 *            <code>Subsystem</code>.
	 * @param repoPattern
	 *            Pattern of the repository or repositories to use, e.g.
	 *            <code>file:/*</code> or <code>project:/resource</code>.
	 * @param filePatterns
	 *            Comma-separated list of file patterns, such as
	 *            <code>/a/b/c.xmi</code> or <code>/d/*</code>.
	 *            If URI-invalid characters are found in the pattern,
	 *            they will be URI-encoded.
	 * @throws EolInternalException
	 *             Error while retrieving the type/file nodes.
	 * @throws EolModelElementTypeNotFoundException
	 *             Could not find the specified type.
	 */
	public Collection<GraphNodeWrapper> getAllOf(final String metamodelURI, final String typeName, final String repoPattern, final String filePatterns)
		throws EolInternalException, EolModelElementTypeNotFoundException
	{
		try {
			List<IGraphNode> typeNodes = getTypeNodes(metamodelURI, typeName);
			if (typeNodes.size() == 1) {
				final TypeNode targetTypeNode = new TypeNode(typeNodes.get(0));

				final List<GraphNodeWrapper> results = new ArrayList<>();
				final Set<FileNode> fileNodes = new GraphWrapper(graph).getFileNodes(
					Collections.singleton(repoPattern),
					Arrays.asList(filePatterns.split(",")));

				for (FileNode fn : fileNodes) {
					for (ModelElementNode me : fn.getModelElements()) {
						if (me.isOfKind(targetTypeNode)) {
							results.add(new GraphNodeWrapper(me.getNode(), this));
						}
					}
				}

				return results;
			}

		} catch (Exception e) {
			throw new EolInternalException(e);
		}

		throw new EolModelElementTypeNotFoundException(this.getName(), typeName);
	}

	public Collection<Object> getAllOf(IGraphNode typeNode, final String typeorkind) {
		Collection<Object> nodes = createAllOfCollection(typeNode);

		for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {
			nodes.add(new GraphNodeWrapper(n.getStartNode(), this));
		}

		broadcastAllOfXAccess(nodes);
		return nodes;
	}

	protected Collection<Object> createAllOfCollection(IGraphNode typeNode) {
		Collection<Object> nodes = useOptimisableCollection
			? new OptimisableCollection(this, new GraphNodeWrapper(typeNode, this)) : new EolSequence<>();
		return nodes;
	}

	public List<IGraphNode> getTypeNodes(String typeName) {
		List<IGraphNode> typeNodes = typeNodesCache.get(typeName);
		if (typeNodes == null) {
			typeNodes = computeTypeNodes(typeName);
			typeNodesCache.put(typeName, typeNodes);
		}
		return typeNodes;
	}

	protected List<IGraphNode> computeTypeNodes(String typeName) {
		final int idxColon = typeName.lastIndexOf(MMURI_TYPE_SEPARATOR);
		if (idxColon != -1) {
			final String epackage = typeName.substring(0, idxColon);
			final String type = typeName.substring(idxColon + MMURI_TYPE_SEPARATOR.length()); 
			return getTypeNodes(epackage, type);
		} else {
			final Iterator<? extends IGraphNode> packs = metamodeldictionary.query("id", "*").iterator();
			final List<IGraphNode> candidates = new LinkedList<IGraphNode>();

			while (packs.hasNext()) {
				IGraphNode pack = packs.next();
				for (IGraphEdge n : pack.getIncomingWithType("epackage")) {
	
					final IGraphNode othernode = n.getStartNode();
					final Object id = othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
					if (id.equals(typeName)) {
						candidates.add(othernode);
					}
				}
			}

			if (candidates.size() == 1) {
				return candidates;
			} else if (candidates.size() > 1) {
				// use default namespaces to limit types
				for (Iterator<IGraphNode> it = candidates.iterator(); it.hasNext();) {
					IGraphNode n = it.next();
					String metamodel = new TypeNode(n).getMetamodelURI();
					if (defaultNamespaces != null && !defaultNamespaces.isEmpty() && !defaultNamespaces.contains(metamodel)) {
						it.remove();
					}
				}
	
				return candidates;
			}
		}

		return Collections.emptyList();
	}

	protected List<IGraphNode> getTypeNodes(String mmURI, String type) {
		Iterator<? extends IGraphNode> itPack = metamodeldictionary.get("id", mmURI).iterator();
		if (!itPack.hasNext()) {
			throw new NoSuchElementException("Could not find the metamodel node for " + mmURI);
		}

		IGraphNode pack = itPack.next();
		for (IGraphEdge r : pack.getIncomingWithType("epackage")) {
			IGraphNode othernode = r.getStartNode();
			if (othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(type)) {
				return Collections.singletonList(othernode);
			}
		}
		return Collections.emptyList();
	}

	protected void broadcastAllOfXAccess(Iterable<?> ret) {
		/*
		 * TODO can optimise by not keeping all the nodes of type/kind but the
		 * concept of type/kind and only update the attr if a new node is added
		 * or one is removed.
		 */
		if (((GraphPropertyGetter) propertyGetter).getBroadcastStatus()) {
			for (Object n : ret) {
				final AccessListener accessListener = ((GraphPropertyGetter) propertyGetter).getAccessListener();
				final String sID = ((GraphNodeWrapper) n).getId() + "";
				accessListener.accessed(sID, "property_unused_type_or_kind");
			}
		}
	}

	@Override
	public Object getElementById(String id) {

		IGraphNode ret = graph.getNodeById(id);

		return ret == null ? null : new GraphNodeWrapper(ret, this);

	}

	@Override
	public String getElementId(Object arg0) {
		if (arg0 instanceof GraphNodeWrapper)
			return ((GraphNodeWrapper) arg0).getId() + "";
		else
			return null;
	}

	@Override
	public String getTypeNameOf(Object arg0) {
		String ret = null;
		try {
			Object type = getTypeOf(arg0);
			IGraphNode typeNode = ((GraphNodeWrapper) type).getNode();

			ret = typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return ret;
	}

	@Override
	public TypeNodeWrapper getTypeOf(Object arg0) {
		IGraphNode objectNode = ((GraphNodeWrapper) arg0).getNode();
		TypeNode typeNode = new ModelElementNode(objectNode).getTypeNode();
		if (typeNode != null) {
			return new TypeNodeWrapper(typeNode, this);
		} else {
			return null;
		}
	}

	@Override
	public FileNodeWrapper getFileOf(Object arg0) {
		IGraphNode objectNode = ((GraphNodeWrapper) arg0).getNode();
		FileNode fileNode = new ModelElementNode(objectNode).getFileNode();
		if (fileNode != null) {
			return new FileNodeWrapper(fileNode, this);
		} else {
			return null;
		}
	}

	@Override
	public List<FileNodeWrapper> getFilesOf(Object arg0) {
		List<FileNodeWrapper> ret = new ArrayList<>();
		IGraphNode objectNode = ((GraphNodeWrapper) arg0).getNode();
		for (FileNode fn : new ModelElementNode(objectNode).getFileNodes()) {
			ret.add(new FileNodeWrapper(fn, this));
		}
		return ret;
	}

	@Override
	public boolean hasType(String type) {
		return getTypeNodes(type).size() == 1;
	}

	@Override
	public boolean isModelElement(Object arg0) {
		return arg0 instanceof GraphNodeWrapper;
	}

	@Override
	public void load() throws EolModelLoadingException {
		if (graph != null)
			load((IModelIndexer) null);
		else
			throw new EolModelLoadingException(new Exception("load called with no graph store initialized"), this);
	}

	public void load(IModelIndexer m) throws EolModelLoadingException {
		if (m != null) {
			indexer = m;
			graph = m.getGraph();
			aliases.add(m.getName());
		}

		if (name == null) {
			name = "Model";
		}

		if (propertyGetter == null || propertyGetter.getGraph() != graph) {
			propertyGetter = createContextlessPropertyGetter();
		}

		if (graph != null) {
			try (IGraphTransaction tx = graph.beginTransaction()) {
				metamodeldictionary = graph.getMetamodelIndex();
				useOptimisableCollection = !indexer.getIndexedAttributes().isEmpty() || !indexer.getDerivedAttributes().isEmpty();

				tx.success();
			} catch (Exception e) {
				LOGGER.error("Could not retrieve the metamodel index", e);
			}
		} else {
			throw new EolModelLoadingException(new Exception("Attempt to load a model from an invalid graph: " + graph), this);
		}
	}

	@Override
	public boolean owns(Object arg0) {
		if (arg0 instanceof GraphNodeWrapper) {
			final GraphNodeWrapper gnw = (GraphNodeWrapper) arg0;
			if (gnw.getContainerModel() == null || gnw.getContainerModel() == this) {
				return true;
			} else {
				LOGGER.warn("owns failed on {} with getContainerModel(): {} and 'this': {}", arg0, gnw.getContainerModel(), this);
			}
		}

		return false;
	}

	@Override
	public boolean isOfKind(Object instance, String metaClass) throws EolModelElementTypeNotFoundException {
		return isOf(instance, metaClass, ModelElementNode::isOfKind);
	}

	@Override
	public boolean isOfType(Object instance, String metaClass) throws EolModelElementTypeNotFoundException {
		return isOf(instance, metaClass, ModelElementNode::isOfType);
	}

	private boolean isOf(Object instance, String metaClass, Function3<ModelElementNode, IGraphNode, Boolean> isOfCheck) {
		if (!(instance instanceof GraphNodeWrapper)) {
			return false;
		}
		if (ANY_TYPE.equals(metaClass)) {
			// Needed to support the 'Any' supertype in Epsilon (useful in EPL)
			return true;
		}

		final List<IGraphNode> typeNodes = getTypeNodes(metaClass);
		if (typeNodes.isEmpty()) {
			return false;
		} else {
			final GraphNodeWrapper gnw = (GraphNodeWrapper) instance;
			final ModelElementNode men = new ModelElementNode(gnw.getNode());
			return isOfCheck.apply(men, typeNodes.get(0));
		}
	}

	@Override
	public boolean knowsAboutProperty(Object instance, String property) {
		if (!owns(instance))
			return false;

		if (instance instanceof GraphNodeWrapper)
			return true;

		return false;

	}

	@Override
	public IPropertyGetter getPropertyGetter() {
		if (propertyGetter == null) {
			LOGGER.warn("null property getter, was load() called?");
		}

		return propertyGetter;
	}

	@Override
	public IPropertySetter getPropertySetter() {
		return null;
	}

	public IGraphDatabase getBackend() {
		return graph;
	}

	public List<TypeNodeWrapper> getTypes() {
		final List<TypeNodeWrapper> nodes = new ArrayList<>();
		for (IGraphNode n : graph.getMetamodelIndex().query("*", "*")) {
			final MetamodelNode pn = new MetamodelNode(n);
			for (TypeNode tn : pn.getTypes()) {
				nodes.add(new TypeNodeWrapper(tn, this));
			}
		}
		return nodes;
	}

	public List<MetamodelNodeWrapper> getMetamodels() {
		final List<MetamodelNodeWrapper> nodes = new ArrayList<>();
		for (IGraphNode n : graph.getMetamodelIndex().query("*", "*")) {
			final MetamodelNode pn = new MetamodelNode(n);
			nodes.add(new MetamodelNodeWrapper(pn, this));
		}
		return nodes;
	}

	/**
	 * Returns the collection of all the files indexed in the graph.
	 */
	public Set<FileNodeWrapper> getFiles() {
		Set<FileNodeWrapper> allFNW = new HashSet<>();
		for (IGraphNode n : graph.allNodes(FileNode.FILE_NODE_LABEL)) {
			allFNW.add(new FileNodeWrapper(new FileNode(n), this));
		}
		return allFNW;
	}

	// deriving attributes
	@Override
	public AccessListener calculateDerivedAttributes(IModelIndexer m, Iterable<IGraphNode> nodes) {
		final boolean enableDebug = false;

		indexer = m;
		graph = m.getGraph();
		if (propertyGetter == null) {
			propertyGetter = createContextlessPropertyGetter();
		}

		try {
			load(m);
		} catch (EolModelLoadingException e2) {
			e2.printStackTrace();
		}

		// listen to accesses
		GraphPropertyGetter pg = null;
		if (!enableDebug) {
			pg = (GraphPropertyGetter) getPropertyGetter();
			pg.setBroadcastAccess(true);
		}

		// Compute the derived attributes
		final Map<String, EolModule> cachedModules = new HashMap<String, EolModule>();
		try (IGraphTransaction t = graph.beginTransaction()) {
			for (IGraphNode n : nodes) {
				calculateDerivedAttributes(cachedModules, n);
			}
			t.success();
		} catch (Exception e) {
			LOGGER.error("Failed to compute the derived attributes", e);
		}

		if (!enableDebug) {
			pg.setBroadcastAccess(false);
			return pg.getAccessListener();
		}
		return null;
	}

	protected GraphPropertyGetter createContextlessPropertyGetter() {
		return new GraphPropertyGetter(graph, this);
	}

	protected void calculateDerivedAttributes(Map<String, EolModule> cachedModules, IGraphNode n) {
		for (String s : n.getPropertyKeys()) {
			String prop = n.getProperty(s).toString();

			if (prop.startsWith(DirtyDerivedFeaturesListener.NOT_YET_DERIVED_PREFIX)) {
				Object derived = "DERIVATION_EXCEPTION";
				try {
					derived = new DeriveFeature().deriveFeature(cachedModules, indexer, n, this, s, prop);
				} catch (Exception e) {
					LOGGER.error("Error while deriving feature " + prop, e);
				}

				// Unset the current value (if there is any)
				final String derivedEdgeLabel = ModelElementNode.DERIVED_EDGE_PREFIX + s;
				for (IGraphEdge edge : n.getOutgoingWithType(derivedEdgeLabel)) {
					LOGGER.debug("Clearing edge {}", edge.getType());
					edge.delete();
				}
				n.removeProperty(s);

				// Set the new value
				if (derived instanceof Object[] && ((Object[])derived).length > 0 && ((Object[])derived)[0] instanceof GraphNodeWrapper) {
					GraphNodeWrapper[] nodes = (GraphNodeWrapper[])derived;

					// Replace existing edges with new ones
					for (GraphNodeWrapper gw : nodes) {
						graph.createRelationship(n, gw.getNode(), derivedEdgeLabel);
					}
				} else if (derived instanceof GraphNodeWrapper) {
					GraphNodeWrapper gw = (GraphNodeWrapper) derived;
					graph.createRelationship(n, gw.getNode(), derivedEdgeLabel);
				} else if (derived != null) {
					n.setProperty(s, derived);
				} else {
					n.setProperty(s, new String[0]);
				}

				final IGraphNode elementNode = n.getIncoming().iterator().next().getStartNode();
				final String idxName = n.getProperty(GraphModelInserter.DERIVED_IDXNAME_NODEPROP).toString();
				final IGraphNodeIndex idxNodeByDerivedValue = graph.getOrCreateNodeIndex(idxName);

				// flatten multi-valued derived features for indexing
				if (derived != null) {
					if (derived.getClass().getComponentType() != null || derived instanceof Collection<?>) {
						derived = new Utils().toString(derived);
					}

					// TODO: need to test how this works with derived edges
					idxNodeByDerivedValue.add(elementNode, s, derived);
				}
			}
		}

		IGraphNodeIndex derivedProxyDictionary = graph.getOrCreateNodeIndex("derivedproxydictionary");
		derivedProxyDictionary.remove(n);
	}

	@Override
	public String getType() {
		/*
		 * This *must* be overridden in subclasses, otherwise Hawk may pick the wrong
		 * query engine! It happened in GH issue #78.
		 */
		return TYPE;
	}

	@Override
	public List<String> validate(String derivationlogic) {
		final IEolModule module = createModule();

		final List<String> ret = new LinkedList<>();
		try {
			module.parse(derivationlogic);
			for (ParseProblem p : module.getParseProblems()) {
				ret.add(p.toString());
			}
		} catch (Exception e) {
			LOGGER.error("Error while parsing EOL", e);
		}
		return ret;
	}

	@Override
	public Object query(IModelIndexer m, String query, Map<String, Object> context)
			throws InvalidQueryException, QueryExecutionException {
		/*
		 * Check if we're in the right state: we should not use {@link
		 * IModelIndexer#waitFor} here, as that would introduce unwanted
		 * synchronisation (reducing peak throughput for some cases).
		 */
		final HawkState currentState = m.getCompositeStateListener().getCurrentState();
		if (currentState != HawkState.RUNNING) {
			throw new QueryExecutionException(
					String.format("Cannot run the query, as the indexer is not in the RUNNING state: it is %s instead.",
							currentState));
		}

		if (context != null && (context.containsKey(PROPERTY_REPOSITORYCONTEXT) || context.containsKey(PROPERTY_FILECONTEXT) || context.containsKey(PROPERTY_SUBTREECONTEXT))) {
			return contextfulQuery(m, query, context);
		} else {
			return contextlessQuery(m, query, context);
		}
	}

	protected Object contextlessQuery(IModelIndexer m, String query, Map<String, Object> context)
			throws QueryExecutionException, InvalidQueryException {
		String defaultnamespaces = null;
		if (context != null)
			defaultnamespaces = (String) context.get(PROPERTY_DEFAULTNAMESPACES);

		/*
		 * We need to always create a new engine for every query: reusing the
		 * same engine would be thread-unsafe.
		 */
		final EOLQueryEngine q = new EOLQueryEngine();
		try {
			q.load(m);
			q.setDefaultNamespaces(defaultnamespaces);
		} catch (EolModelLoadingException e) {
			throw new QueryExecutionException("Loading of EOLQueryEngine failed");
		}

		final IEolModule module = createModule();
		parseQuery(query, context, q, module);
		return q.runQuery(module);
	}

	protected Object contextfulQuery(IModelIndexer m, String query, Map<String, Object> context)
			throws QueryExecutionException, InvalidQueryException {
		CEOLQueryEngine q = new CEOLQueryEngine();
		try {
			q.load(m);
			q.setContext(context);
		} catch (EolModelLoadingException e) {
			throw new QueryExecutionException("Loading of EOLQueryEngine failed");
		}
		LOGGER.debug("Graph path: {}", graph.getPath());

		final IEolModule module = createModule();
		parseQuery(query, context, q, module);
		return q.runQuery(module);
	}

	// IQueryEngine part //////////////////////////////////////////////////////
	
	/**
	 * Query engines that add support for other Epsilon languages should redefine this method.
	 */
	protected IEolModule createModule() {
		return new EolModule();
	}

	protected void parseQuery(String query, Map<String, Object> context, final EOLQueryEngine model,
			final IEolModule module) throws InvalidQueryException {
		try {
			module.parse(query);
			if (!module.getParseProblems().isEmpty()) {
				StringBuilder sb = new StringBuilder("Query failed to parse correctly:");
				for (ParseProblem problem : module.getParseProblems()) {
					sb.append("\n");
					sb.append(problem.toString());
				}
				throw new InvalidQueryException(sb.toString());
			}
		} catch (Exception ex) {
			throw new InvalidQueryException(ex);
		}

		module.getContext().getModelRepository().addModel(model);
		addQueryArguments(context, module);
		if (context != null && context.containsKey(IQueryEngine.PROPERTY_CANCEL_CONSUMER)) {
			@SuppressWarnings("unchecked")
			final Consumer<Runnable> cancelProvider = (Consumer<Runnable>) context.get(IQueryEngine.PROPERTY_CANCEL_CONSUMER);

			final SettableExecutionController controller = new SettableExecutionController();
			module.getContext().getExecutorFactory().setExecutionController(controller);
			cancelProvider.accept(() -> {
				controller.setTerminated(true);
			});
		}
	}

	protected Object runQuery(final IEolModule module) throws QueryExecutionException {
		Object ret = null;
		try (IGraphTransaction tx = graph.beginTransaction()) {
			ret = module.execute();
			tx.success();
		} catch (EolUndefinedVariableException ex) {
			// Provide more details than Epsilon about ambiguous intra-model type references
			try (IGraphTransaction tx = graph.beginTransaction()) {
				/*
				 * Use the same EOLQueryEngine as in the model - if using a separate object
				 * (e.g. the time-aware one) this EOLQueryEngine might not be loaded.
				 */
				final EOLQueryEngine eolQuery = (EOLQueryEngine) module.getContext().getModelRepository().getModels().get(0);
				final List<IGraphNode> typeNodes = eolQuery.getTypeNodes(ex.getVariableName());

				final StringBuilder sb = new StringBuilder("Ambiguous type reference '" + ex.getVariableName() + "' across these metamodels:\n");
				for (IGraphNode typeNode : typeNodes) {
					sb.append("\n* ");
					sb.append(new TypeNode(typeNode).getMetamodelURI());
				}
				sb.append("\n\nSpecify the desired one in the default namespaces to resolve the ambiguity.");
				tx.success();

				if (typeNodes.size() > 1) {
					throw new QueryExecutionException(sb.toString());
				} else {
					throw new QueryExecutionException(ex);
				}
			} catch (QueryExecutionException e) {
				throw e;
			} catch (Exception e) {
				throw new QueryExecutionException(e);
			}
		} catch (Exception e) {
			throw new QueryExecutionException(e);
		}

		return ret;
	}

	@SuppressWarnings("unchecked")
	protected void addQueryArguments(Map<String, Object> context, final IEolModule module) {
		if (context != null) {
			final Map<String, Object> args = (Map<String, Object>) context.get(PROPERTY_ARGUMENTS);
			if (args != null) {
				for (Entry<String, Object> entry : args.entrySet()) {
					module.getContext().getFrameStack().putGlobal(new Variable(entry.getKey(), entry.getValue(), null));
				}
			}
		}
	}

	@Override
	public void setDefaultNamespaces(String namespaces) {
		// set default packages if applicable
		try {
			defaultNamespaces = new HashSet<String>();

			if (namespaces != null && !namespaces.trim().equals("")) {
				String[] eps = ((String) namespaces).split(",");
				for (String s : eps) {
					defaultNamespaces.add(s.trim());
				}
			}

		} catch (Throwable t) {
			LOGGER.error("Setting default namespaces failed, malformed property: " + namespaces, t);
		}
	}

	@Override
	public String getHumanReadableName() {
		return "EOL Query Engine";
	}

}
