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
import org.hawk.core.IAbstractConsole;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.IQueryEngine;

public class EOLQueryEngine extends AbstractEpsilonModel implements
		IQueryEngine
// implements IOperationContributorProvider
{

	public static final String TYPE = "org.hawk.epsilon.emc.EOLQueryEngine";

	protected HashSet<String> cachedTypes = new HashSet<String>();
	protected String backendURI = null;
	protected StringProperties config = null;
	// TODO try re-enable the use of a cache
	// protected boolean enableCache = true;

	protected static IGraphDatabase graph = null;

	// TODO memory management should these get too big
	protected Collection<Object> allContents = null;
	protected HashMap<String, OptimisableCollection> typeContents = new HashMap<>();
	protected HashMap<String, OptimisableCollection> superTypeContents = new HashMap<>();

	protected IGraphNodeIndex epackagedictionary;
	protected Set<String> epackages = null;
	protected static IAbstractConsole console;
	// protected OptimisableCollectionOperationContributor operationContributor
	// = new OptimisableCollectionOperationContributor();

	// public ModelIndexer hawkContainer;
	protected IPropertyGetter propertygetter;

	public static long time = 0L; // total time taken in specific methods for
									// debugging

	public EOLQueryEngine() {
	}

	/**
	 * deprecated - file choice and output choice is on ui side
	 * 
	 * @param g
	 * @param c
	 * @throws Exception
	 */
	// public void run(IGraphDatabase g, IAbstractConsole c) throws Exception {
	//
	// // this.parser = parser;
	// console = c;
	// graph = g;
	// boolean exit = false;
	//
	// if (propertygetter == null)
	// propertygetter = new GraphPropertyGetter(graph, this);
	//
	// JFrame fileChoserWindow = null;
	// File selectedEOL = null;
	//
	// fileChoserWindow = new JFrame();
	//
	// JFileChooser filechoser = new JFileChooser();
	// filechoser.setDialogTitle("Chose EOL File to run:");
	// File genericWorkspaceFile = new File("");
	// String parent = genericWorkspaceFile.getAbsolutePath().replaceAll(
	// "\\\\", "/");
	//
	// // change to workspace directory or a generic one on release
	// filechoser.setCurrentDirectory(new File(new File(parent)
	// .getParentFile().getAbsolutePath()
	// + "workspace/org.hawk.neo4j/src/org/hawk/neo4j/emc"));
	//
	// // filechoser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	//
	// if (filechoser.showDialog(fileChoserWindow, "Select File") ==
	// JFileChooser.APPROVE_OPTION)
	// selectedEOL = filechoser.getSelectedFile();
	// else {
	// System.err.println("Chosing of EOL file canceled");
	// exit = true;
	// }
	//
	// fileChoserWindow.dispose();
	//
	// if (!exit) {
	//
	// EolModule module = new EolModule();
	//
	// module.parse(selectedEOL);
	//
	// System.out.println("PARSING:\n----------\n" + selectedEOL
	// + "\n----------");
	//
	// // Neo4JEpsilonModel model = new Neo4JEpsilonModel();
	//
	// System.out.println("Graph path: " + g.getPath() + "\n----------");
	//
	// //
	// StringProperties configuration = new StringProperties();
	//
	// long x = Runtime.getRuntime().maxMemory() / 1000000 / 60;
	// // configuration.put("DUMP_DATABASE_CONFIG_ON_EXIT", true);
	// // configuration.put("DUMP_MODEL_CONFIG_ON_EXIT", true);
	// // configuration.put("DUMP_FULL_DATABASE_CONFIG_ON_EXIT", true);
	//
	// configuration.put("DATABASE_LOCATION", g.getPath());
	// configuration.put("name", "Model");
	// configuration.put("ENABLE_CASHING", true);
	//
	// // HashSet<String> ep = new HashSet<String>();
	// // ep.add("org.amma.dsl.jdt.core");
	// // ep.add("org.amma.dsl.jdt.primitiveTypes");
	// // ep.add("org.amma.dsl.jdt.dom");
	// //
	// // configuration.put("EPACKAGES", ep);
	//
	// configuration.put("neostore.nodestore.db.mapped_memory", 5 * x
	// + "M");
	// configuration.put("neostore.relationshipstore.db.mapped_memory", 15
	// * x + "M");
	// configuration.put("neostore.propertystore.db.mapped_memory", 20 * x
	// + "M");
	// configuration.put(
	// "neostore.propertystore.db.strings.mapped_memory", 2 * x
	// + "M");
	// configuration.put("neostore.propertystore.db.arrays.mapped_memory",
	// x + "M");
	// //
	//
	// setDatabaseConfig(configuration);
	//
	// load();
	//
	// module.getContext().getModelRepository().addModel(this);
	//
	// long init = System.nanoTime();
	//
	// module.execute();
	//
	// System.out.println("PROGRAM TOOK ~" + (System.nanoTime() - init)
	// / 1000000000 + "s to run");
	//
	// init = System.nanoTime();
	//
	// this.dispose();
	//
	// System.out.println("DISPOSAL TOOK ~" + (System.nanoTime() - init)
	// / 1000000000 + "s to run");
	//
	// //
	// System.err.println("Time (s) spent in tracked method: "+time/1000000000+"."+time%1000000000
	// // +
	// //
	// " (number of times visited) "+count+"\nsecond time tracker: "+time2/1000000000+"."+time2%1000000000);
	// }
	// }

	/**
	 * Returns all of the contents of the database in the form of lightweight
	 * NeoIdWrapper objects
	 */
	@Override
	public Collection<?> allContents() {
		// TODOdone limit to packages / files of interest -- implemented in
		// CEOLQueryEngine
		// if (!enableCache || (enableCache && allContents == null)) {
		allContents = new HashSet<Object>();
		// GlobalGraphOperations ops = GlobalGraphOperations.at(graph);

		try (IGraphTransaction t = graph.beginTransaction()) {

			for (IGraphNode node : graph.allNodes("eobject")) {
				GraphNodeWrapper wrapper = new GraphNodeWrapper(node.getId()
						.toString(), this);
				allContents.add(wrapper);
			}
			t.success();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// }
		broadcastAllOfXAccess(allContents);
		return allContents;
	}

	/**
	 * Creates a node and inserts it into the database
	 */
	@Override
	public Object createInstance(String metaClassName)
			throws EolModelElementTypeNotFoundException,
			EolNotInstantiableModelElementTypeException {
		System.err
				.println("createInstance called on a Neo4JEpsilonModel, this is not supported as Hawk is a read-only index, returning null");
		return null;

		// GraphNode node;
		//
		// if (!hasType(metaClassName))
		// throw new EolModelElementTypeNotFoundException(this.getName(),
		// metaClassName);
		// else if (!isInstantiable(metaClassName))
		// throw new EolNotInstantiableModelElementTypeException(
		// this.getName(), metaClassName);
		// else {
		//
		// // find metaclass node
		// GraphNode cl = new MetamodelUtils().getClassNode(graph,
		// metaClassName);
		//
		// try (GraphTransaction tx = graph.beginTransaction()) {
		// // operations on the graph
		// // ...
		//
		// node = graph.createNode();
		//
		// node.setProperty("id", "generated");
		//
		// node.createRelationshipTo(cl, new RelationshipType() {
		// @Override
		// public String name() {
		// return "typeOf";
		// }
		// });
		// tx.success();
		// tx.close();
		// }
		//
		// }
		// return new GraphNodeWrapper(graph, node.getId(), this);
	}

	/**
	 * Deletes the element from the database
	 */
	@Override
	public void deleteElement(Object arg0) throws EolRuntimeException {
		// TODOdepracated is this supposed to delete references too ? how ? !
		System.err.println("delete element called!");
		throw new EolRuntimeException(
				"warning: this type of model cannot delete elements");
		// try (Transaction tx = graph.beginTx()) {
		// // operations on the graph
		// // ...
		//
		// Node node = graph.getNodeById(((GraphNodeWrapper) arg0)
		// .getId());
		//
		// for (Relationship rel : node.getRelationships()) {
		//
		// Node other = null;
		// if (rel.getStartNode().equals(node)
		// && ((HawkReference) new MetamodelUtils()
		// .getClassFromNode(node,
		// hawkContainer.getModelParser())
		// .getEStructuralFeature(rel.getType().name()))
		// .isContainment()) {
		// other = rel.getOtherNode(node);// handles containment use
		// // ref name vs eclass maybe
		// rel.delete();
		// other.delete();
		// }
		//
		// }
		//
		// node.delete();
		//
		// tx.success();tx.close();
		//
		// } catch (Exception e) {
		// throw new EolRuntimeException("Failed to delete: " + arg0);
		// }

	}

	public Collection<Object> getAllOf(String arg0, final String typeorkind)
			throws EolModelElementTypeNotFoundException {

		try {

			if (hasType(arg0)) {

				// cashing
				// if (enableCache) {
				//
				// OptimisableCollection ret = typeorkind.equals("typeOf") ?
				// typeContents
				// .get(arg0) : superTypeContents.get(arg0);
				//
				// if (ret != null) {
				// // System.err.println("using cashed collection of all: "
				// // +
				// // typeorkind + " : " + arg0);
				// broadcastAllOfXAccess(ret);
				// return ret;
				// }
				//
				// }

				IGraphNode typeNode = null;

				if (arg0.contains("::")) {

					String ep = arg0.substring(0, arg0.indexOf("::"));

					IGraphNode pack = null;

					try (IGraphTransaction tx = graph.beginTransaction()) {
						// operations on the graph
						// ...

						pack = epackagedictionary.get("id", ep).getSingle();

						tx.success();
						tx.close();
					}

					for (IGraphEdge r : pack.getIncomingWithType("epackage")) {

						IGraphNode othernode = r.getStartNode();
						if (othernode.getProperty("id").equals(
								arg0.substring(arg0.indexOf("::") + 2))) {
							typeNode = othernode;
							break;
						}

					}

				} else {

					if (epackages == null) {

						try (IGraphTransaction tx = graph.beginTransaction()) {
							// operations on the graph
							// ...

							Iterator<IGraphNode> packs = epackagedictionary
									.query("id", "*").iterator();
							LinkedList<IGraphNode> possibletypenodes = new LinkedList<IGraphNode>();

							while (packs.hasNext()) {

								IGraphNode pack = packs.next();
								for (IGraphEdge n : pack
										.getIncomingWithType("epackage")) {

									IGraphNode othernode = n.getStartNode();
									if (othernode.getProperty("id")
											.equals(arg0)) {

										possibletypenodes.add(othernode);

									}
								}
							}

							if (possibletypenodes.size() == 1)
								typeNode = possibletypenodes.getFirst();
							else
								throw new EolModelElementTypeNotFoundException(
										this.getName(),
										possibletypenodes.size()
												+ " CLASSES FOUND FOR: " + arg0);

							tx.success();
							tx.close();
						}

					} else {

						for (String p : epackages) {

							try (IGraphTransaction tx = graph
									.beginTransaction()) {
								// operations on the graph
								// ...

								IGraphNode pack = epackagedictionary.get("id",
										p).getSingle();
								for (IGraphEdge n : pack
										.getIncomingWithType("epackage")) {

									IGraphNode othernode = n.getStartNode();
									if (othernode.getProperty("id")
											.equals(arg0)) {

										typeNode = othernode;
										break;

									}
								}

								tx.success();
								tx.close();
							}
						}

					}
				}

				// HashSet<Object> nodes = new HashSet<>();

				OptimisableCollection nodes = new OptimisableCollection(this,
						new GraphNodeWrapper(typeNode.getId().toString(), this));

				if (typeNode != null) {

					try (IGraphTransaction tx = graph.beginTransaction()) {
						// operations on the graph
						// ...

						for (IGraphEdge n : typeNode
								.getIncomingWithType(typeorkind)) {

							nodes.add(new GraphNodeWrapper(n.getStartNode()
									.getId().toString(), this));
						}
						tx.success();
						tx.close();
					}
				}

				// System.out.println(nodes);

				// for(NeoIdWrapper n:nodes){System.out.println(new
				// IsOf().isOfClass(graph.getNodeById(n.getId())));}

				// if (enableCache) {
				//
				// if (typeorkind.equals("typeOf")
				// && !typeContents.containsKey(arg0))
				// typeContents.put(arg0, nodes);
				// else if (typeorkind.equals("kind")
				// && !superTypeContents.containsKey(arg0))
				// superTypeContents.put(arg0, nodes);
				//
				// }

				broadcastAllOfXAccess(nodes);
				return nodes;

			} else
				throw new EolModelElementTypeNotFoundException(this.getName(),
						arg0);

		} catch (Exception e) {
			throw new EolModelElementTypeNotFoundException(this.getName(), arg0);
		}
	}

	private void broadcastAllOfXAccess(Iterable<Object> ret) {

		// TODO can optimise by not keeping all the nodes of type/kind but the
		// concept of type/kind and only update the attr if a new node is added
		// or one is removed

		if (((GraphPropertyGetter) propertygetter).getBroadcastStatus()) {

			for (Object n : ret)
				((GraphPropertyGetter) propertygetter).getAccessListener()
						.accessed(((GraphNodeWrapper) n).getId() + "",
								"property_unused_type_or_kind");

		}

	}

	@Override
	public Object getElementById(String arg0) {
		try {
			Long id = Long.parseLong(arg0);

			boolean isnull = false;

			try (IGraphTransaction tx = graph.beginTransaction()) {
				// operations on the graph
				// ...

				isnull = graph.getNodeById(id) == null;

				tx.success();
				tx.close();
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

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			Object type = getTypeOf(arg0);
			IGraphNode typeNode = graph.getNodeById(((GraphNodeWrapper) type)
					.getId());

			ret = typeNode.getProperty("id").toString();

			tx.success();
			tx.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	@Override
	public Object getTypeOf(Object arg0) {

		IGraphNode typeNode = null;

		try (IGraphTransaction tx = graph.beginTransaction()) {

			IGraphNode objectNode = graph.getNodeById(((GraphNodeWrapper) arg0)
					.getId());

			// operations on the graph
			// ...

			// returns the typeOf relationship of arg0 as a node id wrapper
			// do we want the e-class instead?

			typeNode = objectNode.getOutgoingWithType("typeOf").iterator()
					.next().getEndNode();

			tx.success();
			tx.close();

		} catch (Exception e) {
			// System.err
			// .println("error in getTypeOf, returning new NeoIdWrapper(graph,0L, this);");
			System.err.println("error in getTypeOf, returning null");
			// return new NeoIdWrapper(graph,0L, this);
		}

		if (typeNode != null)
			return new GraphNodeWrapper(typeNode.getId().toString(), this);
		else
			return null;
	}

	@Override
	public boolean hasType(String arg0) {
		// If conflict return false
		// doneTODO Can receive both simple and fully-qualified name e.g.
		// x::y::A -
		// currently x::y/A

		try {

			boolean found = false;

			if (arg0.equals("UNSET"))
				return false;

			if (cachedTypes.contains(arg0)) {
				return true;
			} else {

				if (arg0.contains("::")) {

					String ep = arg0.substring(0, arg0.indexOf("::"));

					IGraphNode pack = null;

					try (IGraphTransaction tx = graph.beginTransaction()) {
						// operations on the graph
						// ...

						pack = epackagedictionary.get("id", ep).getSingle();

						for (IGraphEdge r : pack
								.getIncomingWithType("epackage")) {

							IGraphNode othernode = r.getStartNode();
							if (othernode.getProperty("id").equals(
									arg0.substring(arg0.indexOf("::") + 2))) {

								found = true;
								break;

							}

						}
						tx.success();
						tx.close();
					}
					found = false;
				} else {
					if (epackages == null) {

						LinkedList<IGraphNode> possibletypenodes = new LinkedList<IGraphNode>();

						try (IGraphTransaction tx = graph.beginTransaction()) {
							// operations on the graph
							// ...

							Iterator<IGraphNode> packs = epackagedictionary
									.query("id", "*").iterator();

							while (packs.hasNext()) {

								IGraphNode pack = packs.next();
								for (IGraphEdge n : pack
										.getIncomingWithType("epackage")) {

									IGraphNode othernode = n.getStartNode();
									if (othernode.getProperty("id")
											.equals(arg0)) {

										possibletypenodes.add(othernode);

									}
								}
							}
							tx.success();
							tx.close();
						}
						if (possibletypenodes.size() == 1) {
							cachedTypes.add(arg0);
							return true;
						} else {
							System.err
									.println("ERROR IN hasType(String arg0). "
											+ possibletypenodes.size()
											+ " CLASSES FOUND FOR " + arg0
											+ ", RETURNING FALSE");
							return false;
						}

					} else {

						for (String p : epackages) {

							IGraphNode pack = null;

							try (IGraphTransaction tx = graph
									.beginTransaction()) {
								// operations on the graph
								// ...

								pack = epackagedictionary.get("id", p)
										.getSingle();

								for (IGraphEdge n : pack
										.getIncomingWithType("epackage")) {

									IGraphNode othernode = n.getStartNode();
									if (othernode.getProperty("id")
											.equals(arg0)) {
										cachedTypes.add(arg0);

										found = true;
										break;
									}
								}

								tx.success();
							} catch (Exception e) {
								System.err
										.println("hastype("
												+ arg0
												+ ") cannot find relevant meta-info in database, returning false");
								return false;
							}
						}
						return false;

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
			load((IGraphDatabase) null);
		else
			throw new EolModelLoadingException(new Exception(
					"load called with no graph store initialized"), this);
		// load(null, new EMFparser());
	}

	public void load(IGraphDatabase g) throws EolModelLoadingException {

		if (config == null)
			config = getDatabaseConfig();

		if (g != null // && graph == null
		)
			graph = g;

		// if (propertygetter == null)
		propertygetter = new GraphPropertyGetter(graph, this);

		backendURI = (String) config.get("DATABASE_LOCATION");
		// if (backendURI != null && graph == null) {
		// //init graph -- cannot do in this architecture
		// System.err.println("IGraphDatabase is null");
		// }

		name = (String) config.get(EOLQueryEngine.PROPERTY_NAME);
		// String ec = (String)
		// config.get(EOLQueryEngine.PROPERTY_ENABLE_CASHING);
		// enableCache = Boolean.parseBoolean((ec == null) ? "true" : ec);
		// System.err.println("EOL EC: " + enableCache);

		// limit to declared epckages if applicable
		Object pa = config.get(EOLQueryEngine.PROPERTY_METAMODELS);
		if (pa != null) {
			String[] eps = ((String) pa).split(",");

			if (!(eps.length == 1 && eps[0].equals("[]"))) {

				epackages = new HashSet<String>();

				for (String s : eps) {
					// System.err.println(s);
					epackages.add(s.trim().replaceAll("\\[", "")
							.replaceAll("]", "").trim());
				}
			}
		}

		if (graph != null) {

			try (IGraphTransaction tx = graph.beginTransaction()) {

				epackagedictionary = graph.getMetamodelIndex();

				tx.success();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// System.out.println("Registry: " +
			// EPackage.Registry.INSTANCE.keySet());
			// System.out.println("--Graph connection opened--");

			// System.out.println("Loging graph - disabled");
			try {
				// fullLog(graph);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// System.out.println("Graph Loged - disabled");

		} else
			throw new EolModelLoadingException(
					new Exception(
							"Attempt to load a model from a null graph please ensure that properties in: load(StringProperties properties, String basePath) "
									+ "has a field DATABASE_LOCATION with the uri as a value and DATABASE_TYPE with value the type of database used"),
					this);
	}

	@Override
	public void dispose() {
		// graph.shutdown();
		super.dispose();
		System.out.println("--Graph shut down--");

		System.out.println("--Logging Graph (disable for large)-- disabled");
		try {
			// fullLog(graph);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("--Done!--");
	}

	@SuppressWarnings("unused")
	private void fullLog(IGraphDatabase graph2) throws IOException {

		int count = 1;

		File file = new File("D:/workspace/Neo4Jstore-v2/" + "eol-graph-log-"
				+ count + ".txt");

		while (file.exists()) {
			count++;
			file = new File("D:/workspace/Neo4Jstore-v2/" + "eol-graph-log-"
					+ count + ".txt");
		}

		FileWriter w = new FileWriter(file);
		String id = null;

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			for (IGraphNode n : graph.allNodes(null)) {

				String refs = "\n--> ";

				for (IGraphEdge r : n.getOutgoing()) {

					id = "";

					try {
						id = r.getEndNode().getProperty("id").toString();
					} catch (Exception e) {
						id = "NO ID FOUND";
					}

					String x = "->";
					if (!r.getStartNode().equals(n))
						x = "<-";

					refs += "[" + r.getType() + " " + x + " "
							+ r.getEndNode().getProperty("id") + "" + "]";

				}

				String props = "";

				for (String s : n.getPropertyKeys()) {

					props += "[" + s + " : " + n.getProperty(s) + "]";

				}

				w.append(n + " : " + props + refs + "\n");

			}
			tx.success();
			tx.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		w.close();

	}

	@Override
	public boolean owns(Object arg0) {

		// System.out.println("OWNS CALLED: " + arg0);
		boolean ret = true;

		if (arg0 == null)
			return false;

		if (arg0 instanceof Collection<?>) {
			return false;
			// for (Object o : ((Collection<?>) arg0)) {
			// if (!(o instanceof GraphNodeWrapper)) {
			// System.err.println("non-neo: " + o.getClass());
			// return false;
			// } else
			// ret = ret
			// && ((GraphNodeWrapper) o).getContainerModel() == null
			// || ((GraphNodeWrapper) o).getContainerModel()
			// .equals(this);
			// }
		} else {

			if (!(arg0 instanceof GraphNodeWrapper)) {
				// System.err.println("non-neo: " + arg0.getClass());
				return false;
			} else {
				// System.out.println(((GraphNodeWrapper)arg0).tostring());
				// System.out.println(((GraphNodeWrapper)arg0).getInfo());
				// System.err.println(((GraphNodeWrapper)
				// arg0).getContainerModel());
				// System.err.println(this);
				ret = ((GraphNodeWrapper) arg0).getContainerModel() == null
						|| ((GraphNodeWrapper) arg0).getContainerModel()
								.equals(this);
			}

		}

		if (ret == false)
			System.err.println("warning owns failed on : " + arg0
					+ "\nwith getContainerModel() : "
					+ ((GraphNodeWrapper) arg0).getContainerModel()
					+ "\nand 'this' : " + this);

		return ret;
	}

	public boolean isOf(Object instance, String metaClass,
			final String typeorkind)
			throws EolModelElementTypeNotFoundException {

		// FIXMEdone catch unset returns from property getter better - >
		// org.eclipse.epsilon.eol.types.EolNoType$EolNoTypeInstance cannot be
		// cast to org.hawk.neo4j.emc.NeoIdWrapper
		// if (instance.equals("UNSET"))
		// return false;
		if (instance == null)
			return false;

		if (!(instance instanceof GraphNodeWrapper))
			return false;

		// needed?
		if (hasType(metaClass)) {

			String id = null;

			try (IGraphTransaction tx = graph.beginTransaction()) {
				// operations on the graph
				// ...

				// System.err.println(instance);

				IGraphNode objectNode = graph
						.getNodeById(((GraphNodeWrapper) instance).getId());

				IGraphNode typeNode = null;

				// String[] splitId = null;
				try {
					typeNode = objectNode.getOutgoingWithType(typeorkind)
							.iterator().next().getEndNode();
					id = typeNode.getProperty("id").toString();
				} catch (Exception e) {
					// dont have a type - only if you are iterating all the
					// nodes
					// even the metanodes
					// System.err.println("warning: metaclass node asked for its type, ignored");
					// e.printStackTrace();
				}
				tx.success();
				tx.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (id != null) {

				return metaClass.equals(id)
						|| metaClass
								.equals(id.substring(id.lastIndexOf("/") + 1));
			} else {

				return false;
			}
			// doesn't exist in database
			// tx.success();tx.close();

		} else
			throw new EolModelElementTypeNotFoundException(this.getName(),
					metaClass);

	}

	protected StringProperties getDefaultDatabaseConfig() {

		long x = Runtime.getRuntime().maxMemory() / 1000000 / 60;
		StringProperties defaultConfig = new StringProperties();
		defaultConfig.put("neostore.nodestore.db.mapped_memory", 5 * x + "M");
		defaultConfig.put("neostore.relationshipstore.db.mapped_memory", 15 * x
				+ "M");
		defaultConfig.put("neostore.propertystore.db.mapped_memory", 20 * x
				+ "M");
		defaultConfig.put("neostore.propertystore.db.strings.mapped_memory", 2
				* x + "M");
		defaultConfig.put("neostore.propertystore.db.arrays.mapped_memory", x
				+ "M");
		defaultConfig.put("keep_logical_logs", "false");

		File genericWorkspaceFile = new File("");

		defaultConfig.put(databaseLocation, new File(new File(
				genericWorkspaceFile.getAbsolutePath().replaceAll("\\\\", "/"))
				.getParentFile().getAbsolutePath())
				+ "/DB");

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
		//
		// // System.out.println("--");
		//
		// // if (instance.equals("UNSET"))
		// // return true;
		//
		// Node n = null;
		//
		// try (Transaction tx = graph.beginTx()) {
		// // operations on the graph
		// // ...
		//
		// n = graph.getNodeById(((GraphNodeWrapper) instance).getId());
		//
		// tx.success();
		// tx.close();
		// }
		//
		// HawkClass e = new MetamodelUtils().getTypeOfFromNode(n, parser);
		//
		// if (e != null) {
		//
		// if (property.startsWith("revRefNav_")) {
		//
		// String property2 = property.substring(10);
		//
		// return true;
		// }
		//
		// if (property.equals("eContainer")) {
		//
		// // delegate to propertygetter
		// return true;
		//
		// }
		//
		// boolean ret = getEStructuralFeature(e, property) != null;
		//
		// if (!ret)
		// System.err.println("warning: knowsAboutProperty(\n" + instance
		// + "\n" + property + "\n) failed, returning false [0]");
		// // System.out.println(e.getName());
		// // System.out.println(e.geturi());
		// // System.out.println(e.geturifragment());
		// return ret;
		//
		// } else {
		// System.err.println("warning: knowsAboutProperty(\n" + instance
		// + "\n" + property + "\n) failed, returning false [1]");
		// return false;
		// }
	}

	@Override
	public IPropertyGetter getPropertyGetter() {

		if (propertygetter == null)
			System.err.println("null property getter, was run() called?");

		return propertygetter;

	}

	@Override
	public IPropertySetter getPropertySetter() {
		return null;// new NeoPropertySetter(graph, this);
	}

	public IGraphDatabase getBackend() {
		return graph;
	}

	public void dumpMinimalDatabaseConfig() {

		for (Object c : config.keySet())
			System.out.println(">" + c + " = " + config.get(c));

	}

	public void dumpDatabaseConfig() {

		for (Object c : graph.getConfig().keySet())
			System.out.println(c + " = " + graph.getConfig().get(c));

	}

	@Override
	public Object contextlessQuery(IGraphDatabase g, String query) {
		return contextlessQuery(g, query, null);
	}

	public Object contextlessQuery(IGraphDatabase g, String query, String name) {

		Object ret = null;

		try {

			load(g);

			long truestart = System.currentTimeMillis();

			EolModule module = new EolModule();

			module.parse(query);

			if (name != null)
				System.out.println("PARSING:\n----------\n" + name
						+ "\n----------");

			System.out.println("Graph path: " + graph.getPath()
					+ "\n----------");

			module.getContext().getModelRepository().addModel(this);

			long init = System.currentTimeMillis();

			ret = module.execute();
			if (ret == null)
				ret = "no result returned (maybe it directly printed the result to console?)";

			System.err.println("time variable = " + EOLQueryEngine.time + "ms");

			System.out
					.println("QUERY TOOK "
							+ (System.currentTimeMillis() - init) / 1000 + "s"
							+ (System.currentTimeMillis() - init) % 1000
							+ "ms, to run");

			init = System.nanoTime();

			System.err.println("true time before disposal: "
					+ (System.currentTimeMillis() - truestart) / 1000 + "s"
					+ (System.currentTimeMillis() - truestart) % 1000 + "ms");

			// model.dispose();

			System.out.println("DISPOSAL TOOK ~" + (System.nanoTime() - init)
					/ 1000000000 + "s to run");

			System.err.println("true time after disposal: "
					+ (System.currentTimeMillis() - truestart) / 1000 + "s"
					+ (System.currentTimeMillis() - truestart) % 1000 + "ms");

		} catch (Exception e) {
			System.err.println("query failed, returning null:");
			e.printStackTrace();
		}

		return ret;
	}

	public Object contextlessQuery(IGraphDatabase g, File query) {
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
		return contextlessQuery(g, code, query.getPath());
	}

	@Override
	public Object contextfullQuery(IGraphDatabase g, String query,
			Map<String, String> context) {

		Object ret = null;

		String interestingFiles = null;

		if (context != null)
			interestingFiles = context.get(PROPERTY_FILECONTEXT);

		if (interestingFiles != null) {

			try {

				load(g);

				long truestart = System.currentTimeMillis();

				EolModule module = new EolModule();

				module.parse(query);

				System.out.println("Graph path: " + graph.getPath()
						+ "\n----------");

				CEOLQueryEngine q = new CEOLQueryEngine();

				name = context.get(EOLQueryEngine.PROPERTY_NAME);
				if (name == null)
					name = "Model";

				// defaults to true
				// String ec =
				// context.get(EOLQueryEngine.PROPERTY_ENABLE_CASHING);
				// enableCache = ec == null ? true :
				// ec.equalsIgnoreCase("true");

				// limit to declared packages if applicable
				String pa = context.get(EOLQueryEngine.PROPERTY_METAMODELS);
				if (pa != null) {
					String[] eps = ((String) pa).split(",");

					if (!(eps.length == 1 && eps[0].equals("[]"))) {

						epackages = new HashSet<String>();

						for (String s : eps) {
							// System.err.println(s);
							epackages.add(s.trim().replaceAll("\\[", "")
									.replaceAll("]", "").trim());
						}
					}
				}

				q.load(context);

				module.getContext().getModelRepository().addModel(q);

				long init = System.currentTimeMillis();

				ret = module.execute();
				if (ret == null)
					ret = "no result returned (maybe it directly printed the result to console?)";

				System.err.println("time variable = " + EOLQueryEngine.time
						+ "ms");

				System.out.println("QUERY TOOK "
						+ (System.currentTimeMillis() - init) / 1000 + "s"
						+ (System.currentTimeMillis() - init) % 1000
						+ "ms, to run");

				init = System.nanoTime();

				System.err.println("true time before disposal: "
						+ (System.currentTimeMillis() - truestart) / 1000 + "s"
						+ (System.currentTimeMillis() - truestart) % 1000
						+ "ms");

				// model.dispose();

				System.out.println("DISPOSAL TOOK ~"
						+ (System.nanoTime() - init) / 1000000000 + "s to run");

				System.err.println("true time after disposal: "
						+ (System.currentTimeMillis() - truestart) / 1000 + "s"
						+ (System.currentTimeMillis() - truestart) % 1000
						+ "ms");

			} catch (Exception e) {
				System.err.println("query failed, returning null:");
				e.printStackTrace();
			}

			return ret;

		} else {
			System.err
					.println("contextfullQuery passed no valid arguments as context, running contextlessQuery instead:");
			return contextlessQuery(g, query);
		}

	}

	@Override
	public Object contextfullQuery(IGraphDatabase g, File query,
			Map<String, String> context) {
		graph = g;
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
		return contextfullQuery(g, code, context);
	}

	// deriving attributes
	@Override
	public AccessListener calculateDerivedAttributes(IGraphDatabase g,
			Iterable<IGraphNode> nodes) {

		graph = g;

		if (config == null)
			config = getDatabaseConfig();

		if (propertygetter == null)
			propertygetter = new GraphPropertyGetter(graph, this);

		StringProperties configuration = new StringProperties();
		configuration.put(EOLQueryEngine.PROPERTY_ENABLE_CASHING, true);
		setDatabaseConfig(configuration);

		try {
			load(graph);
		} catch (EolModelLoadingException e2) {
			e2.printStackTrace();
		}

		// listen to accesses
		GraphPropertyGetter pg = (GraphPropertyGetter) getPropertyGetter();
		pg.setBroadcastAccess(true);

		//
		// init eol stuff
		//

		HashMap<String, EolModule> cashedModules = new HashMap<String, EolModule>();

		try (IGraphTransaction tx = graph.beginTransaction()) {

			for (IGraphNode n : nodes) {

				// System.err.println(n.getId());
				// ...
				for (String s : n.getPropertyKeys()) {

					String prop = n.getProperty(s).toString();

					if (prop.startsWith("_NYD##")) {

						Object derived = "DERIVATION_EXCEPTION";
						try {
							Object enablecashing = getDatabaseConfig().get(
									EOLQueryEngine.PROPERTY_ENABLE_CASHING);
							if (enablecashing != null
									&& (enablecashing.equals("true") || enablecashing
											.equals(true))) {

								derived = new DeriveFeature().deriveFeature(
										cashedModules, graph, n, this, s, prop);

							} else

								derived = new DeriveFeature().deriveFeature(
										null, graph, n, this, s, prop);

						} catch (Exception e1) {
							System.err
									.println("Exception in deriving attribute");
							e1.printStackTrace();
						}

						// System.err.println(derived.getClass());
						// System.err.println(derived);

						n.setProperty(s, derived);

						IGraphNode elementnode = n.getIncoming().iterator()
								.next().getStartNode();

						IGraphNodeIndex derivedFeature = graph
								.getOrCreateNodeIndex(

								elementnode.getOutgoingWithType("typeOf")
										.iterator().next().getEndNode()
										.getOutgoingWithType("epackage")
										.iterator().next().getEndNode()
										.getProperty("id").toString()
										//
										// e.getEPackage().getNsURI()
										+ "##"
										// -
										+ elementnode
												.getOutgoingWithType("typeOf")
												.iterator().next().getEndNode()
												.getProperty("id").toString()
										//
										+ "##" + s);

						// System.err.println(">< " + derived);

						// XXXdeprecated derived attribute to metamodel
						// resolution -
						// only needed if no info in mm

						// n.getRelationships(
						// Direction.OUTGOING,
						// RelationshipUtil
						// .getNewRelationshipType("typeOf"))
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

				IGraphNodeIndex derivedProxyDictionary = graph
						.getOrCreateNodeIndex("derivedproxydictionary");
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

			tx.success();
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
			derivationlogic = "return " + derivationlogic + ";";
			module.parse(derivationlogic);
			for (ParseProblem p : module.getParseProblems())
				ret.add(p.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.err.println(ret);

		return ret;
	}

}
