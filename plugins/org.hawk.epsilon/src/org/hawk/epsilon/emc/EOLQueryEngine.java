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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.exceptions.EolInternalException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.EolUndefinedVariableException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.eclipse.epsilon.eol.execute.introspection.IPropertySetter;
import org.eclipse.epsilon.eol.models.Model;
import org.eclipse.epsilon.eol.types.EolAnyType;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.core.util.Utils;
import org.hawk.graph.FileNode;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;
import org.hawk.graph.internal.updater.DirtyDerivedAttributesListener;
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
public class EOLQueryEngine extends AbstractEpsilonModel implements IQueryEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(EOLQueryEngine.class);

	public static final String TYPE = "org.hawk.epsilon.emc.EOLQueryEngine";
	private static final String ANY_TYPE = new EolAnyType().getName();

	protected final Set<String> cachedTypes = new HashSet<String>();
	protected final Map<String, OptimisableCollection> typeContents = new HashMap<>();
	protected final Map<String, OptimisableCollection> superTypeContents = new HashMap<>();

	/* TODO: these two should not have to be static.*/
	protected static IModelIndexer indexer = null;
	protected static IGraphDatabase graph = null;

	protected IGraphNodeIndex metamodeldictionary;

	protected StringProperties config = null;
	protected Set<String> defaultNamespaces = null;

	protected GraphPropertyGetter propertyGetter;

	/**
	 * Returns all of the contents of the database in the form of lightweight
	 * {@link GraphNodeWrapper} objects.
	 */
	@Override
	public Collection<?> allContents() {
		final Set<Object> allContents = new HashSet<Object>();

		for (IGraphNode node : graph.allNodes(ModelElementNode.OBJECT_VERTEX_LABEL)) {
			GraphNodeWrapper wrapper = new GraphNodeWrapper(node, this);
			allContents.add(wrapper);
		}
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

	public Collection<Object> getAllOf(IGraphNode typeNode, final String typeorkind) {
		OptimisableCollection nodes = new OptimisableCollection(this, new GraphNodeWrapper(typeNode, this));

		for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {
			nodes.add(new GraphNodeWrapper(n.getStartNode(), this));
		}
		broadcastAllOfXAccess(nodes);
		return nodes;
	}

	protected List<IGraphNode> getTypeNodes(String typeName) {
		if (typeName.contains("::")) {
			String ep = typeName.substring(0, typeName.indexOf("::"));
			IGraphNode pack = null;
	
			// operations on the graph
			// ...
	
			pack = metamodeldictionary.get("id", ep).getSingle();
	
			for (IGraphEdge r : pack.getIncomingWithType("epackage")) {
				IGraphNode othernode = r.getStartNode();
				if (othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
						.equals(typeName.substring(typeName.indexOf("::") + 2))) {
					return Collections.singletonList(othernode);
				}
			}
		} else {
	
			Iterator<IGraphNode> packs = metamodeldictionary.query("id", "*").iterator();
			LinkedList<IGraphNode> candidates = new LinkedList<IGraphNode>();
	
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

	private void broadcastAllOfXAccess(Iterable<Object> ret) {

		// TODO can optimise by not keeping all the nodes of type/kind but the
		// concept of type/kind and only update the attr if a new node is added
		// or one is removed

		if (((GraphPropertyGetter) propertyGetter).getBroadcastStatus()) {

			for (Object n : ret)
				((GraphPropertyGetter) propertyGetter).getAccessListener().accessed(((GraphNodeWrapper) n).getId() + "",
						"property_unused_type_or_kind");

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

		if (config == null)
			config = getDatabaseConfig();

		if (m != null) {
			indexer = m;
			graph = m.getGraph();
			aliases.add(m.getName());
		}

		if (propertyGetter == null || propertyGetter.getGraph() != graph)
			propertyGetter = new GraphPropertyGetter(graph, this);

		name = (String) config.get(EOLQueryEngine.PROPERTY_NAME);
		String aliasString = config.getProperty(Model.PROPERTY_ALIASES);
		boolean aliasStringIsValid = aliasString != null && aliasString.trim().length() > 0;
		String[] aliasArray = aliasStringIsValid ? aliasString.split(",") : new String[0];
		for (int i = 0; i < aliasArray.length; i++) {
			this.aliases.add(aliasArray[i].trim());
		}

		if (graph != null) {

			try (IGraphTransaction tx = graph.beginTransaction()) {
				metamodeldictionary = graph.getMetamodelIndex();
				tx.success();
			} catch (Exception e) {
				LOGGER.error("Could not retrieve the metamodel index", e);
			}

		} else
			throw new EolModelLoadingException(new Exception("Attempt to load a model from an invalid graph: " + graph),
					this);

		if (enableDebugOutput) {
			LOGGER.info("engine initialised with model named: {}, with alises: {}", name, aliases);
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
		if (!(instance instanceof GraphNodeWrapper)) {
			return false;
		}
		if (ANY_TYPE.equals(metaClass)) {
			// Needed to support the 'Any' supertype in Epsilon (useful in EPL)
			return true;
		}

		final GraphNodeWrapper gnw = (GraphNodeWrapper) instance;
		final ModelElementNode men = new ModelElementNode(gnw.getNode());
		return men.isOfKind(metaClass);
	}

	@Override
	public boolean isOfType(Object instance, String metaClass) throws EolModelElementTypeNotFoundException {
		if (!(instance instanceof GraphNodeWrapper)) {
			return false;
		}
		final GraphNodeWrapper gnw = (GraphNodeWrapper) instance;
		final ModelElementNode men = new ModelElementNode(gnw.getNode());
		return men.isOfType(metaClass);
	}

	public boolean isOf(Object instance, String metaClass, final String typeorkind)
			throws EolModelElementTypeNotFoundException {
		if (instance == null) {
			return false;
		}
		if (!(instance instanceof GraphNodeWrapper)) {
			return false;
		}

		String id = null;
		try {
			try {
				id = ((GraphNodeWrapper) instance).getTypeName();
			} catch (Exception e) {
				LOGGER.debug("warning: metaclass node asked for its type, ignored");
			}
		} catch (Exception e) {
			LOGGER.error("Could not retrieve type name", e);
		}

		if (id != null) {
			return metaClass.equals(id) || metaClass.equals(id.substring(id.lastIndexOf("/") + 1));
		} else {
			return false;
		}
	}

	/*
	 * TODO: move this to the Neo4j backend.
	 */
	protected StringProperties getDefaultDatabaseConfig() {
		final long x = Runtime.getRuntime().maxMemory() / 1000000 / 60;
		StringProperties defaultConfig = new StringProperties();
		defaultConfig.put("neostore.nodestore.db.mapped_memory", 5 * x + "M");
		defaultConfig.put("neostore.relationshipstore.db.mapped_memory", 15 * x + "M");
		defaultConfig.put("neostore.propertystore.db.mapped_memory", 20 * x + "M");
		defaultConfig.put("neostore.propertystore.db.strings.mapped_memory", 2 * x + "M");
		defaultConfig.put("neostore.propertystore.db.arrays.mapped_memory", x + "M");
		defaultConfig.put("keep_logical_logs", "false");

		File genericWorkspaceFile = new File("");

		defaultConfig.put(databaseLocation,
				new File(new File(genericWorkspaceFile.getAbsolutePath().replaceAll("\\\\", "/")).getParentFile()
						.getAbsolutePath()) + "/DB");

		defaultConfig.put("name", "Model");

		return defaultConfig;
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
		return null;// new NeoPropertySetter(graph, this);
	}

	public IGraphDatabase getBackend() {
		return graph;
	}

	protected void dumpDatabaseConfig() {
		for (Object c : config.keySet()) {
			LOGGER.debug(">" + c + " = " + config.get(c));
		}
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

		if (config == null)
			config = getDatabaseConfig();

		if (propertyGetter == null)
			propertyGetter = new GraphPropertyGetter(graph, this);

		StringProperties configuration = new StringProperties();
		configuration.put(EOLQueryEngine.PROPERTY_ENABLE_CACHING, true);
		setDatabaseConfig(configuration);
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

	protected void calculateDerivedAttributes(Map<String, EolModule> cachedModules, IGraphNode n) {
		for (String s : n.getPropertyKeys()) {
			String prop = n.getProperty(s).toString();

			if (prop.startsWith(DirtyDerivedAttributesListener.NOT_YET_DERIVED_PREFIX)) {
				Object derived = "DERIVATION_EXCEPTION";
				try {
					Object enablecaching = getDatabaseConfig().get(EOLQueryEngine.PROPERTY_ENABLE_CACHING);
					if (enablecaching != null && (enablecaching.equals("true") || enablecaching.equals(true))) {
						derived = new DeriveFeature().deriveFeature(cachedModules, indexer, n, this, s, prop);
					} else {
						derived = new DeriveFeature().deriveFeature(new HashMap<String, EolModule>(), indexer, n, this,
								s, prop);
					}
				} catch (Exception e1) {
					LOGGER.error("Error while deriving feature " + prop, e1);
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

				IGraphNode elementnode = n.getIncoming().iterator().next().getStartNode();

				IGraphNode typeNode = elementnode.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator()
						.next().getEndNode();
				IGraphNodeIndex idxNodeByDerivedValue = graph.getOrCreateNodeIndex(typeNode.getOutgoingWithType("epackage")
						.iterator().next().getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString() + "##"
						+ typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString() + "##" + s);

				// flatten multi-valued derived features for indexing
				if (derived != null) {
					if (derived.getClass().getComponentType() != null || derived instanceof Collection<?>) {
						derived = new Utils().toString(derived);
					}

					// TODO: need to test how this works with derived edges
					idxNodeByDerivedValue.add(elementnode, s, derived);
				}
			}
		}

		IGraphNodeIndex derivedProxyDictionary = graph.getOrCreateNodeIndex("derivedproxydictionary");
		derivedProxyDictionary.remove(n);
	}

	@Override
	public String getType() {
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

		if (context == null)
			return contextlessQuery(m, query, context);

		final Object repo = context.get(PROPERTY_REPOSITORYCONTEXT);
		final Object file = context.get(PROPERTY_FILECONTEXT);
		if (repo == null && file == null) // no scope
			return contextlessQuery(m, query, context);
		else
			// scoped
			return contextfulQuery(m, query, context);
	}

	@Override
	public Object query(IModelIndexer m, File query, Map<String, Object> context)
			throws InvalidQueryException, QueryExecutionException {

		String code = "";
		try {
			BufferedReader r = new BufferedReader(new FileReader(query));
			String line;
			while ((line = r.readLine()) != null)
				code = code + "\r\n" + line;
			r.close();
		} catch (Exception e) {
			LOGGER.error("Error reading the EOL file", e);
		}
		return query(m, code, context);

	}

	protected Object contextlessQuery(IModelIndexer m, String query, Map<String, Object> context)
			throws QueryExecutionException, InvalidQueryException {
		final long trueStart = System.currentTimeMillis();
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
		return runQuery(trueStart, module);
	}

	protected Object contextfulQuery(IModelIndexer m, String query, Map<String, Object> context)
			throws QueryExecutionException, InvalidQueryException {
		final long trueStart = System.currentTimeMillis();

		CEOLQueryEngine q = new CEOLQueryEngine();
		q.setContext(context);
		try {
			q.load(m);
		} catch (EolModelLoadingException e) {
			throw new QueryExecutionException("Loading of EOLQueryEngine failed");
		}
		LOGGER.debug("Graph path: {}", graph.getPath());

		final IEolModule module = createModule();
		parseQuery(query, context, q, module);
		return runQuery(trueStart, module);
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
			if (enableDebugOutput) {
				System.out.println("PARSING:\n----------\n" + name == null ? "QUERY" : name + "\n----------");
				System.out.println("Graph path: " + graph.getPath() + "\n----------");
			}

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
	}

	protected Object runQuery(final long trueStart, final IEolModule module) throws QueryExecutionException {
		final long init = System.currentTimeMillis();
		Object ret = null;
		try (IGraphTransaction tx = graph.beginTransaction()) {
			ret = module.execute();
			if (ret == null)
				ret = "no result returned (maybe it directly printed the result to console?)";
			tx.success();
		} catch (EolUndefinedVariableException ex) {
			// Provide more details than Epsilon about ambiguous intra-model type references
			try (IGraphTransaction tx = graph.beginTransaction()) {
				final List<IGraphNode> typeNodes = getTypeNodes(ex.getVariableName());
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

		if (enableDebugOutput) {
			System.out.println("QUERY TOOK " + (System.currentTimeMillis() - init) / 1000 + "s"
					+ (System.currentTimeMillis() - init) % 1000 + "ms, to run");

			System.out.println("total time taken " + (System.currentTimeMillis() - trueStart) / 1000 + "s"
					+ (System.currentTimeMillis() - trueStart) % 1000 + "ms, to run");
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

}
