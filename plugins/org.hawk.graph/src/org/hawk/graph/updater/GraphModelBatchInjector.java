/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 ******************************************************************************/
package org.hawk.graph.updater;

import java.io.File;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.graph.FileNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;
import org.hawk.graph.util.GraphUtil;
import org.hawk.graph.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphModelBatchInjector {
	private static final Logger LOGGER = LoggerFactory.getLogger(GraphModelBatchInjector.class);

	public static final String FRAGMENT_DICT_NAME = "fragmentdictionary";
	public static final String ROOT_DICT_FILE_KEY = FileNode.FILE_NODE_LABEL;
	public static final String ROOT_DICT_NAME = "rootdictionary";

	public static final String PROXY_DICT_NAME = "proxydictionary";
	public static final String DERIVED_PROXY_DICT_NAME = "derivedproxydictionary";

	// integer array containing the current number of added elements:
	// (element,((ofType)M->MM)reference,((ofKind)M->MM)reference,(unset(M->M))reference)
	private int[] objectCount = { 0, 0, 0, 0 };
	private int unset;

	private String repoURL;

	private static enum ParseOptions {
		MODELELEMENTS, MODELREFERENCES
	};

	private IGraphDatabase graph;

	/*
	 * We don't keep the original objects, only the URIs. We split the URIs into path + fragment so
	 * we can use -XX:+UseStringDeduplication (new in Java 8u20) to save memory, and the underlying
	 * IGraphNodes only have identifiers (for further memory savings).
	 */
	private final Map<Pair<String, String>, IGraphNode> hash = new HashMap<>();

	IGraphNodeIndex fileDictionary, proxyDictionary, rootDictionary, fragmentIdx,
			derivedProxyDictionary;

	long startTime;

	private final IGraphChangeListener listener;
	private final VcsCommitItem commitItem;
	private final String tempDirURI;
	private Mode previousMode = Mode.UNKNOWN;

	private final TypeCache typeCache;

	private boolean successState = true;

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
		this.tempDirURI = new File(g.getTempDir()).toURI().toString();

		refreshIndexes();
	}

	public GraphModelBatchInjector(IModelIndexer hawk, Supplier<DeletionUtils> deletionUtils, TypeCache typeCache, VcsCommitItem s, IHawkModelResource r, IGraphChangeListener listener, boolean verbose) throws Exception {

		IGraphDatabase g = hawk.getGraph();
		this.graph = g;
		this.typeCache = typeCache;
		this.commitItem = s;
		this.listener = listener;
		this.tempDirURI = new File(g.getTempDir()).toURI().toString();

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
					Iterable<IHawkObject> children = r.getAllContents();
					startTime = System.nanoTime();
					if (verbose) {
						LOGGER.debug("Adding elements of file {}", s.getPath());
					}

					int[] addedElements = parseResource(fileNode, ParseOptions.MODELELEMENTS, children, hawk, r.providesSingletonElements());
					if (verbose) {
						LOGGER.debug("{} NODES AND {} M->MM REFERENCES! (took ~{}sec)",
							addedElements[0], addedElements[1], addedElements[2],
							(System.nanoTime() - startTime) / 1_000_000_000
						);
					}

					// add references
					startTime = System.nanoTime();
					if (verbose) {
						LOGGER.debug("Adding edges of file {}", s.getPath());
					}
					addedElements = parseResource(fileNode, ParseOptions.MODELREFERENCES, children, hawk,
							r.providesSingletonElements());
					setUnset(getUnset() + addedElements[3]);
					if (verbose) {
						LOGGER.debug("{} REFERENCES! (took ~{} sec)", addedElements[0], (System.nanoTime() - startTime) / 1_000_000_000);
					}

					listener.changeSuccess();
					successState = true;
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);

					IGraphNode n = new Utils().getFileNodeFromVCSCommitItem(graph, s);
					if (n != null) {

						try (IGraphTransaction t = g.beginTransaction()) {
							deletionUtils.get().deleteAll(n, s, listener);
							t.success();

						} catch (Exception e2) {
							LOGGER.error("error in reverting from erroneous batch insert", e2);
						}
					}
					listener.changeFailure();
					successState = false;
				}
			} else /* if not new */ {
				LOGGER.warn("GraphModelBatchInjector used with a model already in Hawk.");
				listener.changeSuccess();
				successState = true;
			}
		} catch (Exception ex) {
			successState = false;
			LOGGER.error(ex.getMessage(), ex);
			listener.changeFailure();
		}
	}

	private IGraphNode addFileNode(VcsCommitItem s, IGraphChangeListener listener) {
		IGraphNode fileNode;
		Map<String, Object> mapForFileNode = new HashMap<>();
		mapForFileNode.put(IModelIndexer.IDENTIFIER_PROPERTY, s.getPath());
		mapForFileNode.put("revision", s.getCommit().getRevision());
		mapForFileNode.put(FileNode.PROP_REPOSITORY, repoURL);

		fileNode = graph.createNode(mapForFileNode, FileNode.FILE_NODE_LABEL);

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
	private int[] parseResource(IGraphNode originatingFile, ParseOptions parseOption, Iterable<IHawkObject> children,
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
				LOGGER.error("parse option {} not recognised!", parseOption);
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
		try {
			final List<IHawkAttribute> normalAttributes = new ArrayList<IHawkAttribute>();
			final List<IHawkAttribute> indexedAttributes = new ArrayList<IHawkAttribute>();
			IGraphNode node = createBasicEObjectNode(eObject, normalAttributes, indexedAttributes);

			// Derived features and indexed attributes
			final IHawkClassifier classifier = eObject.getType();
			addDerivedFeatureNodes(node, typenode, classifier.getPackageNSURI(), typeCache.getEClassNodeSlots(graph, classifier));
			for (IHawkAttribute a : indexedAttributes) {
				IGraphNodeIndex i = graph.getOrCreateNodeIndex(String.format("%s##%s##%s", 
					classifier.getPackageNSURI(), eObject.getType().getName(), a.getName()));

				final Object rawValue = eObject.get(a);
				i.add(node, a.getName(), convertValue(a, rawValue));
			}

			return node;
		} catch (Exception e) {
			LOGGER.error("Error in inserting attributes", e);
		}

		return null;
	}

	/**
	 * Creates the model element node in the graph. Does not index nor add derived feature nodes. 
	 */
	private IGraphNode createBasicEObjectNode(IHawkObject eObject, final List<IHawkAttribute> normalAttributes, final List<IHawkAttribute> indexedAttributes) throws Exception {
		final String eObjectId = getEObjectId(eObject);

		final Map<String, Object> nodeMap = new HashMap<>();
		nodeMap.put(IModelIndexer.IDENTIFIER_PROPERTY, eObjectId);
		nodeMap.put(IModelIndexer.SIGNATURE_PROPERTY, eObject.signature());
		classifyAttributes(eObject, normalAttributes, indexedAttributes);
		for (IHawkAttribute a1 : normalAttributes) {
			final Object value1 = eObject.get(a1);
			nodeMap.put(a1.getName(), convertValue(a1, value1));
		}

		try {
			IGraphNode node = graph.createNode(nodeMap, ModelElementNode.OBJECT_VERTEX_LABEL);
			if (eObject.isFragmentUnique()) {
				fragmentIdx.add(node, "id", eObject.getUriFragment());
				fragmentIdx.flush();
			}

			// propagate changes to listeners
			listener.modelElementAddition(commitItem, eObject, node, false);
			for (String s : nodeMap.keySet()) {
				Object value = nodeMap.get(s);
				listener.modelElementAttributeUpdate(commitItem, eObject, s, null, value, node,
						ModelElementNode.TRANSIENT_ATTRIBUTES.contains(s));
			}

			return node;
		} catch (Throwable ex) {
			LOGGER.error(ex.getMessage(), ex);
		}

		return null;
	}

	private Object convertValue(IHawkAttribute attribute, final Object value) {
		if (!attribute.isMany()) {
			final Class<?> valueClass = value.getClass();
			if (GraphUtil.isPrimitiveOrWrapperType(valueClass)) {
				return value;
			} else if (value instanceof Date) {
				return formatDate((Date)value);
			}  else {
				return value.toString();
			}
		} else {
			Collection<Object> collection;
			if (attribute.isUnique()) {
				collection = new LinkedHashSet<Object>();
			} else {
				collection = new LinkedList<Object>();
			}

			final Collection<?> srcCollection = (Collection<?>) value;
			Class<?> elemClass = null;
			boolean primitiveOrWrapperClass = false;
			if (!srcCollection.isEmpty()) {
				final Object first = srcCollection.iterator().next();
				elemClass = first.getClass();
				primitiveOrWrapperClass = GraphUtil.isPrimitiveOrWrapperType(elemClass);
				if (primitiveOrWrapperClass) {
					for (Object o : srcCollection) {
						collection.add(o);
					}
				} else if (first instanceof Date) {
					for (Object o : srcCollection) {
						collection.add(formatDate((Date)o));
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

			return ret;
		}
	}

	protected Object formatDate(final Date value) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		return sdf.format(value);
	}

	/**
	 * Adds all set attributes to <code>allAttributes</code>, and all set and
	 * indexed attributes to <code>indexedAttributes</code>.
	 */
	private void classifyAttributes(IHawkObject eObject, final List<IHawkAttribute> allAttributes, final List<IHawkAttribute> indexedAttributes) throws Exception {
		final IHawkClass iHawkClass = (IHawkClass) eObject.getType();
		for (final IHawkAttribute eAttribute : iHawkClass.getAllAttributes()) {
			if (eObject.isSet(eAttribute)) {
				final Map<String, Slot> slots = typeCache.getEClassNodeSlots(graph, eObject.getType());
				final Slot slot = slots.get(eAttribute.getName());

				if (slot == null) {
					LOGGER.error("Attribute {} is not within the properties of the node for type {}, skipping",
							eAttribute.getName(), iHawkClass.getName());
				} else {
					allAttributes.add(eAttribute);

					if (slot.isIndexed()) {
						indexedAttributes.add(eAttribute);
					}
				}
			}
		}
	}

	private void addDerivedFeatureNodes(IGraphNode node, IGraphNode typenode, String metamodelURI, Map<String, Slot> slots) {
		TypeNode tn = new TypeNode(typenode);
		for (Slot slot : slots.values()) {
			if (!slot.isDerived()) {
				continue;
			}

			Map<String, Object> dfnAttributes = new HashMap<>(); 
			dfnAttributes.put("isMany", slot.isMany());
			dfnAttributes.put("isOrdered", slot.isOrdered());
			dfnAttributes.put("isUnique", slot.isUnique());
			dfnAttributes.put("attributetype", slot.getType());
			dfnAttributes.put("derivationlanguage", slot.getDerivationLanguage());
			dfnAttributes.put("derivationlogic", slot.getDerivationLogic());

			/*
			 * We cannot use tn.getMetamodelURI() - in Neo4j batch mode, we can't get the
			 * outgoing edges of a certain type and instead we get all outgoing edges. Instead,
			 * we rely on the metamodel URI reported by the IHawkClassifier.
			 */
			final String idxName = String.format("%s##%s##%s", metamodelURI, tn.getTypeName(), slot.getName());
			dfnAttributes.put(GraphModelInserter.DERIVED_IDXNAME_NODEPROP, idxName);
			dfnAttributes.put(slot.getName(),
				DirtyDerivedFeaturesListener.NOT_YET_DERIVED_PREFIX + slot.getDerivationLogic());

			IGraphNode derivedattributenode = graph.createNode(dfnAttributes, "derivedattribute");

			graph.createRelationship(node, derivedattributenode, slot.getName(),
				Collections.singletonMap(GraphModelInserter.DERIVED_FEATURE_EDGEPROP, true));
			addToProxyAttributes(derivedattributenode);
		}
	}

	private void addToProxyAttributes(IGraphNode node) {
		Map<String, Object> m = new HashMap<>();
		m.put("derived", "_");

		derivedProxyDictionary.add(node, m);
	}

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
	public IGraphNode addEObject(IGraphNode originatingFile, IHawkObject eObject,
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
				LOGGER.error("The node for {} is null", eObject);
			} else {
				hash.put(splitURI(eObject.getUri()), node);

				final HashMap<String, Object> emptyMap = new HashMap<String, Object>();
				createReference(ModelElementNode.EDGE_LABEL_OFTYPE, node, eClass, emptyMap, true);
				createReference(ModelElementNode.EDGE_LABEL_OFKIND, node, eClass, emptyMap, true);
				if (originatingFile != null) {
					createReference(ModelElementNode.EDGE_LABEL_FILE, node, originatingFile, emptyMap, true);
				}
				objectCount[1]++;

				// use metamodel to infer all supertypes for fast search and log them
				for (IHawkClass superType : ((IHawkClass) eObject.getType()).getAllSuperTypes()) {
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

	private Pair<String, String> splitURI(String uri) {
		final String[] parts = uri.split("#", 1);

		if (parts.length == 1) {
			return new Pair<>(parts[0], "");
		} else {
			return new Pair<>(parts[0], parts[1]);
		}
	}

	private IGraphNode getFromFragmentIndex(IHawkObject eObject) {
		IGraphNode node = null;

		final Iterator<IGraphNode> itr = fragmentIdx.get("id", eObject.getUriFragment()).iterator();
		while (itr.hasNext()) {
			if (node == null)
				node = itr.next();
			else {
				LOGGER.warn("isFragmentUnique returned more than one node, keeping first one.");
				break;
			}
		}

		return node;
	}

	/**
	 * Creates an edge with the parameters given and links it to the appropriate
	 * nodes. Both nodes are contained in the same resource.
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

		source = hash.get(splitURI(from.getUri()));
		destination = hash.get(splitURI(to.getUri()));

		if (source == null && destination == null) {
			LOGGER.warn(
				"hash error 1, not found from (class: {}) and to (class: {}) on reference: {}, source = {}, destination = {}",
				from.getType().getName(), ((IHawkObject) to).getType().getName(), edgelabel, source, destination
			);
		} else if (source == null) {
			LOGGER.warn(
				"hash error 2, not found from (class: {}) and to (class: {}) on reference: {}, source = {}, destination = {}",
				from.getType().getName(), ((IHawkObject) to).getType().getName(), edgelabel, source, destination
			);
		} else if (destination == null) {
			// the modelling technology managed to resolve a cross-file proxy
			// early (before it was inserted into hawk -- handle it like any
			// other proxy)

			addProxyRef(from, to, edgelabel, isContainment, isContainer);
		} else {
			Map<String, Object> props = new HashMap<String, Object>();

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
			LOGGER.error(e.getMessage(), e);
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
	public boolean addEReferences(IHawkObject source, boolean resourceCanProvideSingletons) throws Exception {
		boolean atLeastOneSetReference = false;
		if (source.isFragmentUnique() && resourceCanProvideSingletons && hash.get(splitURI(source.getUri())) == null) {
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
		IGraphNode withProxy = hash.get(splitURI(from.getUri()));
		return addProxyRef(withProxy, destinationObject, edgelabel, isContainment, isContainer);
	}

	private boolean addProxyRef(IGraphNode node, IHawkObject destinationObject, String edgelabel, boolean isContainment,
			boolean isContainer) {

		try {
			final String uri = destinationObject.getUri();

			String destinationObjectRelativePathURI = uri;
			if (destinationObject.isFragmentUnique()) {
				/*
				 * In this scenario, we don't care about the file anymore: we
				 * need to flag it properly for the later resolution.
				 */
				destinationObjectRelativePathURI = GraphModelUpdater.PROXY_FILE_WILDCARD + "#"
						+ destinationObjectRelativePathURI.substring(destinationObjectRelativePathURI.indexOf("#") + 1);
			} else if (!destinationObject.URIIsRelative()) {
				if (destinationObjectRelativePathURI.startsWith(tempDirURI)) {
					destinationObjectRelativePathURI = destinationObjectRelativePathURI.substring(tempDirURI.length());
				} else {
					final IVcsManager vcs = commitItem.getCommit().getDelta().getManager();
					destinationObjectRelativePathURI = vcs.getRepositoryPath(destinationObjectRelativePathURI);
				}
			}

			final String destinationObjectRelativeFileURI =
				destinationObjectRelativePathURI.substring(0, destinationObjectRelativePathURI.indexOf("#"));

			String destinationObjectFullPathURI = repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + destinationObjectRelativePathURI;
			String destinationObjectFullFileURI = repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + destinationObjectRelativeFileURI;

			Object proxies = null;
			proxies = node.getProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX + destinationObjectFullFileURI);
			proxies = new Utils().addToElementProxies((String[]) proxies, destinationObjectFullPathURI, edgelabel, isContainment, isContainer);

			node.setProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX + destinationObjectFullFileURI, proxies);

			proxyDictionary.add(node, Collections.singletonMap(
				GraphModelUpdater.PROXY_REFERENCE_PREFIX, destinationObjectFullFileURI));

		} catch (Exception e) {
			LOGGER.error("proxydictionary error", e);
			return false;
		}
		return true;
	}

	protected boolean resolveProxyRef(IGraphNode source, IGraphNode target, String edgeLabel, boolean isContainment,
			boolean isContainer) throws Exception {

		refreshIndexes();

		boolean found = false;
		for (IGraphEdge e : source.getOutgoingWithType(edgeLabel)) {
			if (e.getEndNode().getId().equals(target.getId())) {
				found = true;
				break;
			}
		}

		if (found)
			return false;
		else {
			final HashMap<String, Object> props = new HashMap<String, Object>();
			if (isContainment) {
				props.put(ModelElementNode.EDGE_PROPERTY_CONTAINMENT, "true");
			} else if (isContainer) {
				props.put(ModelElementNode.EDGE_PROPERTY_CONTAINER, "true");
			}
			graph.createRelationship(source, target, edgeLabel, props);
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
