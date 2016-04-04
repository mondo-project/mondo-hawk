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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphDatabase.Mode;
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
import org.hawk.graph.FileNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.internal.util.GraphUtil;

public class GraphModelBatchInjector {

	public static final String FRAGMENT_DICT_NAME = "fragmentdictionary";
	public static final String ROOT_DICT_FILE_KEY = "file";
	public static final String ROOT_DICT_NAME = "rootdictionary";

	public static final String PROXY_DICT_NAME = "proxydictionary";
	public static final String DERIVED_PROXY_DICT_NAME = "derivedproxydictionary";

	// integer array containing the current number of added elements:
	// (element,((ofType)M->MM)reference,((ofKind)M->MM)reference,(unset(M->M))reference)
	private int[] objectCount = { 0, 0, 0, 0 };
	private int unset;

	private String repoURL;
	private Set<String> prefixesToStrip = new HashSet<>();

	private static enum ParseOptions {
		MODELELEMENTS, MODELREFERENCES
	};

	private IGraphDatabase graph;

	private final Map<IHawkObject, IGraphNode> hash = new HashMap<IHawkObject, IGraphNode>(8192);

	IGraphNodeIndex fileDictionary, proxyDictionary, rootDictionary, fragmentIdx,
			derivedProxyDictionary;

	long startTime;

	private final IGraphChangeListener listener;
	private final VcsCommitItem commitItem;

	private Mode previousMode = Mode.UNKNOWN;

	private void refreshIndexes() throws Exception {

		// only do it if db state changed to avoid overhead
		Mode currentMode = graph.currentMode();

		if (previousMode != currentMode) {

			previousMode = currentMode;

			if (graph.currentMode().equals(Mode.TX_MODE)) {

				try (IGraphTransaction t = graph.beginTransaction()) {
					refreshIx();
					t.success();
				}

			} else {
				refreshIx();
			}

		}

	}

	private void refreshIx() {
		fileDictionary = graph.getFileIndex();
		proxyDictionary = graph.getOrCreateNodeIndex(PROXY_DICT_NAME);
		rootDictionary = graph.getOrCreateNodeIndex(ROOT_DICT_NAME);
		fragmentIdx = graph.getOrCreateNodeIndex(FRAGMENT_DICT_NAME);
		derivedProxyDictionary = graph.getOrCreateNodeIndex(DERIVED_PROXY_DICT_NAME);
	}

	public GraphModelBatchInjector(IGraphDatabase g, TypeCache typeCache, VcsCommitItem s, IGraphChangeListener listener) throws Exception {
		this.graph = g;
		this.typeCache = typeCache;
		this.commitItem = s;
		this.listener = listener;

		if (s != null) {
			final IVcsManager vcsManager = s.getCommit().getDelta().getManager();
			prefixesToStrip.addAll(vcsManager.getPrefixesToBeStripped());
		}
		prefixesToStrip.add(new File(g.getTempDir()).toURI().toString());

		refreshIndexes();
	}

	public GraphModelBatchInjector(IModelIndexer hawk, TypeCache typeCache, VcsCommitItem s, IHawkModelResource r,
			IGraphChangeListener listener) throws Exception {
		IGraphDatabase g = hawk.getGraph();
		this.graph = g;
		this.typeCache = typeCache;
		this.commitItem = s;
		this.listener = listener;

		final IVcsManager vcsManager = s.getCommit().getDelta().getManager();
		prefixesToStrip.addAll(vcsManager.getPrefixesToBeStripped());
		prefixesToStrip.add(new File(g.getTempDir()).toURI().toString());

		startTime = System.nanoTime();
		graph.enterBatchMode();

		try {
			listener.changeStart();

			refreshIndexes();

			boolean isNew = false;

			repoURL = s.getCommit().getDelta().getManager().getLocation();
			IGraphNode fileNode = null;
			long filerevision = 0L;
			try {
				fileNode = ((IGraphIterable<IGraphNode>) fileDictionary.get("id",
						repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + s.getPath())).getSingle();
				if (fileNode != null)
					filerevision = (Long) fileNode.getProperty("revision");
			} catch (Exception e) {
			}
			if (filerevision == 0L)
				isNew = true;

			if (isNew) {
				// add file
				if (fileNode == null) {
					fileNode = addFileNode(s, listener);
				}

				try {

					// add model elements
					Set<IHawkObject> children = r.getAllContentsSet();

					startTime = System.nanoTime();

					System.out.println("File: " + s.getPath());
					System.out.print("ADDING: ");
					int[] addedElements = parseResource(fileNode, ParseOptions.MODELELEMENTS, children, hawk,
							r.providesSingletonElements());
					System.out.println(addedElements[0] + "\nNODES AND " + addedElements[1] + " + " + addedElements[2]
							+ " M->MM REFERENCES! (took ~" + (System.nanoTime() - startTime) / 1000000000 + "sec)");

					startTime = System.nanoTime();

					// add references
					System.out.println("File: " + s.getPath());
					System.out.print("ADDING: ");
					addedElements = parseResource(fileNode, ParseOptions.MODELREFERENCES, children, hawk,
							r.providesSingletonElements());
					setUnset(getUnset() + addedElements[3]);
					System.out.println(addedElements[0] + "\nREFERENCES! (took ~"
							+ (System.nanoTime() - startTime) / 1000000000 + "sec)");

					// System.out
					// .println(((IGraphIterable<IGraphNode>) proxyDictionary
					// .query(GraphModelUpdater.PROXY_REFERENCE_PREFIX,
					// "*")).size()
					// + " - sets of proxy references left in the store");
					listener.changeSuccess();
					successState = true;
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("ParseMResource Exception on file: " + s.getPath()
							+ "\nReverting all changes on that file.");
					//
					// graph.exitBatchMode();
					//

					IGraphNode n = new Utils().getFileNodeFromVCSCommitItem(graph, s);
					if (n != null) {

						try (IGraphTransaction t = g.beginTransaction()) {
							new DeletionUtils(graph).deleteAll(n, s, listener);
							t.success();

						} catch (Exception e2) {
							System.err.println("error in reverting from erroneous batch insert: " + e2.getCause());

						}
					}
					listener.changeFailure();
					successState = false;
				}
			} else /* if not new */ {
				System.err.println("warning: GraphModelBatchInjector used with a model already in Hawk.");
				listener.changeSuccess();
				successState = true;
			}
		} catch (Exception ex) {
			successState = false;
			ex.printStackTrace();
			listener.changeFailure();
		} finally {
			// graph.exitBatchMode();
			// System.err.println("INDEXERDEBUG: " + debug / 1000 + "s " + debug
			// % 1000 + "ms");
		}
	}

	private IGraphNode addFileNode(VcsCommitItem s, IGraphChangeListener listener) {
		IGraphNode fileNode;
		Map<String, Object> mapForFileNode = new HashMap<>();
		mapForFileNode.put(IModelIndexer.IDENTIFIER_PROPERTY, s.getPath());
		mapForFileNode.put("revision", s.getCommit().getRevision());
		mapForFileNode.put(FileNode.PROP_REPOSITORY, repoURL);

		// System.err.println("creating file node: "+s.getPath());
		fileNode = graph.createNode(mapForFileNode, "file");

		Map<String, Object> mapForDictionary = new HashMap<>();
		mapForDictionary.put("id", repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + s.getPath());
		fileDictionary.add(fileNode, mapForDictionary);

		// propagate changes to listeners
		listener.fileAddition(s, fileNode);
		return fileNode;
	}

	/**
	 * Adds the resource to the graph according to whether it is a model or a
	 * metamodel resource
	 * 
	 * @param originatingFile
	 * 
	 * @param parseOption
	 * @param resource
	 * @param graph
	 * @return
	 */
	private int[] parseResource(IGraphNode originatingFile, ParseOptions parseOption, Set<IHawkObject> children,
			IModelIndexer hawk, boolean resourceCanProvideSingletons) throws Exception {

		graph.enterBatchMode();

		objectCount[0] = 0;
		objectCount[1] = 0;
		objectCount[2] = 0;
		objectCount[3] = 0;

		final String fileID = originatingFile.getProperty(IModelIndexer.IDENTIFIER_PROPERTY) + "";
		long init = System.nanoTime();
		int lastprint = 0;

		for (IHawkObject child : children) {
			switch (parseOption) {
			case MODELELEMENTS:
				addEObject(originatingFile, child, resourceCanProvideSingletons);
				break;
			case MODELREFERENCES:
				addEReferences(child, resourceCanProvideSingletons);
				break;
			default:
				System.err.println("parse option: " + parseOption + " not recognised!");
			}

			if (lastprint < objectCount[0] - 50000) {
				lastprint = objectCount[0];

				final String out = String.format("Adding %s: %d %d sec (%d sec total) to %s",
						parseOption == ParseOptions.MODELELEMENTS ? "nodes" : "references", objectCount[0],
						(System.nanoTime() - init) / 1_000_000_000, (System.nanoTime() - startTime) / 1_000_000_000,
						fileID);

				hawk.getCompositeStateListener().info(out);
				init = System.nanoTime();
			}
		}

		return objectCount;
	}

	/**
	 * 
	 * @param eObject
	 * @return the URI ID of an eObject
	 */
	private String getEObjectId(IHawkObject eObject) {
		return eObject.getUriFragment();
	}

	/**
	 * Creates a node in the graph database with the given eObject's attributes
	 * in it. Also indexes it in the 'dictionary' index.
	 * 
	 * @param eObject
	 * @return the Node
	 * @throws Exception
	 */
	private IGraphNode createEObjectNode(IHawkObject eObject, IGraphNode typenode) throws Exception {
		IGraphNode node = null;

		try {

			String eObjectId = getEObjectId(eObject);
			HashMap<String, Object> m = new HashMap<>();
			m.put(IModelIndexer.IDENTIFIER_PROPERTY, eObjectId);
			m.put(IModelIndexer.SIGNATURE_PROPERTY, eObject.signature());

			final List<IHawkAttribute> normalattributes = new LinkedList<IHawkAttribute>();
			final List<IHawkAttribute> indexedattributes = new LinkedList<IHawkAttribute>();

			for (final IHawkAttribute eAttribute : ((IHawkClass) eObject.getType()).getAllAttributes()) {
				if (eObject.isSet(eAttribute)) {
					final Map<String, Object> hashedProperties = typeCache.getEClassNodeProperties(graph, eObject.getType());
					final String[] attributeProperties = (String[]) hashedProperties.get(eAttribute.getName());

					final boolean isIndexed = attributeProperties[5].equals("t");
					if (isIndexed) {
						indexedattributes.add(eAttribute);
					}

					normalattributes.add(eAttribute);

				} else
				// deprecatedTODO currently unset items are not included to may
				// crash eol etc
				{
					// node.setProperty(eAttribute.getName(), "UNSET");
				}
			}

			for (IHawkAttribute a : normalattributes) {
				final Object value = eObject.get(a);

				if (!a.isMany()) {
					final Class<?> valueClass = value.getClass();
					if (new GraphUtil().isPrimitiveOrWrapperType(valueClass)) {
						m.put(a.getName(), value);
					} else {
						m.put(a.getName(), value.toString());
					}
				} else {
					Collection<Object> collection = null;

					if (a.isUnique())
						collection = new LinkedHashSet<Object>();
					else
						collection = new LinkedList<Object>();

					final Collection<?> srcCollection = (Collection<?>) value;
					Class<?> elemClass = null;
					boolean primitiveOrWrapperClass = false;
					if (!srcCollection.isEmpty()) {
						final Object first = srcCollection.iterator().next();
						elemClass = first.getClass();
						primitiveOrWrapperClass = new GraphUtil().isPrimitiveOrWrapperType(elemClass);
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
						r = Array.newInstance(elemClass, collection.size());
					} else {
						r = Array.newInstance(String.class, collection.size());
					}
					Object ret = collection.toArray((Object[]) r);

					m.put(a.getName(), ret);
				}
			}

			try {
				node = graph.createNode(m, "eobject");
				if (eObject.isFragmentUnique()) {
					fragmentIdx.add(node, "id", eObject.getUriFragment());
					fragmentIdx.flush();
				}
			} catch (IllegalArgumentException ex) {
				System.err.println("here be dragons!");
			} catch (Throwable e) {
				e.printStackTrace();
			}

			// propagate changes to listeners
			listener.modelElementAddition(commitItem, eObject, node, false);
			for (String s : m.keySet()) {
				Object value = m.get(s);
				listener.modelElementAttributeUpdate(commitItem, eObject, s, null, value, node,
						ModelElementNode.TRANSIENT_ATTRIBUTES.contains(s));
			}

			// add derived attrs
			final Map<String, Object> hashed = typeCache.getEClassNodeProperties(graph, eObject.getType());
			final Set<String> attributekeys = hashed.keySet();

			for (String attributekey : attributekeys) {
				final Object attr = hashed.get(attributekey);

				if (attr instanceof String[]) {

					String[] metadata = (String[]) attr;

					if (metadata[0].equals("d")) {
						m.clear();
						m.put("isMany", metadata[1]);
						m.put("isOrdered", metadata[2]);
						m.put("isUnique", metadata[3]);
						m.put("attributetype", metadata[4]);
						m.put("derivationlanguage", metadata[5]);
						m.put("derivationlogic", metadata[6]);
						m.put(attributekey, "_NYD##" + metadata[6]);

						IGraphNode derivedattributenode = graph.createNode(m, "derivedattribute");

						m.clear();
						m.put("isDerived", true);

						graph.createRelationship(node, derivedattributenode, attributekey, m);
						addToProxyAttributes(derivedattributenode);

					}

				}

			}

			for (IHawkAttribute a : indexedattributes) {
				IGraphNodeIndex i = graph.getOrCreateNodeIndex(
						eObject.getType().getPackageNSURI() + "##" + eObject.getType().getName() + "##" + a.getName());

				m.clear();
				// graph.setNodeProperty(node,"value",
				// eObject.eGet(a).toString());

				final Object v = eObject.get(a);

				if (!a.isMany()) {

					if (new GraphUtil().isPrimitiveOrWrapperType(v.getClass()))
						m.put(a.getName(), v);

					else
						m.put(a.getName(), v.toString());

				}

				else {

					Collection<Object> collection = null;

					if (a.isUnique())
						collection = new LinkedHashSet<Object>();
					else
						collection = new LinkedList<Object>();

					final Collection<?> srcCollection = (Collection<?>) v;
					Class<?> elemClass = null;
					boolean primitiveOrWrapperClass = false;
					if (!srcCollection.isEmpty()) {
						final Object first = srcCollection.iterator().next();
						elemClass = first.getClass();
						primitiveOrWrapperClass = new GraphUtil().isPrimitiveOrWrapperType(elemClass);
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

					m.put(a.getName(), ret);

				}

				i.add(node, m);
			}

		}

		catch (Exception e) {
			System.err.println("GraphModelBatchInjector: createEobjectNode: error in inserting attributes: ");
			e.printStackTrace();
		}

		return node;
	}

	private void addToProxyAttributes(IGraphNode node) {

		Map<String, Object> m = new HashMap<>();
		m.put("derived", "_");

		derivedProxyDictionary.add(node, m);

	}

	private final TypeCache typeCache;
	private boolean successState = true;

	/**
	 * Creates a node with the eObject, adds it to the hash and adds it the the
	 * appropriate eClass in the metatracker collection
	 * 
	 * @param originatingFile
	 * 
	 * @param eObject
	 * @return
	 * @throws Exception
	 */
	protected IGraphNode addEObject(IGraphNode originatingFile, IHawkObject eObject,
			boolean resourceCanProvideSingletons) throws Exception {

		refreshIndexes();

		IGraphNode eClass = typeCache.getEClassNode(graph, eObject.getType());

		IGraphNode node = null;

		// if a unique element is already in hawk set the node to that element
		// and return it
		if (resourceCanProvideSingletons && eObject.isFragmentUnique()) {
			node = getFromFragmentIndex(eObject);

			// if this node is from a file not yet registered, add this file
			// reference
			if (node != null && originatingFile != null) {
				boolean found = false;
				for (IGraphEdge e : node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE))
					if (e.getEndNode().equals(originatingFile))
						found = true;
				if (!found)
					createReference(ModelElementNode.EDGE_LABEL_FILE, node, originatingFile,
							new HashMap<String, Object>(), true);
			}

		}

		if (node == null) {

			node = createEObjectNode(eObject, eClass);

			if (node == null) {
				System.err.println(String.format("The node for (%s) is null", eObject));
			} else {
				hash.put(eObject, node);

				final HashMap<String, Object> emptyMap = new HashMap<String, Object>();
				createReference(ModelElementNode.EDGE_LABEL_OFTYPE, node, eClass, emptyMap, true);
				if (originatingFile != null) {
					createReference(ModelElementNode.EDGE_LABEL_FILE, node, originatingFile, emptyMap, true);
				}
				objectCount[1]++;

				// use metamodel to infer all supertypes for fast search and log
				// em
				for (IHawkClass superType : ((IHawkClass) eObject.getType()).getSuperTypes()) {
					eClass = typeCache.getEClassNode(graph, superType);
					createReference(ModelElementNode.EDGE_LABEL_OFKIND, node, eClass, emptyMap, true);
					objectCount[2]++;
				}

				objectCount[0]++;

				if (eObject.isRoot()) {
					rootDictionary.add(node, ROOT_DICT_FILE_KEY, originatingFile.getId().toString());
				}
			}

		}

		return node;
	}

	protected IGraphNode getFromFragmentIndex(IHawkObject eObject) {
		IGraphNode node = null;

		final Iterator<IGraphNode> itr = fragmentIdx.get("id", eObject.getUriFragment()).iterator();
		while (itr.hasNext()) {
			if (node == null)
				node = itr.next();
			else {
				System.err.println(
						"WARNING: GraphModelBatchInjector: addEObject: isFragmentUnique returned more than one node, keeping first one.");
				break;
			}
		}

		return node;
	}

	/**
	 * Creates an edge with the parameters given and links it to the appropriate
	 * nodes
	 * 
	 * @param from
	 * @param to
	 * @param edgelabel
	 * @throws Exception
	 */
	private void addEdge(IHawkObject from, IHawkObject to, final String edgelabel, boolean isContainment,
			boolean isContainer) throws Exception {

		IGraphNode source = null;
		IGraphNode destination = null;

		source = hash.get(from);
		destination = hash.get(to);

		if (source == null && destination == null) {

			System.err.println("hash error 1, not found from (class: " + (from).getType().getName()
					+ ") and to (class: " + ((IHawkObject) to).getType().getName() + ") on reference: " + edgelabel
					+ " source = " + source + " destination = " + destination);

		}

		else if (source == null) {

			System.err.println("hash error 2, not found from (class: " + (from).getType().getName()
					+ ") and to (class: " + ((IHawkObject) to).getType().getName() + ") on reference: " + edgelabel
					+ " source = " + source + " destination = " + destination);

		} else if (destination == null) {
			// the modelling technology managed to resolve a cross-file proxy
			// early (before it was inserted into hawk -- handle it like any
			// other proxy)

			addProxyRef(from, to, edgelabel, isContainment, isContainer);
		} else {

			HashMap<String, Object> props = new HashMap<String, Object>();

			if (isContainment)
				props.put(ModelElementNode.EDGE_PROPERTY_CONTAINMENT, "true");
			if (isContainer)
				props.put(ModelElementNode.EDGE_PROPERTY_CONTAINER, "true");

			createReference(edgelabel, source, destination, props, false);

			objectCount[0]++;
		}

	}

	private void createReference(final String edgelabel, IGraphNode source, IGraphNode destination,
			Map<String, Object> props, boolean isTransient) {
		graph.createRelationship(source, destination, edgelabel, props);
		listener.referenceAddition(commitItem, source, destination, edgelabel, isTransient);
	}

	/**
	 * Iterates through all of the references the eObject has and inserts them
	 * into the graph -- not using hash -- for transactional update
	 * 
	 * @param source
	 * @param addedNodesHash
	 * 
	 * @param eObject
	 * @return
	 * @throws Exception
	 */
	protected boolean addEReferences(IGraphNode fileNode, IGraphNode node, IHawkObject source,
			Map<String, IGraphNode> addedNodesHash, Map<String, IGraphNode> nodes) throws Exception {

		refreshIndexes();

		boolean ret = true;
		try {
			for (final IHawkReference eReference : ((IHawkClass) source.getType()).getAllReferences()) {

				if (source.isSet(eReference)) {

					String edgelabel = eReference.getName();

					Object destinationObject = source.get(eReference, false);

					if (destinationObject instanceof Iterable<?>) {

						for (Object destinationEObject : ((Iterable<?>) destinationObject)) {

							final IHawkObject destinationHawkObject = (IHawkObject) destinationEObject;
							if (!destinationHawkObject.isInDifferentResourceThan(source)) {
								IGraphNode dest = null;
								dest = addedNodesHash.get(destinationHawkObject.getUriFragment());
								if (dest == null)
									dest = nodes.get(destinationHawkObject.getUriFragment());

								Map<String, Object> props = new HashMap<String, Object>();
								if (eReference.isContainment()) {
									props.put(ModelElementNode.EDGE_PROPERTY_CONTAINMENT, "true");
								}
								if (eReference.isContainer()) {
									props.put(ModelElementNode.EDGE_PROPERTY_CONTAINER, "true");
								}

								createReference(edgelabel, node, dest, props, false);
							} else {
								addProxyRef(node, destinationHawkObject, edgelabel, eReference.isContainment(),
										eReference.isContainer());

								// System.err
								// .println("adding proxy [iterable] reference
								// ("
								// + edgelabel
								// + " | "
								// + destinationHawkObject.getUri()
								// + ")... "
								// + (addProxyRef ? "done" : "failed"));
							}
						}

					} else {

						final IHawkObject destinationHawkObject = (IHawkObject) destinationObject;
						if (!destinationHawkObject.isInDifferentResourceThan(source)) {
							IGraphNode dest = addedNodesHash.get(destinationHawkObject.getUriFragment());
							if (dest == null)
								dest = nodes.get(destinationHawkObject.getUriFragment());

							Map<String, Object> props = new HashMap<String, Object>();

							if (eReference.isContainment()) {
								props.put(ModelElementNode.EDGE_PROPERTY_CONTAINMENT, "true");
							}
							if (eReference.isContainer()) {
								props.put(ModelElementNode.EDGE_PROPERTY_CONTAINER, "true");
							}

							createReference(edgelabel, node, dest, props, false);
						} else {
							addProxyRef(node, destinationHawkObject, edgelabel, eReference.isContainment(),
									eReference.isContainer());
						}
					}

				}

			}
		} catch (Exception e) {
			System.err.println(
					"Error in: addEReference(IGraphNode node, IHawkObject object,	HashMap<String, IGraphNode> nodes):");
			e.printStackTrace();
			ret = false;
		}

		return ret;

	}

	/**
	 * Iterates through all of the references the eObject has and inserts them
	 * into the graph -- for batch updates
	 * 
	 * @param originatingFile
	 * 
	 * @param source
	 * @throws Exception
	 */
	private boolean addEReferences(IHawkObject source, boolean resourceCanProvideSingletons) throws Exception {
		boolean atLeastOneSetReference = false;
		if (source.isFragmentUnique() && resourceCanProvideSingletons && hash.get(source) == null) {
			// Avoid trying to add references from a singleton object we already had
			return atLeastOneSetReference;
		}

		for (final IHawkReference eReference : ((IHawkClass) source.getType()).getAllReferences()) {
			if (source.isSet(eReference)) {
				atLeastOneSetReference = true;

				final String edgelabel = eReference.getName();
				final Object destinationObject = source.get(eReference, false);

				if (destinationObject instanceof Iterable<?>) {
					for (Object destinationEObject : ((Iterable<?>) destinationObject)) {
						final IHawkObject destinationHawkObject = (IHawkObject) destinationEObject;
						if (!destinationHawkObject.isInDifferentResourceThan(source)) {
							addEdge(source, destinationHawkObject, edgelabel, eReference.isContainment(),
									eReference.isContainer());
						} else {
							addProxyRef(source, destinationHawkObject, edgelabel, eReference.isContainment(),
									eReference.isContainer());
							// System.err
							// .println("adding proxy [iterable] reference ("
							// + edgelabel
							// + " | "
							// + ((IHawkObject) destinationHawkObject).getUri()
							// + ")... "
							// + (added ? "done" : "failed"));
						}
					}
				} else /* if destination is not iterable */ {
					final IHawkObject destinationHawkObject = (IHawkObject) destinationObject;
					if (!destinationHawkObject.isInDifferentResourceThan(source)) {
						addEdge(source, destinationHawkObject, edgelabel, eReference.isContainment(),
								eReference.isContainer());
					} else {
						addProxyRef(source, destinationHawkObject, edgelabel, eReference.isContainment(),
								eReference.isContainer());
					}
				}

			} else /* if reference is not set */ {
				objectCount[3]++;
			}
		}

		return atLeastOneSetReference;
	}

	private boolean addProxyRef(IHawkObject from, IHawkObject destinationObject, String edgelabel,
			boolean isContainment, boolean isContainer) {
		IGraphNode withProxy = hash.get(from);
		return addProxyRef(withProxy, destinationObject, edgelabel, isContainment, isContainer);
	}

	private boolean addProxyRef(IGraphNode node, IHawkObject destinationObject, String edgelabel, boolean isContainment,
			boolean isContainer) {

		try {
			String uri = destinationObject.getUri();

			String destinationObjectRelativePathURI = uri;
			if (destinationObject.isFragmentUnique()) {
				/*
				 * In this scenario, we don't care about the file anymore: we
				 * need to flag it properly for the later resolution.
				 */
				destinationObjectRelativePathURI = GraphModelUpdater.PROXY_FILE_WILDCARD + "#"
						+ destinationObjectRelativePathURI.substring(destinationObjectRelativePathURI.indexOf("#") + 1);
			} else if (!destinationObject.URIIsRelative()) {
				destinationObjectRelativePathURI = new Utils().makeRelative(prefixesToStrip,
						destinationObjectRelativePathURI);
			}

			String destinationObjectRelativeFileURI = destinationObjectRelativePathURI;
			destinationObjectRelativeFileURI = destinationObjectRelativePathURI.substring(0,
					destinationObjectRelativePathURI.indexOf("#"));

			String destinationObjectFullPathURI = repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
					+ destinationObjectRelativePathURI;

			String destinationObjectFullFileURI = repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
					+ destinationObjectRelativeFileURI;

			Object proxies = null;
			// if
			// (withProxy.hasProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX +
			// relativeFileURI)) {
			// proxies =
			// withProxy.getProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX +
			// relativeFileURI);
			// }
			// System.err.println(">>>>>>>"+relativeFileURI);

			proxies = node.getProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX + destinationObjectFullFileURI);
			proxies = new Utils().addToElementProxies((String[]) proxies, destinationObjectFullPathURI, edgelabel,
					isContainment, isContainer);

			node.setProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX + destinationObjectFullFileURI, proxies);

			HashMap<String, Object> m = new HashMap<>();
			m.put(GraphModelUpdater.PROXY_REFERENCE_PREFIX, destinationObjectFullFileURI);

			proxyDictionary.add(node, m);

		} catch (Exception e) {
			System.err.println("proxydictionary error:");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	protected boolean resolveProxyRef(IGraphNode source, IGraphNode target, String edgeLabel, boolean isContainment,
			boolean isContainer) throws Exception {

		refreshIndexes();

		boolean found = false;

		for (IGraphEdge e : source.getOutgoingWithType(edgeLabel))
			if (e.getEndNode().getId().equals(target.getId())) {
				found = true;
				break;
			}

		if (found)
			return false;
		else {
			IGraphEdge rel = graph.createRelationship(source, target, edgeLabel, new HashMap<String, Object>());
			if (isContainment) {
				rel.setProperty(ModelElementNode.EDGE_PROPERTY_CONTAINMENT, "true");
			} else if (isContainer) {
				rel.setProperty(ModelElementNode.EDGE_PROPERTY_CONTAINER, "true");
			}
			return true;
		}
	}

	public int getUnset() {
		return unset;
	}

	private void setUnset(int unset) {
		this.unset = unset;
	}

	public boolean getSuccess() {
		return successState;
	}

}
