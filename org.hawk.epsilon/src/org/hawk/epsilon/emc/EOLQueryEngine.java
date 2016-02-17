/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
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
import org.hawk.graph.FileNode;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;

public class EOLQueryEngine extends AbstractEpsilonModel implements IQueryEngine {

	public static final String TYPE = "org.hawk.epsilon.emc.EOLQueryEngine";

	protected HashSet<String> cachedTypes = new HashSet<String>();
	protected StringProperties config = null;

	// TODO try re-enable the use of a cache
	// protected boolean enableCache = true;

	protected static IModelIndexer indexer = null;
	protected static IGraphDatabase graph = null;

	// TODO memory management should these get too big
	protected Collection<Object> allContents = null;
	// if cache enabled
	protected HashMap<String, OptimisableCollection> typeContents = new HashMap<>();
	protected HashMap<String, OptimisableCollection> superTypeContents = new HashMap<>();

	protected IGraphNodeIndex metamodeldictionary;

	protected Set<String> defaultnamespaces = null;

	protected GraphPropertyGetter propertygetter;

	public static long time = 0L; // total time taken in specific methods for
									// debugging

	/**
	 * Returns all of the contents of the database in the form of lightweight
	 * GraphNodeWrapper objects
	 */
	@Override
	public Collection<?> allContents() {
		// if (!enableCache || (enableCache && allContents == null)) {
		allContents = new HashSet<Object>();
		// GlobalGraphOperations ops = GlobalGraphOperations.at(graph);

		for (IGraphNode node : graph.allNodes("eobject")) {
			GraphNodeWrapper wrapper = new GraphNodeWrapper(node.getId().toString(), this);
			allContents.add(wrapper);
		}

		// }
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
		OptimisableCollection nodes = new OptimisableCollection(this,
				new GraphNodeWrapper(typeNode.getId().toString(), this));

		for (IGraphEdge n : typeNode.getIncomingWithType(typeorkind)) {
			nodes.add(new GraphNodeWrapper(n.getStartNode().getId().toString(), this));
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
	public Object getElementById(String arg0) {
		try {

			Object id;

			try {
				id = Long.parseLong(arg0);
			} catch (NumberFormatException e) {
				id = arg0;
			}

			boolean isnull = false;

			try {
				// operations on the graph
				// ...

				isnull = graph.getNodeById(id) == null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (isnull)
				return null;
			else
				return new GraphNodeWrapper(id.toString(), this);
		} catch (NumberFormatException e) {
			System.err.println("NumberFormatException returning null");
			return null;
		}
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
			IGraphNode typeNode = graph.getNodeById(((GraphNodeWrapper) type).getId());

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

			IGraphNode objectNode = graph.getNodeById(((GraphNodeWrapper) arg0).getId());

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
			IGraphNode objectNode = graph.getNodeById(((GraphNodeWrapper) arg0).getId());
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
		if (!(arg0 instanceof GraphNodeWrapper))
			return false;
		else if (((GraphNodeWrapper) arg0).getId() != null)
			return true;
		else
			return false;
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

		if (m != null
		// && graph == null
		) {
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

		// String ec = (String)
		// config.get(EOLQueryEngine.PROPERTY_ENABLE_CASHING);
		// enableCache = Boolean.parseBoolean((ec == null) ? "true" : ec);
		// System.err.println("EOL EC: " + enableCache);

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

	@SuppressWarnings("unused")
	private void fullLog() throws IOException {

		int count = 1;

		File file = new File("D:/workspace/Neo4Jstore-v2/" + "eol-graph-log-" + count + ".txt");

		while (file.exists()) {
			count++;
			file = new File("D:/workspace/Neo4Jstore-v2/" + "eol-graph-log-" + count + ".txt");
		}

		FileWriter w = new FileWriter(file);
		String id = null;

		try {
			// operations on the graph
			// ...

			for (IGraphNode n : graph.allNodes(null)) {

				String refs = "\n--> ";

				for (IGraphEdge r : n.getOutgoing()) {

					id = "";

					try {
						id = r.getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
					} catch (Exception e) {
						id = "NO ID FOUND";
					}

					String x = "->";
					if (!r.getStartNode().equals(n))
						x = "<-";

					refs += "[" + r.getType() + " " + x + " "
							+ r.getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY) + "" + "]";

				}

				String props = "";

				for (String s : n.getPropertyKeys()) {

					props += "[" + s + " : " + n.getProperty(s) + "]";

				}

				w.append(n + " : " + props + refs + "\n");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		w.close();

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

	public boolean isOf(Object instance, String metaClass, final String typeorkind)
			throws EolModelElementTypeNotFoundException {

		if (instance == null)
			return false;

		if (!(instance instanceof GraphNodeWrapper))
			return false;

		String id = null;

		try {
			// operations on the graph
			// ...

			// System.err.println(instance);

			IGraphNode objectNode = graph.getNodeById(((GraphNodeWrapper) instance).getId());

			IGraphNode typeNode = null;

			// String[] splitId = null;
			try {
				typeNode = objectNode.getOutgoingWithType(typeorkind).iterator().next().getEndNode();
				id = typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
			} catch (Exception e) {
				// dont have a type - only if you are iterating all the
				// nodes even the metanodes
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
		// doesn't exist in database
		// tx.success();tx.close();

	}

	protected StringProperties getDefaultDatabaseConfig() {

		long x = Runtime.getRuntime().maxMemory() / 1000000 / 60;
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

	public void dumpDatabaseConfig() {

		for (Object c : config.keySet())
			System.out.println(">" + c + " = " + config.get(c));

	}

	@Override
	public Object query(IModelIndexer m, String query, Map<String, String> context)
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

		String repo = context.get(PROPERTY_REPOSITORYCONTEXT);
		String file = context.get(PROPERTY_FILECONTEXT);

		if (repo == null && file == null) // no scope
			return contextlessQuery(m, query, context);
		else
			// scoped
			return contextfulQuery(m, query, context);
	}

	@Override
	public Object query(IModelIndexer m, File query, Map<String, String> context)
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

	private Object contextlessQuery(IModelIndexer m, String query, Map<String, String> context)
			throws QueryExecutionException, InvalidQueryException {

		final long truestart = System.currentTimeMillis();

		String defaultnamespaces = null;
		if (context != null)
			defaultnamespaces = context.get(PROPERTY_DEFAULTNAMESPACES);

		/*
		 * Always create a new engine for every query (reusing the same engine
		 * would not be thread-safe).
		 */
		final EOLQueryEngine q = new EOLQueryEngine();
		try {
			q.load(m);
			q.setDefaultNamespaces(defaultnamespaces);
		} catch (EolModelLoadingException e) {
			throw new QueryExecutionException("Loading of EOLQueryEngine failed");
		}

		final EolModule module = new EolModule();

		Object ret = null;

		try {
			module.parse(query);
		} catch (Exception ex) {
			throw new InvalidQueryException(ex);
		}

		if (enableDebugOutput) {
			System.out.println("PARSING:\n----------\n" + name == null ? "QUERY" : name + "\n----------");
			System.out.println("Graph path: " + graph.getPath() + "\n----------");
		}

		module.getContext().getModelRepository().addModel(q);

		long init = System.currentTimeMillis();

		try (IGraphTransaction tx = graph.beginTransaction()) {
			ret = module.execute();
			if (ret == null)
				ret = "no result returned (maybe it directly printed the result to console?)";
			tx.success();
		} catch (Exception e) {
			throw new QueryExecutionException(e);
		}

		if (enableDebugOutput) {
			System.err.println("time variable = " + EOLQueryEngine.time + "ms");

			System.out.println("QUERY TOOK " + (System.currentTimeMillis() - init) / 1000 + "s"
					+ (System.currentTimeMillis() - init) % 1000 + "ms, to run");

			System.out.println("total time taken " + (System.currentTimeMillis() - truestart) / 1000 + "s"
					+ (System.currentTimeMillis() - truestart) % 1000 + "ms, to run");
		}

		return ret;

	}

	private Object contextfulQuery(IModelIndexer m, String query, Map<String, String> context)
			throws QueryExecutionException, InvalidQueryException {

		Object ret = null;

		final long truestart = System.currentTimeMillis();

		EolModule module = new EolModule();

		try {
			module.parse(query);
		} catch (Exception ex) {
			throw new InvalidQueryException(ex);
		}

		if (enableDebugOutput)
			System.out.println("PARSING:\n----------\n" + name == null ? "QUERY" : name + "\n----------");

		CEOLQueryEngine q = new CEOLQueryEngine();

		// defaults to true
		// String ec =
		// context.get(EOLQueryEngine.PROPERTY_ENABLE_CASHING);
		// enableCache = ec == null ? true :
		// ec.equalsIgnoreCase("true");
		try {
			q.load(m);
		} catch (EolModelLoadingException e) {
			throw new QueryExecutionException("Loading of EOLQueryEngine failed");
		}
		q.setContext(context);

		if (enableDebugOutput)
			System.out.println("Graph path: " + graph.getPath() + "\n----------");

		module.getContext().getModelRepository().addModel(q);

		long init = System.currentTimeMillis();

		try (IGraphTransaction tx = graph.beginTransaction()) {
			ret = module.execute();
			if (ret == null)
				ret = "no result returned (maybe it directly printed the result to console?)";
			tx.success();
		} catch (Exception ex) {
			throw new QueryExecutionException(ex);
		}

		if (enableDebugOutput) {
			System.err.println("time variable = " + EOLQueryEngine.time + "ms");

			System.out.println("QUERY TOOK " + (System.currentTimeMillis() - init) / 1000 + "s"
					+ (System.currentTimeMillis() - init) % 1000 + "ms, to run");

			System.out.println("total time taken " + (System.currentTimeMillis() - truestart) / 1000 + "s"
					+ (System.currentTimeMillis() - truestart) % 1000 + "ms, to run");
		}
		return ret;

	}

	// deriving attributes
	@Override
	public AccessListener calculateDerivedAttributes(IModelIndexer m, Iterable<IGraphNode> nodes) {

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
		GraphPropertyGetter pg = (GraphPropertyGetter) getPropertyGetter();
		pg.setBroadcastAccess(true);

		//
		// init eol stuff
		//

		HashMap<String, EolModule> cachedModules = new HashMap<String, EolModule>();

		try (IGraphTransaction t = graph.beginTransaction()) {

			for (IGraphNode n : nodes) {

				// System.err.println(n.getId());
				// ...
				for (String s : n.getPropertyKeys()) {

					String prop = n.getProperty(s).toString();

					if (prop.startsWith("_NYD##")) {

						Object derived = "DERIVATION_EXCEPTION";
						try {

							// System.out.println(getDatabaseConfig().keySet());

							Object enablecaching = getDatabaseConfig().get(EOLQueryEngine.PROPERTY_ENABLE_CACHING);

							// System.out.println(enablecaching);

							if (enablecaching != null && (enablecaching.equals("true") || enablecaching.equals(true))) {

								derived = new DeriveFeature().deriveFeature(cachedModules, indexer, n, this, s, prop);

							} else

								derived = new DeriveFeature().deriveFeature(new HashMap<String, EolModule>(), indexer,
										n, this, s, prop);

						} catch (Exception e1) {
							System.err.println("Exception in deriving attribute");
							e1.printStackTrace();
						}

						// System.err.println(derived.getClass());
						// System.err.println(derived);

						n.setProperty(s, derived);

						IGraphNode elementnode = n.getIncoming().iterator().next().getStartNode();

						IGraphNodeIndex derivedFeature = graph.getOrCreateNodeIndex(

								elementnode.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator().next()
										.getEndNode().getOutgoingWithType("epackage").iterator().next().getEndNode()
										.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString()
										//
										// e.getEPackage().getNsURI()
										+ "##"
										// -
										+ elementnode.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator()
												.next().getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
												.toString()
												//
										+ "##" + s);

						// System.err.println(">< " + derived);

						// XXXdeprecated derived attribute to metamodel
						// resolution -
						// only needed if no info in mm

						// n.getRelationships(
						// Direction.OUTGOING,
						// RelationshipUtil
						// .getNewRelationshipType(ModelElementNode.EDGE_LABEL_OFTYPE))
						// .iterator()
						// .next()
						// .getEndNode()
						// .setProperty(
						// s,
						// "a."
						// + ((derived instanceof Collection<?>) ? "t"
						// : "f") + ".u.u");

						derivedFeature.add(elementnode, s, derived);
					}
				}

				IGraphNodeIndex derivedProxyDictionary = graph.getOrCreateNodeIndex("derivedproxydictionary");
				derivedProxyDictionary.remove(n);

			}

			// allUnresolved = derivedProxyDictionary.query("derived", "*");
			//
			// for (Node n : allUnresolved)
			// System.err.println(n.getId()
			// + " :: "
			// + n.getRelationships(Direction.OUTGOING,
			// RelationshipUtil
			// .getNewRelationshipType("ofType")).iterator().next().getEndNode()
			// + " :: " + n.getPropertyKeys());
			t.success();
		} catch (Exception e) {
			e.printStackTrace();
		}

		pg.setBroadcastAccess(false);

		return pg.getAccessListener();
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public List<String> validate(String derivationlogic) {

		// System.err.println("validating:");
		// System.err.println(derivationlogic);

		EolModule module = new EolModule();

		List<String> ret = new LinkedList<>();

		try {
			module.parse(derivationlogic);
			for (ParseProblem p : module.getParseProblems())
				ret.add(p.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.err.println(ret);

		return ret;
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

	public void setDefaultNamespaces(String namespaces) {

		// set default packages if applicable
		try {
			if (namespaces != null && !namespaces.equals("")) {
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
