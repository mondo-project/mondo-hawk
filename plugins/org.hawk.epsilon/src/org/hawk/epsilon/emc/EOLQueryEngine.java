/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.epsilon.eol.IEolExecutableModule;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.eclipse.epsilon.eol.execute.introspection.IPropertySetter;
import org.eclipse.epsilon.eol.models.Model;
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

	public static final String TYPE = "org.hawk.epsilon.emc.EOLQueryEngine";

	protected final Set<String> cachedTypes = new HashSet<String>();
	protected final Map<String, OptimisableCollection> typeContents = new HashMap<>();
	protected final Map<String, OptimisableCollection> superTypeContents = new HashMap<>();

	/* TODO: these two should not have to be static.*/
	protected static IModelIndexer indexer = null;
	protected static IGraphDatabase graph = null;

	protected IGraphNodeIndex metamodeldictionary;

	protected StringProperties config = null;
	protected Set<String> defaultnamespaces = null;

	protected GraphPropertyGetter propertygetter;

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

	public Collection<Object> getAllOf(String typeName, final String typeorkind)
			throws EolModelElementTypeNotFoundException {

		try {
			IGraphNode typeNode = null;

			if (typeName.contains("::")) {
				String ep = typeName.substring(0, typeName.indexOf("::"));

				IGraphNode pack = null;

				try {
					// operations on the graph
					// ...

					pack = metamodeldictionary.get("id", ep).getSingle();

					for (IGraphEdge r : pack.getIncomingWithType("epackage")) {

						IGraphNode othernode = r.getStartNode();
						if (othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
								.equals(typeName.substring(typeName.indexOf("::") + 2))) {
							typeNode = othernode;
							break;
						}

					}
				} catch (Exception e) {
					throw new EolModelElementTypeNotFoundException(this.getName(), typeName);
				}

			} else {

				Iterator<IGraphNode> packs = metamodeldictionary.query("id", "*").iterator();
				LinkedList<IGraphNode> possibletypenodes = new LinkedList<IGraphNode>();

				while (packs.hasNext()) {

					IGraphNode pack = packs.next();
					for (IGraphEdge n : pack.getIncomingWithType("epackage")) {

						IGraphNode othernode = n.getStartNode();
						if (othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(typeName)) {

							possibletypenodes.add(othernode);

						}
					}
				}

				if (possibletypenodes.size() == 1)
					typeNode = possibletypenodes.getFirst();
				else if (possibletypenodes.size() > 1) {
					// use default namespaces to limit types
					LinkedList<String> ret = new LinkedList<>();
					for (Iterator<IGraphNode> it = possibletypenodes.iterator(); it.hasNext();) {
						IGraphNode n = it.next();
						String metamodel = n.getOutgoingWithType("epackage").iterator().next().getEndNode()
								.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
						if (defaultnamespaces != null && !defaultnamespaces.contains(metamodel)) {
							it.remove();
						} else
							ret.add(metamodel + "::" + n.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString());
					}

					if (possibletypenodes.size() == 1) {
						typeNode = possibletypenodes.getFirst();
					} else {
						System.err.println("types found:" + ret);
						throw new EolModelElementTypeNotFoundException(this.getName(),
								possibletypenodes.size() + " CLASSES FOUND FOR: " + typeName + "\ntypes found:" + ret);
					}
				}
			}

			if (typeNode != null) {
				return getAllOf(typeNode, typeorkind);
			}

			throw new EolModelElementTypeNotFoundException(this.getName(), typeName);
		} catch (Exception e) {
			throw new EolModelElementTypeNotFoundException(this.getName(), typeName);
		}
	}

	public Collection<Object> getAllOf(IGraphNode typeNode, final String typeorkind) {
		OptimisableCollection nodes = new OptimisableCollection(this, new GraphNodeWrapper(typeNode, this));

		for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {
			nodes.add(new GraphNodeWrapper(n.getStartNode(), this));
		}
		broadcastAllOfXAccess(nodes);
		return nodes;
	}

	private void broadcastAllOfXAccess(Iterable<Object> ret) {

		// TODO can optimise by not keeping all the nodes of type/kind but the
		// concept of type/kind and only update the attr if a new node is added
		// or one is removed

		if (((GraphPropertyGetter) propertygetter).getBroadcastStatus()) {

			for (Object n : ret)
				((GraphPropertyGetter) propertygetter).getAccessListener().accessed(((GraphNodeWrapper) n).getId() + "",
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
			// operations on the graph
			// ...

			Object type = getTypeOf(arg0);
			IGraphNode typeNode = ((GraphNodeWrapper) type).getNode();

			ret = typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	@Override
	public TypeNodeWrapper getTypeOf(Object arg0) {
		IGraphNode typeNode = null;

		try {

			IGraphNode objectNode = ((GraphNodeWrapper) arg0).getNode();

			// operations on the graph
			// ...

			// returns the typeOf relationship of arg0 as a node id wrapper
			// do we want the e-class instead?

			typeNode = objectNode.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator().next()
					.getEndNode();
		} catch (Exception e) {
			// System.err
			// .println("error in getTypeOf, returning new
			// NeoIdWrapper(graph,0L, this);");
			System.err.println("error in getTypeOf, returning null");
			// return new NeoIdWrapper(graph,0L, this);
		}

		if (typeNode != null)
			return new TypeNodeWrapper(new TypeNode(typeNode), this);
		else
			return null;
	}

	@Override
	public FileNodeWrapper getFileOf(Object arg0) {
		IGraphNode fileNode = null;

		try {
			IGraphNode objectNode = ((GraphNodeWrapper) arg0).getNode();
			fileNode = objectNode.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE).iterator().next().getEndNode();
		} catch (Exception e) {
			System.err.println("error in getFileOf, returning null");
		}

		if (fileNode != null)
			return new FileNodeWrapper(new FileNode(fileNode), this);
		else
			return null;
	}

	@Override
	public List<FileNodeWrapper> getFilesOf(Object arg0) {
		final List<FileNodeWrapper> files = new ArrayList<>();

		try {
			IGraphNode objectNode = ((GraphNodeWrapper) arg0).getNode();
			for (IGraphEdge outFileEdge : objectNode.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				final FileNode fileNode = new FileNode(outFileEdge.getEndNode());
				final FileNodeWrapper fileNodeWrapper = new FileNodeWrapper(fileNode, this);
				files.add(fileNodeWrapper);
			}
		} catch (Exception e) {
			System.err.println("error in getFilesOf, returning null: " + e.getMessage());
		}

		return files;
	}

	@Override
	public boolean hasType(String type) {
		// If conflict return false
		// doneTODO Can receive both simple and fully-qualified name e.g.
		// x::y::A -
		// currently x::y/A

		try {

			boolean found = false;

			if (type.equals("UNSET"))
				return false;

			if (cachedTypes.contains(type)) {
				return true;
			} else {

				if (type.contains("::")) {

					final String[] parts = type.split("::", 2);
					final String ep = parts[0];
					final String ec = parts[1];

					IGraphNode pack = null;

					try {
						// operations on the graph
						// ...

						pack = metamodeldictionary.get("id", ep).getSingle();

						for (IGraphEdge r : pack.getIncomingWithType("epackage")) {
							final IGraphNode otherNode = r.getStartNode();
							final Object otherEClass = otherNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
							if (otherEClass.equals(ec)) {
								found = true;
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				} else {

					LinkedList<IGraphNode> possibletypenodes = new LinkedList<IGraphNode>();

					// operations on the graph
					// ...

					Iterator<IGraphNode> packs = metamodeldictionary.query("id", "*").iterator();

					while (packs.hasNext()) {

						IGraphNode pack = packs.next();
						for (IGraphEdge n : pack.getIncomingWithType("epackage")) {

							IGraphNode othernode = n.getStartNode();
							final Object id = othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
							if (id.equals(type)) {
								possibletypenodes.add(othernode);
							}
						}
					}
					if (possibletypenodes.size() == 0)
						return false;
					else if (possibletypenodes.size() > 1) {
						// use default namespaces to limit types
						LinkedList<String> ret = new LinkedList<>();
						for (Iterator<IGraphNode> it = possibletypenodes.iterator(); it.hasNext();) {
							IGraphNode n = it.next();
							String metamodel = n.getOutgoingWithType("epackage").iterator().next().getEndNode()
									.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
							if (defaultnamespaces != null && !defaultnamespaces.contains(metamodel)) {
								it.remove();
							} else
								ret.add(metamodel + "::" + n.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString());
						}

						if (possibletypenodes.size() == 1) {
							cachedTypes.add(type);
							return true;
						} else {
							System.err.println("ERROR IN hasType(String arg0). " + possibletypenodes.size()
									+ " CLASSES FOUND FOR " + type + ", RETURNING FALSE");
							System.err.println("types found:" + ret);
							return false;
						}
					} else {
						cachedTypes.add(type);
						return true;
					}

				}
			}
			return found;

		} catch (Exception e) {
			System.err.println("Warning hasType failed, returning false: ");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean isModelElement(Object arg0) {
		return (arg0 instanceof GraphNodeWrapper);
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

		if (propertygetter == null || propertygetter.getGraph() != graph)
			propertygetter = new GraphPropertyGetter(graph, this);

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
				e.printStackTrace();
			}

		} else
			throw new EolModelLoadingException(new Exception("Attempt to load a model from an invalid graph: " + graph),
					this);

		if (enableDebugOutput)
			System.err.println("engine initialised with model named: " + name + ", with aliases: " + aliases);

	}

	@Override
	public boolean owns(Object arg0) {
		if (arg0 instanceof GraphNodeWrapper) {
			final GraphNodeWrapper gnw = (GraphNodeWrapper) arg0;
			if (gnw.getContainerModel() == null || gnw.getContainerModel() == this) {
				return true;
			} else {
				System.err.println("warning owns failed on : " + arg0 + "\nwith getContainerModel() : "
						+ gnw.getContainerModel() + "\nand 'this' : " + this);
			}
		}

		return false;
	}

	@Override
	public boolean isOfKind(Object instance, String metaClass) throws EolModelElementTypeNotFoundException {
		if (!(instance instanceof GraphNodeWrapper)) {
			return false;
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

		if (instance == null)
			return false;

		if (!(instance instanceof GraphNodeWrapper))
			return false;

		String id = null;

		try {
			try {
				id = ((GraphNodeWrapper) instance).getTypeName();
			} catch (Exception e) {
				// dont have a type - only if you are iterating all the
				// nodes even the metanodes
				if (enableDebugOutput)
					System.err.println("warning: metaclass node asked for its type, ignored");
				// e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
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

		// System.out.println("---");
		// System.out.println(instance.getClass());
		// System.out.println(property);
		// System.out.println(owns(instance));
		// System.out.println("---");

		if (!owns(instance))
			return false;

		if (instance instanceof GraphNodeWrapper)
			return true;

		return false;

	}

	@Override
	public IPropertyGetter getPropertyGetter() {

		if (propertygetter == null)
			System.err.println("null property getter, was load() called?");

		return propertygetter;

	}

	@Override
	public IPropertySetter getPropertySetter() {
		return null;// new NeoPropertySetter(graph, this);
	}

	public IGraphDatabase getBackend() {
		return graph;
	}

	protected void dumpDatabaseConfig() {
		for (Object c : config.keySet())
			System.out.println(">" + c + " = " + config.get(c));
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

	// deriving attributes
	@Override
	public AccessListener calculateDerivedAttributes(IModelIndexer m, Iterable<IGraphNode> nodes) {
		final boolean enableDebug = false;

		indexer = m;
		graph = m.getGraph();

		if (config == null)
			config = getDatabaseConfig();

		if (propertygetter == null)
			propertygetter = new GraphPropertyGetter(graph, this);

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
			e.printStackTrace();
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

			if (prop.startsWith("_NYD##")) {
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
					System.err.println("Exception in deriving attribute");
					e1.printStackTrace();
				}

				n.setProperty(s, derived);

				IGraphNode elementnode = n.getIncoming().iterator().next().getStartNode();

				IGraphNode typeNode = elementnode.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator()
						.next().getEndNode();
				IGraphNodeIndex derivedFeature = graph.getOrCreateNodeIndex(typeNode.getOutgoingWithType("epackage")
						.iterator().next().getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString() + "##"
						+ typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString() + "##" + s);

				// flatten multi-valued derived features for indexing
				if (derived.getClass().getComponentType() != null || derived instanceof Collection<?>)
					derived = new Utils().toString(derived);

				derivedFeature.add(elementnode, s, derived);
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
		final IEolExecutableModule module = createModule();

		final List<String> ret = new LinkedList<>();
		try {
			module.parse(derivationlogic);
			for (ParseProblem p : module.getParseProblems())
				ret.add(p.toString());
		} catch (Exception e) {
			e.printStackTrace();
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
			System.err.println("error reading eol code file:");
			e.printStackTrace();
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

		final IEolExecutableModule module = createModule();
		parseQuery(query, context, q, module);
		return runQuery(trueStart, module);
	}

	protected Object contextfulQuery(IModelIndexer m, String query, Map<String, Object> context)
			throws QueryExecutionException, InvalidQueryException {
		final long trueStart = System.currentTimeMillis();

		CEOLQueryEngine q = new CEOLQueryEngine();
		try {
			q.load(m);
		} catch (EolModelLoadingException e) {
			throw new QueryExecutionException("Loading of EOLQueryEngine failed");
		}
		q.setContext(context);
		if (enableDebugOutput)
			System.out.println("Graph path: " + graph.getPath() + "\n----------");

		final IEolExecutableModule module = createModule();
		parseQuery(query, context, q, module);
		return runQuery(trueStart, module);
	}

	// IQueryEngine part //////////////////////////////////////////////////////
	
	/**
	 * Query engines that add support for other Epsilon languages should redefine this method.
	 */
	protected IEolExecutableModule createModule() {
		return new EolModule();
	}

	protected void parseQuery(String query, Map<String, Object> context, final EOLQueryEngine model,
			final IEolExecutableModule module) throws InvalidQueryException {
		try {
			if (enableDebugOutput) {
				System.out.println("PARSING:\n----------\n" + name == null ? "QUERY" : name + "\n----------");
				System.out.println("Graph path: " + graph.getPath() + "\n----------");
			}
			module.parse(query);
		} catch (Exception ex) {
			throw new InvalidQueryException(ex);
		}
		module.getContext().getModelRepository().addModel(model);
		addQueryArguments(context, module);
	}

	protected Object runQuery(final long trueStart, final IEolExecutableModule module) throws QueryExecutionException {
		final long init = System.currentTimeMillis();
		Object ret = null;
		try (IGraphTransaction tx = graph.beginTransaction()) {
			ret = module.execute();
			if (ret == null)
				ret = "no result returned (maybe it directly printed the result to console?)";
			tx.success();
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
	protected void addQueryArguments(Map<String, Object> context, final IEolExecutableModule module) {
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
			if (namespaces != null && !namespaces.trim().equals("")) {
				String[] eps = ((String) namespaces).split(",");
				defaultnamespaces = new HashSet<String>();
				for (String s : eps) {
					// System.err.println(s);
					defaultnamespaces.add(s.trim());
				}
			}

		} catch (Throwable t) {
			System.err.println("setting of default namespaces failed, malformed property: " + namespaces);
			t.printStackTrace();
		}
	}

}
