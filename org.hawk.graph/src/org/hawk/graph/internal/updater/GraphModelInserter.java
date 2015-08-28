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
package org.hawk.graph.internal.updater;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChange;
import org.hawk.core.graph.IGraphChangeDescriptor;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.query.IAccess;
import org.hawk.core.query.IAccessListener;
import org.hawk.core.query.IQueryEngine;
import org.hawk.graph.internal.util.GraphUtil;
import org.hawk.graph.listener.IGraphChangeListener;

/**
 * creates a database with the input xmi file (in args[0]) or reads it if the
 * database exists; and provides debugging information
 * 
 */
public class GraphModelInserter {

	private static final int maxTransactionalAcceptableLoad = Integer.MAX_VALUE;

	@SuppressWarnings("unused")
	private int unset = 0; // number of unset references (used for logging)

	private String repoURL;
	private String tempFolderURI;

	private IHawkModelResource resource;
	private Map<String, IHawkObject> delta = new HashMap<>();
	private Map<String, IHawkObject> added = new HashMap<>();
	private Map<String, IHawkObject> unchanged = new HashMap<>();

	private IModelIndexer indexer;
	private IGraphDatabase graph;
	private GraphModelBatchInjector inj;
	private VcsCommitItem s;

	private Map<String, IGraphNode> nodes = new HashMap<>();
	private final IGraphChangeListener listener;

	public GraphModelInserter(IModelIndexer hawk, IGraphChangeListener listener) {
		indexer = hawk;
		graph = indexer.getGraph();
		tempFolderURI = new File(graph.getTempDir()).toURI().toString();
		this.listener = listener;
	}

	public boolean run(IHawkModelResource res, VcsCommitItem s) throws Exception {
		resource = res;
		this.s = s;

		// indexer = i;
		inj = new GraphModelBatchInjector(graph, this.s, listener);

		try {
			// f = new File(dir + "/" + s.getPath());

			int delta = calculateModelDeltaSize();
			if (delta != -1) {

				System.err
						.println("file already present, calculating deltas with respect to graph storage");
				//
				if (delta > maxTransactionalAcceptableLoad) {
					System.err.print("[" + delta + ">"
							+ maxTransactionalAcceptableLoad + "] ");
					batchUpdate();
				} else {
					System.err.print("[" + delta + "<"
							+ maxTransactionalAcceptableLoad + "] ");
					transactionalUpdate();
				}

				//
				// FIXMEdone -- at end of all updates similar to derived proxy
				// // for each change see if any derived attributes need
				// re-calculation - if they do add em to derived proxy
				// dictionary for re-calculation!
				//
			} else {
				// populate the database from scratch (for this file) -- this
				// will trigger calculation of all derived attrs
				addNodes();
			}

			System.out
					.print("\nProgram ending with no errors, shutting down database...");

			return true;
		} finally {
			long l = System.nanoTime();
			// t.success();
			// t.finish();
			// graph.shutdown();
			System.out.println("(took ~" + (System.nanoTime() - l) / 1000000000
					+ "sec to commit changes)");
		}
	}

	@SuppressWarnings("unchecked")
	private void transactionalUpdate() throws Exception {
		graph.exitBatchMode();

		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.indexerStart();

			repoURL = s.getCommit().getDelta().getRepository().getUrl();
			IGraphNode fileNode = indexer
					.getGraph()
					.getFileIndex()
					.get("id",
							repoURL
									+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
									+ s.getPath()).iterator().next();

			// add new nodes
			final Map<IGraphNode, IHawkObject> addedNodes = new HashMap<>();
			final Map<String, IGraphNode> addedNodesHash = new HashMap<>();

			for (String o : added.keySet()) {
				IHawkObject object = added.get(o);
				IGraphNode node = inj.addEObject(fileNode, object);
				addedNodes.put(node, object);
				addedNodesHash.put(node.getProperty("id").toString(), node);

				// track change new node
				listener.modelElementAddition(s, object, node);
				for (String key : node.getPropertyKeys()) {
					listener.modelElementAttributeUpdate(s, object, key, null, node.getProperty(key), node);
				}
				for (IGraphEdge e : node.getOutgoingWithType("typeOf")) {
					listener.referenceAddition(s, node, e.getEndNode(), "typeOf");
				}
				for (IGraphEdge e : node.getOutgoingWithType("kindOf")) {
					listener.referenceAddition(s, node, e.getEndNode(), "kindOf");
				}
				for (IGraphEdge e : node.getOutgoingWithType("file")) {
					listener.referenceAddition(s, node, e.getEndNode(), "file");
				}
			}

			// references of added object and tracking of changes
			for (IGraphNode node : addedNodes.keySet()) {
				inj.addEReferences(fileNode, node, addedNodes.get(node), addedNodesHash, nodes);
			}

			// delete obsolete nodes and change attributes
			for (String s : nodes.keySet()) {

				IGraphNode node = nodes.get(s);

				if (unchanged.containsKey(node.getProperty("id"))) {
					// do nothing
				} else if (delta.containsKey(node.getProperty("id"))) {
					IHawkObject o = delta.get(node.getProperty("id"));
					//
					// System.err.println("changing node "
					// + node.getId()
					// + " : "
					// + node.getProperty("id")
					// + " : "
					// + node.getOutgoingWithType("typeOf").iterator()
					// .next().getEndNode().getProperty("id")
					// + " :: as new model has altered it!");

					// for (String ss : node.getPropertyKeys()) {
					// reset properties - deprecated
					//
					// System.err.println("previous attribute:\t" + ss);
					// System.err.println("previous value:\t\t" +
					// node.getProperty(ss));
					//
					// node.removeProperty(ss);
					// }

					// node.setProperty("id", o.getUriFragment());
					node.setProperty("hashCode", o.hashCode());
					updateNodeProperties(fileNode, node, o);
					//
					// for (String ss : node.getPropertyKeys()) {
					// System.err.println("new attribute:\t" + ss);
					// System.err.println("new value:\t" +
					// node.getProperty(ss)); }
					//

				} else {
					// not in unchanged or updated so its deleted
					//
					// System.err.println("deleting node " + node +
					// " as new model does not contain it!");
					//

					// track change deleted node
					listener.modelElementRemoval(this.s, node);
					for (String key : node.getPropertyKeys()) {
						listener.modelElementAttributeRemoval(this.s, null, key, node);
					}
					for (IGraphEdge e : node.getOutgoing()) {
						if (e.getProperty("isDerived") == null) {
							listener.referenceRemoval(this.s, node, e.getEndNode());
						}
					}

					remove(node, repoURL, fileNode);
					// new DeletionUtils(graph).delete(node);
				}

			}

			// change references
			for (String o : delta.keySet()) {
				IHawkObject ob = delta.get(o);
				IGraphNode node = nodes.get(ob.getUriFragment());

				// its null if it was just inserted (above), this is fine.
				if (node == null)
					continue;

				for (IHawkReference r : ((IHawkClass) ob.getType())
						.getAllReferences()) {

					if (ob.isSet(r)) {

						Object targets = ob.get(r, false);
						String refname = r.getName();
						boolean isContainment = r.isContainment();
						boolean isContainer = r.isContainer();
						Set<String> targetids = new HashSet<>();

						if (targets instanceof Iterable<?>) {
							for (IHawkObject h : ((Iterable<IHawkObject>) targets)) {
								if (!h.isProxy())
									targetids.add(h.getUriFragment());
								else {
									addProxyRef(node, h, refname);
								}
							}
						} else {
							if (!((IHawkObject) targets).isProxy())
								targetids.add(((IHawkObject) targets)
										.getUriFragment());
							else {
								addProxyRef(node, (IHawkObject) targets,
										refname);
							}
						}

						//
						Iterable<IGraphEdge> graphtargets = node
								.getOutgoingWithType(refname);

						for (IGraphEdge e : graphtargets) {
							IGraphNode n = e.getEndNode();

							if (targetids.contains(n.getProperty("id"))) {
								// update changed list
								targetids.remove(n.getProperty("id"));
							} else {
								// delete removed reference
								e.delete();

								// track change deleted reference
								listener.referenceRemoval(this.s, node, e.getEndNode());
							}
						}

						for (String s : targetids) {

							IGraphNode dest = nodes.get(s);
							if (dest != null) {
								// add new reference
								IGraphEdge e = graph.createRelationship(node, dest, refname);
								if (isContainment) {
									e.setProperty("isContainment", "true");
								}
								if (isContainer) {
									e.setProperty("isContainer", "true");
								}

								// track change new reference
								listener.referenceAddition(this.s, node, dest, refname);
							} else {
								// proxy reference, handled above
							}
						}

					} else {
						// delete unset references which may have been
						// previously set
						String refname = r.getName();
						Iterable<IGraphEdge> graphtargets = node
								.getOutgoingWithType(refname);
						// track change deleted references
						for (IGraphEdge e : graphtargets) {
							e.delete();
							listener.referenceRemoval(this.s, node, e.getEndNode());
						}
					}

				} // for (IHawkReference r)

			} // for (String o)

			fileNode.setProperty("revision", s.getCommit().getRevision());
			t.success();
		} catch (Exception e) {
			System.err.println("exception in transactionalUpdate()");
			e.printStackTrace();
		}

	}

	private boolean addProxyRef(IGraphNode node, IHawkObject destinationObject,
			String edgelabel) {

		try {
			// proxydictionary.add(graph.getNodeById(hash.get((from))),
			// edgelabel,
			// ((EObject)destinationObject).eIsProxy());

			String uri = destinationObject.getUri();

			String destinationObjectRelativePathURI =
			// new DeletionUtils(graph).getRelativeURI(
			uri
			// .toString())
			;

			if (!destinationObject.URIIsRelative()) {

				destinationObjectRelativePathURI = new DeletionUtils(graph)
						.makeRelative(tempFolderURI,
								destinationObjectRelativePathURI);

			}
			// System.err.println(uri.toString().substring(uri.toString().indexOf(".metadata/.plugins/com.google.code.hawk.neo4j/temp/m/")+53));
			// System.err.println(uri.);

			String destinationObjectRelativeFileURI = destinationObjectRelativePathURI;

			destinationObjectRelativeFileURI = destinationObjectRelativePathURI
					.substring(destinationObjectRelativePathURI.indexOf("#"));

			String destinationObjectFullPathURI = repoURL
					+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
					+ destinationObjectRelativePathURI;

			String destinationObjectFullFileURI = repoURL
					+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
					+ destinationObjectRelativeFileURI;

			Object proxies = null;
			// if (withProxy.hasProperty("_proxyRef:" + relativeFileURI)) {
			// proxies = withProxy.getProperty("_proxyRef:" +
			// relativeFileURI);
			// }
			// System.err.println(">>>>>>>"+relativeFileURI);

			proxies = node.getProperty("_proxyRef:"
					+ destinationObjectFullFileURI);
			proxies = new DeletionUtils(graph)
					.addToElementProxies((String[]) proxies,
							destinationObjectFullPathURI, edgelabel);

			node.setProperty("_proxyRef:" + destinationObjectFullFileURI,
					proxies);

			HashMap<String, Object> m = new HashMap<>();
			m.put("_proxyRef", destinationObjectFullFileURI);

			IGraphNodeIndex proxyDictionary = graph
					.getOrCreateNodeIndex("proxydictionary");
			proxyDictionary.add(node, m);

		} catch (Exception e) {
			System.err.println("proxydictionary error:");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void updateNodeProperties(IGraphNode fileNode, IGraphNode node,
			IHawkObject eObject) {

		LinkedList<IHawkAttribute> normalattributes = new LinkedList<IHawkAttribute>();
		LinkedList<IHawkAttribute> indexedattributes = new LinkedList<IHawkAttribute>();
		// LinkedList<IHawkAttribute> derivedattributes = new
		// LinkedList<IHawkAttribute>();

		IGraphNode typenode = node.getOutgoingWithType("typeOf").iterator()
				.next().getEndNode();

		for (final IHawkAttribute eAttribute : ((IHawkClass) eObject.getType())
				.getAllAttributes()) {

			if (eObject.isSet(eAttribute)) {

				if (((String[]) typenode.getProperty(eAttribute.getName()))[5]
						.equals("t"))
					indexedattributes.add(eAttribute);

				normalattributes.add(eAttribute);

			} else
			// depracatedTODO currently unset items are not included to may
			// crash eol etc
			{
				// node.setProperty(eAttribute.getName(), "UNSET");
			}
		}

		for (IHawkAttribute a : normalattributes) {
			final Object oldproperty = node.getProperty(a.getName());
			final Object newproperty = eObject.get(a);

			if (!a.isMany()) {
				Object newValue = newproperty;
				if (!new GraphUtil().isPrimitiveOrWrapperType(newproperty.getClass())) {
					newValue = newValue.toString();
				}

				if (!newValue.equals(oldproperty)) {
					// track changed property (primitive)
					listener.modelElementAttributeUpdate(this.s, eObject, a.getName(), oldproperty, newproperty, node);
					node.setProperty(a.getName(), newproperty);
				}
			}
			else {
				Collection<Object> collection = null;

				if (a.isOrdered() && a.isUnique())
					collection = new LinkedHashSet<Object>();
				else if (a.isOrdered())
					collection = new LinkedList<Object>();
				else if (a.isUnique())
					collection = new HashSet<Object>();
				else
					collection = new LinkedList<Object>();

				final Collection<?> srcCollection = (Collection<?>) newproperty;
				Class<?> elemClass = null;
				boolean primitiveOrWrapperClass = false;
				if (!srcCollection.isEmpty()) {
					final Object first = srcCollection.iterator().next();
					elemClass = first.getClass();
					primitiveOrWrapperClass = new GraphUtil()
							.isPrimitiveOrWrapperType(elemClass);
					if (primitiveOrWrapperClass) {
						for (Object o : srcCollection) {
							collection.add(o);
						}
					} else {
						for (Object o : srcCollection) {
							collection.add(o.toString());
						}
					}
				}

				Object r = null;
				if (primitiveOrWrapperClass && elemClass != null) {
					r = Array.newInstance(elemClass, 1);
				} else {
					r = Array.newInstance(String.class, 1);
				}
				Object ret = collection.toArray((Object[]) r);

				if (!ret.equals(oldproperty)) {
					listener.modelElementAttributeUpdate(this.s, eObject, a.getName(), oldproperty, ret, node);
					node.setProperty(a.getName(), ret);
				}
			}

		}

		// deferred for later
		// for (IHawkAttribute a : derivedattributes) {
		//
		// HashSet<IHawkAnnotation> ean = a.getAnnotations();
		//
		// for (IHawkAnnotation e : ean) {
		//
		// HashMap<String, String> map = e.getDetails();
		//
		// if (map != null && map.containsKey("EOLCode")) {
		//
		// node.setProperty(a.getName(), "_NYD##" + map.get("EOLCode"));
		// inj.addToProxyAttributes(node);
		//
		// } else if (map != null && map.containsKey("String")) {
		//
		// // BatchInserterIndex i = index.nodeIndex(eObject
		// // .eClass().getEPackageNSURI()
		// // + "##"
		// // + eObject.eClass().getName()
		// // + "##"
		// // + a.getName(), MapUtil
		// // .stringMap(IndexManager.PROVIDER,
		// // "lucene", "type", "exact"));
		// String prop = map.get("String").toString();
		// // i.add(node, "value", prop);
		// node.setProperty(a.getName(), prop);
		//
		// } else {
		//
		// System.err
		// .println("derived feature has no runnable eol code or static string, hence is ignored");
		//
		// }
		//
		// }
		// }

		for (IHawkAttribute a : indexedattributes) {

			// FIXME update property index on changes not just insert them
			IGraphNodeIndex i = graph.getOrCreateNodeIndex(eObject.getType()
					.getPackageNSURI()
					+ "##"
					+ eObject.getType().getName()
					+ "##" + a.getName());

			Object v = eObject.get(a);

			if (!a.isMany()) {

				if (new GraphUtil().isPrimitiveOrWrapperType(v.getClass()))
					i.add(node, a.getName(), v);

				else
					i.add(node, a.getName(), v.toString());

			}

			else {

				Collection<Object> collection = null;

				if (a.isOrdered() && a.isUnique())
					collection = new LinkedHashSet<Object>();
				else if (a.isOrdered())
					collection = new LinkedList<Object>();
				else if (a.isUnique())
					collection = new HashSet<Object>();
				else
					collection = new LinkedList<Object>();

				final Collection<?> srcCollection = (Collection<?>) v;
				Class<?> elemClass = null;
				boolean primitiveOrWrapperClass = false;
				if (!srcCollection.isEmpty()) {
					final Object first = srcCollection.iterator().next();
					elemClass = first.getClass();
					primitiveOrWrapperClass = new GraphUtil()
							.isPrimitiveOrWrapperType(elemClass);
					if (primitiveOrWrapperClass) {
						for (Object o : srcCollection) {
							collection.add(o);
						}
					} else {
						for (Object o : srcCollection) {
							collection.add(o.toString());
						}
					}
				}

				Object r = null;
				if (primitiveOrWrapperClass && elemClass != null) {
					r = Array.newInstance(elemClass, 1);
				} else {
					r = Array.newInstance(String.class, 1);
				}
				Object ret = collection.toArray((Object[]) r);

				i.add(node, a.getName(), ret);

			}

		}

		final IGraphNodeIndex rootDictionary = graph
				.getOrCreateNodeIndex(GraphModelBatchInjector.ROOT_DICT_NAME);
		if (eObject.isRoot()) {
			rootDictionary.add(node,
					GraphModelBatchInjector.ROOT_DICT_FILE_KEY,
					fileNode.getId());
		} else {
			rootDictionary.remove(node);
		}
	}

	private void batchUpdate() throws Exception {
		System.err.println("batch update called");

		graph.exitBatchMode();
		final String repositoryURL = s.getCommit().getDelta().getRepository().getUrl();
		new DeletionUtils(graph).deleteAll(repositoryURL, s.getPath());

		graph.enterBatchMode();
		new GraphModelBatchInjector(graph, s, resource, listener);
		graph.exitBatchMode();
	}

	private int calculateModelDeltaSize() throws Exception {

		System.err.println("calculateModelDeltaSize() called:");

		try (IGraphTransaction t = graph.beginTransaction()) {

			final String repositoryURL = s.getCommit().getDelta()
					.getRepository().getUrl();
			boolean modelfilealreadypresent = graph
					.getFileIndex()
					.get("id",
							repositoryURL
									+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
									+ s.getPath()).iterator().hasNext();

			if (modelfilealreadypresent) {
				int delta = 0;
				HashMap<String, Integer> hashCodes = new HashMap<>();

				for (IGraphEdge e : graph
						.getFileIndex()
						.get("id",
								repositoryURL
										+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
										+ s.getPath()).getSingle()
						.getIncomingWithType("file")) {
					IGraphNode n = e.getStartNode();

					// TODO Ask Kostas - this map only takes into account the
					// name of the file and not the repo: is it safe as is?
					nodes.put(n.getProperty("id").toString(), n);

					hashCodes.put((String) n.getProperty("id"),
							(int) n.getProperty("hashCode"));
				}
				System.err.println("file contains: " + nodes.size() + " ("
						+ hashCodes.size() + ") nodes in store");

				int newo = 0;

				Iterator<IHawkObject> iterator = resource.getAllContents();
				while (iterator.hasNext()) {
					IHawkObject o = iterator.next();
					Integer hash = hashCodes.get(o.getUriFragment());
					if (hash != null) {
						if (hash != o.hashCode()) {
							delta++;
							this.delta.put(o.getUriFragment(), o);
						} else {
							this.unchanged.put(o.getUriFragment(), o);

						}
					} else {
						newo++;
						delta++;
						this.added.put(o.getUriFragment(), o);
					}
				}
				t.success();
				System.err.println("delta is of size: " + (delta - newo) + "+"
						+ newo + "+" + +(nodes.size() - unchanged.size()));
				return delta + nodes.size() - unchanged.size();
			} else {
				t.success();
				System.err
						.println("file not in store, performing initial batch file insertion");
				return -1;
			}

		}
	}

	/**
	 * Populates the database with the model, using util.parseresource
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	private void addNodes() throws Exception {
		if (resource != null) {
			GraphModelBatchInjector batch = new GraphModelBatchInjector(graph, s, resource, listener);
			unset = batch.getUnset();
		} else {
			System.err
				.println("model insertion aborted, see above error (maybe you need to register the metamodel?)");
		}

		try {
			System.gc();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void remove(IGraphNode modelElement, String repositoryURL, IGraphNode fileNode) {
		DeletionUtils del = new DeletionUtils(graph);
		del.dereference(modelElement);
		del.makeProxyRefs(modelElement, repositoryURL, fileNode);
		del.delete(modelElement);
	}

	/*
	 * Deprected
	 */
	@SuppressWarnings("unused")
	private void init(File dbloc) {

		try {

			// create resource set
			// modelResourceSet = new ResourceSetImpl();
			// modelResourceSet.getResourceFactoryRegistry()
			// .getExtensionToFactoryMap()
			// .put("ecore", new EcoreResourceFactoryImpl());
			// modelResourceSet.getResourceFactoryRegistry()
			// .getExtensionToFactoryMap()
			// .put("*", new XMIResourceFactoryImpl());

			long cpu = System.nanoTime();

			// String db = System.getProperty("user.dir").replaceAll("\\\\",
			// "/")
			// + "/svntest";
			String db = dbloc.getPath();

			long x = Runtime.getRuntime().maxMemory() / 1000000 / 60;

			Map<String, String> config = new HashMap<String, String>();
			config.put("neostore.nodestore.db.mapped_memory", 3 * x + "M");
			config.put("neostore.relationshipstore.db.mapped_memory", 14 * x
					+ "M");
			config.put("neostore.propertystore.db.mapped_memory", x + "M");
			config.put("neostore.propertystore.db.strings.mapped_memory", 2 * x
					+ "M");
			config.put("neostore.propertystore.db.arrays.mapped_memory", x
					+ "M");

			// File fi = new File(db);

			// deletion of database each time used for debugging, remove to
			// make
			// it reference a static db
			// NB: DELETE ALL DATABASES BEFORE COMMITING ELSE YOU
			// WILL MAKE THE SVN ANGRY!!!
			// if (fi.exists()) {
			// if (!deleteDir(fi)) {
			// System.err
			// .println("Cannot delete database, exiting program, io error!");
			// oldSystemdotexit(1);
			// }
			// }

			// graph = new EmbeddedGraphDatabase(db, config);

			// registerShutdownHook(graph);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int resolveProxies(IGraphDatabase graph, IGraphChangeDescriptor ret) throws Exception {

		long start = System.currentTimeMillis();

		int proxiesLeft = -1;

		graph.exitBatchMode();

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			IGraphNodeIndex proxyDictionary = graph
					.getOrCreateNodeIndex("proxydictionary");

			IGraphIterable<IGraphNode> proxiesToBeResolved = proxyDictionary
					.query("_proxyRef", "*");

			if (proxiesToBeResolved != null && proxiesToBeResolved.size() > 0) {

				for (IGraphNode n : proxiesToBeResolved) {

					Set<String[]> allProxies = new HashSet<>();
					for (String propertyKey : n.getPropertyKeys()) {
						if (propertyKey.startsWith("_proxyRef:")) {
							final String[] propertyValue = (String[]) n
									.getProperty(propertyKey);
							allProxies.add(propertyValue);
						}
					}

					for (String[] proxies : allProxies) {
						String fullPathURI = proxies[0].substring(0,
								proxies[0].indexOf("#"));

						final String[] split = fullPathURI.split(GraphModelUpdater.FILEINDEX_REPO_SEPARATOR, 2);
						final String repoURL = split[0];
						final String filePath = split[1];

						final IGraphNode fileNode = getFileNode(graph, repoURL, filePath);
						Iterable<IGraphEdge> rels = allNodesWithFile(fileNode);

						if (rels != null) {
							HashSet<IGraphNode> nodes = new HashSet<IGraphNode>();
							for (IGraphEdge r : rels)
								nodes.add(r.getStartNode());

							if (nodes.size() != 0) {

								for (int i = 0; i < proxies.length; i = i + 2) {
									boolean found = false;

									for (IGraphNode no : nodes) {
										String nodeURI = fullPathURI
												+ "#"
												+ no.getProperty("id")
														.toString();

										if (nodeURI.equals(proxies[i])) {

											boolean change = new GraphModelBatchInjector(graph, null, listener).resolveProxyRef(n, no, proxies[i + 1]);

											if (!change) {
												System.err
														.println("resolving proxy ref returned false, edge already existed: "
																+ n.getId()
																+ " - "
																+ proxies[i + 1]
																+ " -> "
																+ no.getId());
											} else {
												listener.referenceAddition(this.s, n, no, proxies[i+1]);
											}

											found = true;
											break;
										}
									}

									if (!found)
										System.err
												.println("[GraphModelInserter | resolveProxies] Warning: proxy unresolved: "
														+ proxies[i + 1]
														+ " "
														+ proxies[i]);
								}

								n.removeProperty("_proxyRef:" + fullPathURI);
								proxyDictionary.remove("_proxyRef",
										fullPathURI, n);

							} else
								System.err
										.println("[GraphModelInserter | resolveProxies] Warning: no nodes were found for file: "
												+ filePath
												+ " originating in repository "
												+ repoURL
												+ " "
												+ proxies.length
												/ 2
												+ " proxies cannot be resolved");
						}
					}
				}

			}

			tx.success();
			tx.close();
		}

		try (IGraphTransaction tx = graph.beginTransaction()) {
			IGraphNodeIndex proxyDictionary = graph
					.getOrCreateNodeIndex("proxydictionary");

			proxiesLeft = proxyDictionary.query("_proxyRef", "*").size();

			System.out.println(proxiesLeft
					+ " - sets of proxy references left in the store");
			tx.success();
			tx.close();
		}

		System.out.println("proxy resolution took: ~"
				+ (System.currentTimeMillis() - start) / 1000 + "s");

		return proxiesLeft;

	}

	public int resolveDerivedAttributeProxies(IGraphDatabase graph,
			IModelIndexer m, String type) throws Exception {

		int derivedLeft = -1;
		Iterable<IGraphNode> allUnresolved = null;
		IGraphNodeIndex derivedProxyDictionary = null;
		int size = 0;

		System.err.println("deriving attributes...");

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			// doneFIXME (a) discuss strategy for class level annotations

			derivedProxyDictionary = graph
					.getOrCreateNodeIndex("derivedproxydictionary");

			allUnresolved = derivedProxyDictionary.query("derived", "*");

			size = ((IGraphIterable<IGraphNode>) allUnresolved).size();

			tx.success();
		}

		if (size > 0) {

			// GraphEpsilonModel model = new GraphEpsilonModel();
			// model.hawkContainer = this;
			// System.err.println(indexer.getKnownQueryLanguages());
			// System.err.println(type);

			IQueryEngine q = indexer.getKnownQueryLanguages().get(type);
			//
			// System.err.println(indexer.getKnownQueryLanguages());
			// System.err.println(type);
			//
			IAccessListener accessListener = q.calculateDerivedAttributes(
					graph, allUnresolved);

			// dump access to lucene and add hooks on updates

			try (IGraphTransaction tx = graph.beginTransaction()) {
				// operations on the graph
				// ...

				IGraphNodeIndex derivedAccessDictionary = graph
						.getOrCreateNodeIndex("derivedaccessdictionary");

				// reset accesses of nodes updated
				// ...

				for (IAccess a : accessListener.getAccesses()) {

					IGraphNode sourceNode = graph.getNodeById(a
							.getSourceObjectID());

					if (sourceNode != null)
						derivedAccessDictionary.remove(sourceNode);

				}

				for (IAccess a : accessListener.getAccesses()) {

					IGraphNode sourceNode = graph.getNodeById(a
							.getSourceObjectID());

					if (sourceNode != null)
						derivedAccessDictionary.add(sourceNode,
								a.getAccessObjectID(), a.getProperty());

					// System.out.println(sourceNode.getId()
					// + "("
					// + sourceNode.getIncoming().iterator().next()
					// .getStartNode()
					// .getOutgoingWithType("typeOf").iterator()
					// .next().getEndNode().getProperty("id")
					// + ") :: "
					// + a.getAccessObjectID()
					// + "("
					// + graph.getNodeById(a.getAccessObjectID())
					// .getOutgoingWithType("typeOf").iterator()
					// .next().getEndNode().getProperty("id")
					// + ") :: " + a.getProperty());
				}

				System.err.println("accesses: "
						+ accessListener.getAccesses().size() + " ("
						+ derivedAccessDictionary.query("*", "*").size()
						+ " nodes)");

				tx.success();
			}

			accessListener.resetAccesses();

		}

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			derivedLeft = ((IGraphIterable<IGraphNode>) derivedProxyDictionary
					.query("derived", "*")).size();
			tx.success();
		}

		System.out
				.println(derivedLeft
						+ " - sets of proxy [derived] attributes left incomplete in the store");

		return derivedLeft;
	}

	public void updateDerivedAttributes(IGraphChangeDescriptor ret, String type)
			throws Exception {

		HashSet<IGraphNode> nodesToBeUpdated = new HashSet<>();

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			IGraphNodeIndex derivedAccessDictionary = graph
					.getOrCreateNodeIndex("derivedaccessdictionary");

			for (IGraphChange c : ret.getChanges()) {

				if (!c.isTransient()) {

					// c.getElementType();
					// c.getIdentifier();

					if (c.getElementType().equals(IGraphChange.FILE)
							|| c.getElementType().equals(IGraphChange.INSTANCE)) {

						for (IGraphNode node : derivedAccessDictionary.query(
								c.getIdentifier(), "*")) {

							String derivedPropertyName = node.getIncoming()
									.iterator().next().getType();

							if (node.getPropertyKeys().contains(
									derivedPropertyName)) {

								node.setProperty(derivedPropertyName, "_NYD##"
										+ node.getProperty("derivationlogic"));

								nodesToBeUpdated.add(node);

							} else
								throw new Exception(
										"Exception in updateDerivedAttributes() -- derived attribute node did not contain property: "
												+ derivedPropertyName);
						}

					} else if (c.getElementType().equals(IGraphChange.PROPERTY)
							|| c.getElementType()
									.equals(IGraphChange.REFERENCE)) {

						String[] split = c.getIdentifier().split("::");
						String id = split[0];
						String prop = split[1];

						for (IGraphNode node : derivedAccessDictionary.query(
								id, prop)) {

							String derivedPropertyName = node.getIncoming()
									.iterator().next().getType();

							if (node.getPropertyKeys().contains(
									derivedPropertyName)) {

								node.setProperty(derivedPropertyName, "_NYD##"
										+ node.getProperty("derivationlogic"));

								nodesToBeUpdated.add(node);

							} else
								throw new Exception(
										"Exception in updateDerivedAttributes() -- derived attribute node did not contain property: "
												+ derivedPropertyName);
						}

					} else if (c.getElementType()
							.equals(IGraphChange.METAMODEL)
							|| c.getElementType().equals(IGraphChange.TYPE)) {

						// metamodel change do nothing

					} else
						throw new Exception(
								"Exception in updateDerivedAttributes() -- change of type: "
										+ c.getElementType());

				}
			}

			IQueryEngine q = indexer.getKnownQueryLanguages().get(type);
			//
			//
			IAccessListener accessListener = q.calculateDerivedAttributes(
					graph, nodesToBeUpdated);

			for (IAccess a : accessListener.getAccesses()) {

				IGraphNode sourceNode = graph
						.getNodeById(a.getSourceObjectID());

				if (sourceNode != null)
					derivedAccessDictionary.remove(sourceNode);

			}

			for (IAccess a : accessListener.getAccesses()) {

				IGraphNode sourceNode = graph
						.getNodeById(a.getSourceObjectID());

				if (sourceNode != null)
					derivedAccessDictionary.add(sourceNode,
							a.getAccessObjectID(), a.getProperty());
			}

			tx.success();

		} catch (Exception e) {
			throw e;
		}
	}

	private static Iterable<IGraphEdge> allNodesWithFile(final IGraphNode fileNode) {
		if (fileNode != null)
			return fileNode.getIncomingWithType("file");
		else
			return null;
	}

	private static IGraphNode getFileNode(IGraphDatabase graph,
			String repositoryURL, String file) {
		IGraphNodeIndex filedictionary = graph.getFileIndex();

		// Path path = Paths.get(file);
		// String relative = path.getFileName().toString();
		IGraphNode fileNode = null;

		try {

			fileNode = filedictionary.get(
					"id",
					repositoryURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
							+ file).getSingle();

		} catch (Exception e) {
			//
		}
		return fileNode;
	}

	public void updateDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic) {

		HashSet<IGraphNode> derivedPropertyNodes = new HashSet<>();

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...
			IGraphNode metamodelNode = graph.getMetamodelIndex()
					.get("id", metamodeluri).getSingle();

			IGraphNode typeNode = null;

			for (IGraphEdge e : metamodelNode.getIncomingWithType("epackage")) {
				IGraphNode othernode = e.getStartNode();
				if (othernode.getProperty("id").equals(typename)) {
					typeNode = othernode;
					break;
				}
			}

			HashSet<IGraphNode> nodes = new HashSet<>();
			for (IGraphEdge e : typeNode.getIncomingWithType("typeOf")) {
				nodes.add(e.getStartNode());
			}
			for (IGraphEdge e : typeNode.getIncomingWithType("KindOf")) {
				nodes.add(e.getStartNode());
			}

			for (IGraphNode instanceNode : nodes) {

				Iterator<IGraphEdge> derived = instanceNode
						.getOutgoingWithType(attributename).iterator();

				HashMap<String, Object> m = new HashMap<>();
				m.put("isMany", isMany);
				m.put("isOrdered", isOrdered);
				m.put("isUnique", isUnique);
				m.put("attributetype", attributetype);
				m.put("derivationlanguage", derivationlanguage);
				m.put("derivationlogic", derivationlogic);
				m.put(attributename, "_NYD##" + derivationlogic);

				// derived node exists -- update derived property
				if (derived.hasNext()) {

					IGraphNode derivedPropertyNode = derived.next()
							.getEndNode();

					for (String s : m.keySet())
						derivedPropertyNode.setProperty(s, m.get(s));

					derivedPropertyNodes.add(derivedPropertyNode);

				} else// derived node does not exist -- create derived property
				{

					IGraphNode derivedPropertyNode = graph.createNode(m,
							"derivedattribute");

					m.clear();
					m.put("isDerived", true);

					graph.createRelationship(instanceNode, derivedPropertyNode,
							attributename, m);

					derivedPropertyNodes.add(derivedPropertyNode);

				}

			}

			// derive the new property
			IQueryEngine q = indexer.getKnownQueryLanguages().get(
					derivationlanguage);
			//
			//
			IAccessListener accessListener = q.calculateDerivedAttributes(
					graph, derivedPropertyNodes);

			IGraphNodeIndex derivedAccessDictionary = graph
					.getOrCreateNodeIndex("derivedaccessdictionary");

			for (IAccess a : accessListener.getAccesses()) {

				IGraphNode sourceNode = graph
						.getNodeById(a.getSourceObjectID());

				if (sourceNode != null)
					derivedAccessDictionary.remove(sourceNode);

			}

			for (IAccess a : accessListener.getAccesses()) {

				IGraphNode sourceNode = graph
						.getNodeById(a.getSourceObjectID());

				if (sourceNode != null)
					derivedAccessDictionary.add(sourceNode,
							a.getAccessObjectID(), a.getProperty());
			}

			tx.success();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void updateIndexedAttribute(String metamodeluri, String typename,
			String attributename) {

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			IGraphNodeIndex i = graph.getOrCreateNodeIndex(metamodeluri + "##"
					+ typename + "##" + attributename);

			IGraphNode typeNode = null;

			for (IGraphEdge r : graph.getMetamodelIndex()
					.get("id", metamodeluri).getSingle()
					.getIncomingWithType("epackage")) {

				IGraphNode othernode = r.getStartNode();
				if (othernode.getProperty("id").equals(typename)) {
					typeNode = othernode;
					break;
				}

			}

			// a isMany isOrdered isUnique attrType isIndexed
			String[] metadata = (String[]) typeNode.getProperty(attributename);

			boolean isPrimitiveOrWrapperType = false;
			Class<?> c = String.class;

			try {
				c = Class.forName(metadata[4]);
				isPrimitiveOrWrapperType = new GraphUtil()
						.isPrimitiveOrWrapperType(c);
			} catch (Exception e) {
				//
				e.printStackTrace();
			}

			HashSet<IGraphNode> nodes = new HashSet<>();
			for (IGraphEdge e : typeNode.getIncomingWithType("typeOf")) {
				nodes.add(e.getStartNode());
			}
			for (IGraphEdge e : typeNode.getIncomingWithType("KindOf")) {
				nodes.add(e.getStartNode());
			}

			for (IGraphNode node : nodes) {

				Map<String, Object> m = new HashMap<>();

				if (!(metadata[1] == "t")) {

					if (isPrimitiveOrWrapperType)
						m.put(attributename, node.getProperty(attributename));

					else
						m.put(attributename, node.getProperty(attributename)
								.toString());

				}

				else {

					Collection<Object> collection = null;

					if (metadata[2] == "t" && metadata[3] == "t")
						collection = new LinkedHashSet<Object>();
					else if (metadata[2] == "t")
						collection = new LinkedList<Object>();
					else if (metadata[3] == "t")
						collection = new HashSet<Object>();
					else
						collection = new LinkedList<Object>();

					for (Object o : (Collection<?>) node
							.getProperty(attributename)) {

						if (isPrimitiveOrWrapperType)
							collection.add(o);

						else
							collection.add(o.toString());

					}

					Object r = Array.newInstance(c, 1);

					Object ret = collection.toArray((Object[]) r);

					m.put(attributename, ret);

				}

				i.add(node, m);

			}

			tx.success();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

// end
