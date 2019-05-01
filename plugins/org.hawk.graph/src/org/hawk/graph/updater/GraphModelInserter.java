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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
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
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.updater.proxies.ProxyReferenceList;
import org.hawk.graph.updater.proxies.ProxyReferenceList.ProxyReference;
import org.hawk.graph.updater.proxies.ProxyReferenceTarget;
import org.hawk.graph.util.GraphUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * creates a database with the input xmi file (in args[0]) or reads it if the
 * database exists; and provides debugging information
 * 
 */
public class GraphModelInserter {

	protected class ReloadNodeCollectionIterable implements Iterable<IGraphNode> {
		private final Iterable<IGraphNode> nodes;

		protected ReloadNodeCollectionIterable(Iterable<IGraphNode> nodesToBeUpdated) {
			this.nodes = nodesToBeUpdated;
		}

		@Override
		public Iterator<IGraphNode> iterator() {
			Iterator<IGraphNode> itNodes = nodes.iterator();

			return new Iterator<IGraphNode>() {
				@Override
				public boolean hasNext() {
					return itNodes.hasNext();
				}

				@Override
				public IGraphNode next() {
					Object id = itNodes.next().getId();
					return indexer.getGraph().getNodeById(id);
				}
			};
		}
	}

	/**
	 * Timestamp of the last time we derived features on a node. Used to make sure
	 * new node versions are created when an attribute is derived in time-aware
	 * backends.
	 */
	public static final String LAST_DERIVED_TSTAMP_NODEPROP = "h_lastDerived";

	/**
	 * Name of the node index used to track property accesses during derived feature computation.
	 */
	public static final String DERIVED_ACCESS_IDXNAME = "derivedaccessdictionary";

	/**
	 * Property set on the edges that connects a model element to its derived
	 * feature nodes.
	 */
	public static final String DERIVED_FEATURE_EDGEPROP = "isDerived";

	/**
	 * Name of the feature in the derived feature nodes which stores the name of the
	 * index which should be told about any new values.
	 */
	public static final String DERIVED_IDXNAME_NODEPROP = "indexName";

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphModelInserter.class);

	// toggle to enabled detailed output of update process
	private static final boolean enableDebug = false;

	private static final int PROXY_RESOLVE_NOTIFY_INTERVAL = 25000;
	private static final int PROXY_RESOLVE_TX_SIZE = 5000;
	private static final int DERIVED_PNODE_TX_SIZE = 1000;
	private static final double MAX_TX_LOADRATIO = 0.5;

	private String repoURL;
	private String tempDirURI;

	private IHawkModelResource resource;
	private Map<String, IHawkObject> updated = new HashMap<>();
	private Map<String, IHawkObject> added = new HashMap<>();
	private Map<String, IHawkObject> unchanged = new HashMap<>();
	private Map<String, IHawkObject> retyped = new HashMap<>();

	private IModelIndexer indexer;
	private IGraphDatabase graph;
	private GraphModelBatchInjector inj;
	private VcsCommitItem commitItem;

	private Map<String, IGraphNode> nodes = new HashMap<>();
	private TypeCache typeCache;
	private Supplier<DeletionUtils> deletionUtils;

	public GraphModelInserter(IModelIndexer hawk, Supplier<DeletionUtils> deletionUtils, TypeCache typeCache) {
		this.indexer = hawk;
		this.graph = indexer.getGraph();
		this.typeCache = typeCache;
		this.deletionUtils = deletionUtils;
	}

	public boolean run(IHawkModelResource res, VcsCommitItem s, final boolean verbose) throws Exception {
		if (verbose) {
			indexer.getCompositeStateListener().info("Calculating model delta for file: " + s.getPath() + "...");
		}

		this.resource = res;
		this.commitItem = s;
		this.inj = new GraphModelBatchInjector(graph, typeCache, this.commitItem, indexer.getCompositeGraphChangeListener());

		final double ratio = calculateModelDeltaRatio(verbose);
		if (ratio >= 0) {
			this.tempDirURI = new File(graph.getTempDir()).toURI().toString();
			if (verbose) {
				LOGGER.debug("File already present, calculating deltas with respect to graph storage");
			}

			if (ratio > MAX_TX_LOADRATIO) {
				return batchUpdate(verbose);
			} else {
				indexer.getCompositeStateListener()
					.info("Performing transactional update (ratio:" + ratio + ") on file: " + commitItem.getPath() + "...");
				LOGGER.debug("transactional update called");

				return transactionalUpdate(verbose);
			}

		} else {
			/*
			 * Populate the database from scratch (for this file) -- this will trigger
			 * calculation of all derived attributes.
			 */
			return addNodes(verbose);
		}
	}

	@SuppressWarnings("unchecked")
	private boolean transactionalUpdate(final boolean verbose) throws Exception {
		graph.exitBatchMode();

		final IGraphChangeListener listener = indexer.getCompositeGraphChangeListener();
		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.changeStart();

			repoURL = commitItem.getCommit().getDelta().getManager().getLocation();
			IGraphNode fileNode = graph.getFileIndex()
					.get("id", repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + commitItem.getPath()).iterator().next();

			// manage retyped nodes
			for (final Map.Entry<String, IHawkObject> entry : retyped.entrySet()) {
				final String uriFragment = entry.getKey();
				final IHawkObject o = entry.getValue();

				final IGraphNode node = nodes.remove(uriFragment);
				remove(node, fileNode, listener);
				added.put(uriFragment, o);
			}

			// add new nodes
			final Map<IGraphNode, IHawkObject> addedNodes = new HashMap<>();
			final Map<String, IGraphNode> addedNodesHash = new HashMap<>();

			for (final String o : added.keySet()) {
				final IHawkObject object = added.get(o);
				final IGraphNode fileNode1 = fileNode;
				final IGraphNode node1 = inj.addEObject(fileNode1, object, resource.providesSingletonElements());
				addedNodes.put(node1, object);
				final String newID = node1.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
				addedNodesHash.put(newID, node1);

				// track change new node
				for (final String transientLabelEdge : ModelElementNode.TRANSIENT_EDGE_LABELS) {
					for (final IGraphEdge e : node1.getOutgoingWithType(transientLabelEdge)) {
						listener.referenceAddition(commitItem, node1, e.getEndNode(), transientLabelEdge, true);
					}
				}
			}

			// references of added object and tracking of changes
			for (IGraphNode node : addedNodes.keySet()) {
				inj.addEReferences(fileNode, node, addedNodes.get(node), addedNodesHash, nodes);
			}

			// delete obsolete nodes and change attributes
			for (String s : nodes.keySet()) {

				IGraphNode node = nodes.get(s);

				if (unchanged.containsKey(node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY))) {
					// do nothing as node is identical to current model element
				} else if (updated.containsKey(node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY))) {
					// remove all old proxies of this node to other nodes (as
					// any new ones will be re-created)
					cleanupNode(node);
					// change properties of node to the new values
					final IHawkObject o = updated.get(node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));
					node.setProperty(IModelIndexer.SIGNATURE_PROPERTY, o.signature());
					updateNodeProperties(fileNode, node, o);
				} else {
					remove(node, fileNode, listener);
				}
			}

			// change references (including adding new proxies as required)
			for (String o : updated.keySet()) {
				IHawkObject source = updated.get(o);
				IGraphNode node = nodes.get(source.getUriFragment());

				// its null if it was just inserted (above), this is fine.
				if (node == null)
					continue;

				for (IHawkReference r : ((IHawkClass) source.getType()).getAllReferences()) {

					if (source.isSet(r)) {

						Object targets = source.get(r, false);
						String refname = r.getName();
						boolean isContainment = r.isContainment();
						boolean isContainer = r.isContainer();
						Set<String> targetids = new HashSet<>();

						if (targets instanceof Iterable<?>) {
							for (IHawkObject target : ((Iterable<IHawkObject>) targets)) {
								if (!target.isInDifferentResourceThan(source))
									targetids.add(target.getUriFragment());
								else {
									addProxyRef(node, target, refname, isContainment, isContainer);
								}
							}
						} else {
							if (!((IHawkObject) targets).isInDifferentResourceThan(source))
								targetids.add(((IHawkObject) targets).getUriFragment());
							else {
								addProxyRef(node, (IHawkObject) targets, refname, isContainment, isContainer);
							}
						}

						//
						Iterable<IGraphEdge> graphtargets = node.getOutgoingWithType(refname);

						for (IGraphEdge e : graphtargets) {
							IGraphNode n = e.getEndNode();

							final Object id = n.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
							final boolean targetIdPresent = targetids.remove(id);
							if (!targetIdPresent) {
								// need to store this before we delete the edge:
								// once we delete() it might be impossible to
								// retrieve it
								final String edgeType = e.getType();

								// delete removed reference
								e.delete();

								// track change deleted reference
								listener.referenceRemoval(this.commitItem, node, n, edgeType, false);
							}
						}

						for (String s : targetids) {
							IGraphNode dest = nodes.get(s);
							if (dest == null) {
								dest = addedNodesHash.get(s);
							}

							if (dest == null)
								dest = addedNodesHash.get(s);
							if (dest != null) {
								// add new reference
								final Map<String, Object> props = new HashMap<>();
								if (isContainment) {
									props.put(ModelElementNode.EDGE_PROPERTY_CONTAINMENT, "true");
								}
								if (isContainer) {
									props.put(ModelElementNode.EDGE_PROPERTY_CONTAINER, "true");
								}
								graph.createRelationship(node, dest, refname, props);

								// track change new reference
								listener.referenceAddition(this.commitItem, node, dest, refname, false);
							} else {
								// proxy reference, handled above
							}
						}

					} else {
						// delete unset references which may have been
						// previously set
						String refname = r.getName();
						Iterable<IGraphEdge> graphtargets = node.getOutgoingWithType(refname);
						// track change deleted references
						for (IGraphEdge e : graphtargets) {
							final IGraphNode endNode = e.getEndNode();
							final String type = e.getType();
							e.delete();
							listener.referenceRemoval(this.commitItem, node, endNode, type, false);
						}
					}

				} // for (IHawkReference r)

			} // for (String o)

			fileNode.setProperty("revision", commitItem.getCommit().getRevision());
			t.success();
			listener.changeSuccess();
			return true;
		} catch (Exception e) {
			LOGGER.error("exception in transactionalUpdate()", e);
			listener.changeFailure();
			return false;
		} finally {
			if (verbose) {
				indexer.getCompositeStateListener()
						.info("Performed transactional update on file: " + commitItem.getPath() + ".");
			}
		}

	}

	private void cleanupNode(IGraphNode node) {

		IGraphNodeIndex proxyDictionary = graph.getOrCreateNodeIndex(GraphModelBatchInjector.PROXY_DICT_NAME);
		proxyDictionary.remove(node);

		for (String propertyKey : node.getPropertyKeys()) {
			if (propertyKey.startsWith(GraphModelUpdater.PROXY_REFERENCE_PREFIX))
				node.removeProperty(propertyKey);
		}

	}

	protected void remove(IGraphNode node, IGraphNode fileNode, final IGraphChangeListener listener) {
		// track change deleted node
		for (String key : node.getPropertyKeys()) {
			listener.modelElementAttributeRemoval(this.commitItem, null, key, node,
					ModelElementNode.TRANSIENT_ATTRIBUTES.contains(key));
		}
		for (IGraphEdge e : node.getOutgoing()) {
			if (e.getProperty(DERIVED_FEATURE_EDGEPROP) == null) {
				final boolean isTransient = ModelElementNode.TRANSIENT_EDGE_LABELS.contains(e.getType());
				listener.referenceRemoval(this.commitItem, node, e.getEndNode(), e.getType(), isTransient);
			}
		}

		remove(node, repoURL, fileNode, listener);
	}

	private boolean addProxyRef(final IGraphNode node, final IHawkObject destinationObject, final String edgelabel,
			boolean isContainment, boolean isContainer) {

		try {
			final String uri = destinationObject.getUri();

			String destinationObjectRelativePathURI = uri;
			if (!destinationObject.URIIsRelative()) {
				if (destinationObjectRelativePathURI.startsWith(tempDirURI)) {
					destinationObjectRelativePathURI = destinationObjectRelativePathURI.substring(tempDirURI.length());
				} else {
					final IVcsManager vcs = commitItem.getCommit().getDelta().getManager();
					destinationObjectRelativePathURI = vcs.getRepositoryPath(destinationObjectRelativePathURI);
				}
			}

			String destinationObjectRelativeFileURI = destinationObjectRelativePathURI;

			destinationObjectRelativeFileURI = destinationObjectRelativePathURI
					.substring(destinationObjectRelativePathURI.indexOf("#"));

			String destinationObjectFullPathURI = repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
					+ destinationObjectRelativePathURI;

			String destinationObjectFullFileURI = repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
					+ destinationObjectRelativeFileURI;

			Object proxies = null;
			proxies = node.getProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX + destinationObjectFullFileURI);
			proxies = new Utils().addToElementProxies((String[]) proxies, destinationObjectFullPathURI, edgelabel,
					isContainment, isContainer);

			node.setProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX + destinationObjectFullFileURI, proxies);

			IGraphNodeIndex proxyDictionary = graph.getOrCreateNodeIndex(GraphModelBatchInjector.PROXY_DICT_NAME);
			proxyDictionary.add(node, GraphModelUpdater.PROXY_REFERENCE_PREFIX, destinationObjectFullFileURI);

		} catch (Exception e) {
			LOGGER.error("proxydictionary error", e);
			return false;
		}
		return true;
	}

	private void updateNodeProperties(IGraphNode fileNode, IGraphNode node, IHawkObject eObject) {

		List<IHawkAttribute> normalattributes = new LinkedList<IHawkAttribute>();
		List<IHawkAttribute> indexedattributes = new LinkedList<IHawkAttribute>();
		IGraphNode typenode = node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator().next()
				.getEndNode();
		final IGraphChangeListener listener = indexer.getCompositeGraphChangeListener();

		for (final IHawkAttribute eAttribute : ((IHawkClass) eObject.getType()).getAllAttributes()) {
			final String attrName = eAttribute.getName();
			if (eObject.isSet(eAttribute)) {
				final String[] propValue = (String[]) typenode.getProperty(attrName);
				if (propValue != null && propValue[5].equals("t")) {
					indexedattributes.add(eAttribute);
				}

				normalattributes.add(eAttribute);
			} else if (node.getProperty(attrName) != null) {
				node.removeProperty(attrName);
				indexer.getCompositeGraphChangeListener().modelElementAttributeRemoval(commitItem, eObject, eAttribute.getName(),
						node, false);
			}
		}

		for (IHawkAttribute a : normalattributes) {
			final Object oldproperty = node.getProperty(a.getName());
			final Object newproperty = eObject.get(a);

			if (!a.isMany()) {
				Object newValue = newproperty;
				if (newValue instanceof Date) {
					newValue = formatDate((Date)newValue);
				} else if (!GraphUtil.isPrimitiveOrWrapperType(newproperty.getClass())) {
					newValue = newValue.toString();
				}

				if (!newValue.equals(oldproperty)) {
					// track changed property (primitive)
					listener.modelElementAttributeUpdate(this.commitItem, eObject, a.getName(), oldproperty, newValue, node,
							false);
					node.setProperty(a.getName(), newValue);
				}
			} else {
				Collection<Object> collection = null;

				if (a.isUnique())
					collection = new LinkedHashSet<Object>();
				else
					collection = new LinkedList<Object>();

				final Collection<?> srcCollection = (Collection<?>) newproperty;
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
							collection.add(formatDate((Date) o));
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

				if (!ret.equals(oldproperty)) {
					listener.modelElementAttributeUpdate(this.commitItem, eObject, a.getName(), oldproperty, ret, node, false);
					node.setProperty(a.getName(), ret);
				}
			}

		}

		for (IHawkAttribute a : indexedattributes) {

			IGraphNodeIndex i = graph.getOrCreateNodeIndex(
					eObject.getType().getPackageNSURI() + "##" + eObject.getType().getName() + "##" + a.getName());

			Object v = eObject.get(a);

			if (!a.isMany()) {
				if (GraphUtil.isPrimitiveOrWrapperType(v.getClass())) {
					i.add(node, a.getName(), v);
				} else if (v instanceof Date) {
					i.add(node, a.getName(), formatDate((Date)v));
				} else {
					i.add(node, a.getName(), v.toString());
				}
			} else {
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
					r = Array.newInstance(elemClass, 1);
				} else {
					r = Array.newInstance(String.class, 1);
				}
				Object ret = collection.toArray((Object[]) r);

				i.add(node, a.getName(), ret);

			}

		}

		final IGraphNodeIndex rootDictionary = graph.getOrCreateNodeIndex(GraphModelBatchInjector.ROOT_DICT_NAME);
		if (eObject.isRoot()) {
			rootDictionary.add(node, GraphModelBatchInjector.ROOT_DICT_FILE_KEY, fileNode.getId());
		} else {
			rootDictionary.remove(node);
		}
	}

	private boolean batchUpdate(final boolean verbose) throws Exception {
		if (verbose) {
			indexer.getCompositeStateListener().info("Performing batch update of file: " + commitItem.getPath() + "...");
		}
		final IGraphChangeListener listener = indexer.getCompositeGraphChangeListener();
		listener.changeStart();
		try {
			IGraphNode g = new Utils().getFileNodeFromVCSCommitItem(graph, commitItem);

			if (g != null) {
				try (IGraphTransaction t = graph.beginTransaction()) {
					deletionUtils.get().deleteAll(g, commitItem, listener);
					t.success();
				}
			}
			graph.enterBatchMode();
			new GraphModelBatchInjector(indexer, deletionUtils, typeCache, commitItem, resource, listener, verbose);
			listener.changeSuccess();
			return true;
		} catch (Exception ex) {
			listener.changeFailure();
			return false;
		} finally {
			if (verbose) {
				indexer.getCompositeStateListener().info("Performed batch update of file: " + commitItem.getPath() + ".");
			}
		}
	}

	private double calculateModelDeltaRatio(boolean verbose) throws Exception {
		if (verbose) {
			LOGGER.info("calculateModelDeltaSize() called");
		}

		final IGraphNode fileNode = new Utils().getFileNodeFromVCSCommitItem(graph, commitItem);
		if (fileNode != null) {
			return calculateModelDeltaRatio(fileNode, verbose);
		} else {
			if (verbose) {
				LOGGER.info("File not in store, performing initial batch file insertion");
			}
			return -1;
		}
	}

	protected double calculateModelDeltaRatio(final IGraphNode fileNode, boolean verbose) throws Exception {
		try (IGraphTransaction t = graph.beginTransaction()) {
			final Map<String, byte[]> signatures = new HashMap<>();

			// Get existing nodes from the store (and their signatures)
			for (IGraphEdge e : fileNode.getIncomingWithType(ModelElementNode.EDGE_LABEL_FILE)) {
				IGraphNode n = e.getStartNode();
				nodes.put(n.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString(), n);

				signatures.put((String) n.getProperty(IModelIndexer.IDENTIFIER_PROPERTY),
						(byte[]) n.getProperty(IModelIndexer.SIGNATURE_PROPERTY));
			}
			if (verbose) {
				LOGGER.info("File contains: {} ({}) nodes in store", nodes.size(), signatures.size());
			}

			// Get the model elements from the resource and use signatures and URI
			for (IHawkObject o : resource.getAllContents()) {
				final String uriFragment = o.getUriFragment();
				byte[] hash = signatures.get(uriFragment);
				if (hash != null) {
					if (!Arrays.equals(hash, o.signature())) {
						final String actualType = o.getType().getName();

						final IGraphNode node = nodes.get(uriFragment);
						final Iterator<IGraphEdge> typeEdges = node
								.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator();
						final IGraphNode typeNode = typeEdges.next().getEndNode();
						final String nodeType = typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
						if (actualType.equals(nodeType)) {
							this.updated.put(uriFragment, o);
						} else {
							/*
							 * The model element with this URI fragment has changed type from the previous
							 * version of the model to the current version.
							 */
							this.retyped.put(uriFragment, o);
						}
					} else {
						this.unchanged.put(uriFragment, o);
					}
				} else {
					this.added.put(uriFragment, o);
				}
			}
			t.success();

			final int addedn = added.size();
			final int retypedn = retyped.size();
			final int updatedn = updated.size();
			final int deletedn = nodes.size() - unchanged.size() - updatedn - retypedn;

			final double ratio = (addedn + retypedn + updatedn + deletedn) / ((double) nodes.size());
			if (verbose) {
				LOGGER.info("Update contains | a:{} u:{} d:{} ratio: {}",
					(addedn + retypedn), updatedn, deletedn, ratio);
			}

			return ratio;
		}
	}

	/**
	 * Populates the database with the model, using util.parseresource
	 * 
	 * @param verbose
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	private boolean addNodes(boolean verbose) throws Exception {
		if (verbose) {
			indexer.getCompositeStateListener().info("Performing batch insert on file: " + commitItem.getPath() + "...");
		}
		boolean success = true;
		if (resource != null) {
			GraphModelBatchInjector batch = new GraphModelBatchInjector(indexer, deletionUtils, typeCache,
				commitItem, resource,
				indexer.getCompositeGraphChangeListener(), verbose);
			success = batch.getSuccess();
			if (!success) {
				LOGGER.error(
						"model insertion aborted: see above error (maybe you need to register the metamodel?)");
			}
		} else {
			LOGGER.error("model insertion aborted, see above error (maybe you need to register the metamodel?)");
		}

		if (verbose) {
			indexer.getCompositeStateListener().info("Performed batch insert on file: " + commitItem.getPath() + ".");
		}
		return success;
	}

	private void remove(IGraphNode modelElement, String repositoryURL, IGraphNode fileNode, IGraphChangeListener l) {
		DeletionUtils del = deletionUtils.get();
		del.dereference(modelElement, l, commitItem);
		del.makeProxyRefs(commitItem, modelElement, repositoryURL, fileNode, l);
		if (del.delete(modelElement)) {
			l.modelElementRemoval(this.commitItem, modelElement, false);
		}
	}

	public void resolveProxies(IGraphDatabase graph) throws Exception {
		final long start = System.currentTimeMillis();

		final IGraphChangeListener listener = indexer.getCompositeGraphChangeListener();

		// First, find out about all the proxy reference lists that we have to process
		final List<ProxyReferenceList> proxyReferenceLists = getProxyReferenceLists(graph);

		// Now reorganize individual proxy references by target file
		int nToBeProcessed = 0;
		final Map<ProxyReferenceTarget, List<ProxyReference>> refsByTargetFile = new HashMap<>();
		for (ProxyReferenceList list : proxyReferenceLists) {
			List<ProxyReference> refs = refsByTargetFile.get(list.getTargetFile());
			if (refs == null) {
				refs = new ArrayList<>();
				refsByTargetFile.put(list.getTargetFile(), refs);
			}

			refs.addAll(list.getReferences());
			nToBeProcessed += list.getReferences().size();
		}

		final long startMillis = System.currentTimeMillis();
		int totalProcessed = 0, currentProcessed = 0, totalResolved = 0;
		final Iterator<Entry<ProxyReferenceTarget, List<ProxyReference>>> itTargetFiles
			= refsByTargetFile.entrySet().iterator();
		if (nToBeProcessed > 0) {
			indexer.getCompositeStateListener()
				.info(String.format("Processing %d/%d proxy references (%d sec total)",
					totalProcessed, nToBeProcessed, (System.currentTimeMillis() - startMillis) / 1000));
		}

		// Go through the proxy references pointing to each target file
		while (itTargetFiles.hasNext()) {
			final Entry<ProxyReferenceTarget, List<ProxyReference>> entry = itTargetFiles.next();
			final ProxyReferenceTarget targetFile = entry.getKey();
			final List<ProxyReference> refs = entry.getValue();

			int iFrom = 0;
			while (iFrom < refs.size()) {
				// Need to batch up proxy resolution to keep tx size bound
				List<ProxyReference> sublistRefs = refs.subList(iFrom,
						Math.min(refs.size(), iFrom + PROXY_RESOLVE_TX_SIZE));
				iFrom += PROXY_RESOLVE_TX_SIZE;

				try (IGraphTransaction tx = graph.beginTransaction()) {
					listener.changeStart();
					IGraphNodeIndex proxyDictionary = graph
							.getOrCreateNodeIndex(GraphModelBatchInjector.PROXY_DICT_NAME);
					final int nResolved = resolveProxies(graph, listener, targetFile, sublistRefs, proxyDictionary);
					currentProcessed += sublistRefs.size();
					totalResolved += nResolved;

					tx.success();
					listener.changeSuccess();
				} catch (Throwable ex) {
					listener.changeFailure();
					throw ex;
				}

				if (currentProcessed >= PROXY_RESOLVE_NOTIFY_INTERVAL) {
					totalProcessed += currentProcessed;
					currentProcessed = 0;
					final long elapsedSeconds = (System.currentTimeMillis() - startMillis) / 1000;
					indexer.getCompositeStateListener()
							.info(String.format("Processed %d/%d proxy references (%d sec total)",
									totalProcessed, nToBeProcessed, elapsedSeconds));
				}
			}
		}
		totalProcessed += currentProcessed;

		try (IGraphTransaction tx = graph.beginTransaction()) {
			final IGraphNodeIndex proxyDictionary = graph.getOrCreateNodeIndex(GraphModelBatchInjector.PROXY_DICT_NAME);
			final int proxiesLeft = proxyDictionary.query(GraphModelUpdater.PROXY_REFERENCE_PREFIX, "*").size();
			LOGGER.info("{} proxy ref lists left after resolving {} refs", proxiesLeft, totalResolved);
			tx.success();
		}

		LOGGER.info("proxy resolution took: ~{}s", (System.currentTimeMillis() - start) / 1000.0);
	}

	public List<ProxyReferenceList> getProxyReferenceLists(IGraphDatabase graph) throws Exception {
		final List<ProxyReferenceList> proxyReferenceLists = new ArrayList<>();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			IGraphNodeIndex proxyDictionary = graph.getOrCreateNodeIndex(GraphModelBatchInjector.PROXY_DICT_NAME);
			IGraphIterable<? extends IGraphNode> proxies = proxyDictionary.query(GraphModelUpdater.PROXY_REFERENCE_PREFIX, "*");
			for (IGraphNode n : proxies) {
				for (String propertyKey : n.getPropertyKeys()) {
					if (propertyKey.startsWith(GraphModelUpdater.PROXY_REFERENCE_PREFIX)) {
						final String[] propertyValue = (String[]) n.getProperty(propertyKey);
						if (propertyValue.length > 0) {
							proxyReferenceLists.add(new ProxyReferenceList(n, propertyValue));
						} else {
							// TODO debug and fix?
							LOGGER.warn("Proxy ref list is empty: node {}, key {}", n, propertyKey);
						}
					}
				}
			}
			tx.success();
		}
		return proxyReferenceLists;
	}

	private int resolveProxies(IGraphDatabase graph, IGraphChangeListener listener,	ProxyReferenceTarget targetFile, List<ProxyReference> references, IGraphNodeIndex proxyDictionary) throws Exception {
		// Do URI -> ref mapping, find proxy ref lists that will need to be updated
		final Map<String, List<ProxyReference>> refsByURI = new HashMap<>();
		final Set<ProxyReferenceList> refLists = new HashSet<>();
		for (ProxyReference ref : references) {
			List<ProxyReference> refs = refsByURI.get(ref.getTarget().getElementURI());
			if (refs == null) {
				refs = new LinkedList<>();
				refsByURI.put(ref.getTarget().getElementURI(), refs);
			}
			refs.add(ref);

			refLists.add(ref.getList());
		}

		// Keep track of how many we resolved
		int resolved = 0;

		if (targetFile.isFragmentBased()) {
			// GUID-based proxy resolution (e.g. for Modelio)
			resolved = resolveProxiesByFragment(graph, listener, refsByURI, resolved);
		} else {
			// URI-based proxy resolution (e.g. for most EMF models)
			resolved = resolveProxiesByPath(graph, listener, targetFile, refsByURI, resolved);
		}

		// Go through the proxy reference lists and update graph based on it
		for (ProxyReferenceList list : refLists) {
			final IGraphNode sourceNode = graph.getNodeById(list.getSourceNodeID());
			if (list.getReferences().isEmpty()) {
				sourceNode.removeProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX + list.getFullPathURI());
				proxyDictionary.remove(sourceNode, GraphModelUpdater.PROXY_REFERENCE_PREFIX, list.getFullPathURI());
			} else {
				sourceNode.setProperty(GraphModelUpdater.PROXY_REFERENCE_PREFIX + list.getFullPathURI(), list.toArray());
			}
		}

		return resolved;
	}

	private int resolveProxiesByPath(IGraphDatabase graph, IGraphChangeListener listener,
			ProxyReferenceTarget targetFile, final Map<String, List<ProxyReference>> refsByURI,
			int resolved) throws Exception {
		final IGraphNode fileNode = getFileNode(graph, targetFile.getRepositoryURL(), targetFile.getFilePath());
		Iterable<IGraphEdge> rels = allNodesWithFile(fileNode);
		if (rels != null) {
			for (IGraphEdge r : rels) {
				final IGraphNode targetNode = r.getStartNode();
				final String nodeURI = targetFile.getFileURI() + "#"
						+ targetNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();

				List<ProxyReference> pendingRefs = refsByURI.get(nodeURI);
				if (pendingRefs != null) {
					for (Iterator<ProxyReference> itPendingRefs = pendingRefs.iterator(); itPendingRefs.hasNext();) {
						ProxyReference pendingRef = itPendingRefs.next();

						final IGraphNode sourceNode = graph.getNodeById(pendingRef.getList().getSourceNodeID());
						boolean change = new GraphModelBatchInjector(graph, typeCache, null, listener)
								.resolveProxyRef(sourceNode, targetNode, pendingRef.getEdgeLabel(),
										pendingRef.isContainment(), pendingRef.isContainer());

						if (change) {
							itPendingRefs.remove();
							++resolved;

							// TODO: something more efficient than this?
							pendingRef.getList().getReferences().remove(pendingRef);

							listener.referenceAddition(this.commitItem, sourceNode, targetNode,
									pendingRef.getEdgeLabel(), false);
						}
					}
				}
			}
		}
		return resolved;
	}

	private int resolveProxiesByFragment(IGraphDatabase graph, IGraphChangeListener listener,
			final Map<String, List<ProxyReference>> refsByURI, int resolved) throws Exception {
		final IGraphNodeIndex fragDictionary = graph.getOrCreateNodeIndex(GraphModelBatchInjector.FRAGMENT_DICT_NAME);

		for (List<ProxyReference> refs : refsByURI.values()) {
			for (Iterator<ProxyReference> itPendingRefs = refs.iterator(); itPendingRefs.hasNext();) {
				ProxyReference ref = itPendingRefs.next();
				final String fragment = ref.getTarget().getFragment();
				Iterator<? extends IGraphNode> targetNodes = fragDictionary.get("id", fragment).iterator();

				if (targetNodes.hasNext()) {
					final IGraphNode sourceNode = graph.getNodeById(ref.getList().getSourceNodeID());
					final IGraphNode targetNode = targetNodes.next();
					boolean change = new GraphModelBatchInjector(graph, typeCache, null, listener).resolveProxyRef(
							sourceNode, targetNode, ref.getEdgeLabel(), ref.isContainment(), ref.isContainer());

					if (change) {
						itPendingRefs.remove();
						++resolved;

						// TODO: something more efficient than this?
						ref.getList().getReferences().remove(ref);

						listener.referenceAddition(this.commitItem, sourceNode, targetNode, ref.getEdgeLabel(), false);
					}
				}
			}
		}
		return resolved;
	}

	public int resolveDerivedAttributeProxies(String type) throws Exception {
		IGraphIterable<? extends IGraphNode> allUnresolved = null;
		IGraphNodeIndex derivedProxyDictionary = null;
		int size = 0;

		LOGGER.info("Deriving attributes...");
		try (IGraphTransaction tx = graph.beginTransaction()) {
			derivedProxyDictionary = graph.getOrCreateNodeIndex("derivedproxydictionary");
			allUnresolved = derivedProxyDictionary.query("derived", "*");
			size = allUnresolved.size();
			tx.success();
		}

		if (size > 0) {
			processDerivedFeatureNodes(type, allUnresolved, size);
		}

		int derivedLeft = -1;
		try (IGraphTransaction tx = graph.beginTransaction()) {
			derivedLeft = ((IGraphIterable<IGraphNode>) derivedProxyDictionary.query("derived", "*")).size();
			tx.success();
		}
		LOGGER.info("{} - sets of proxy [derived] attributes left incomplete in the store", derivedLeft);

		return derivedLeft;
	}

	protected void processDerivedFeatureNodes(final String type, final Iterable<? extends IGraphNode> derivedFeatureNodes, final int nNodes)
			throws InvalidQueryException, QueryExecutionException, Exception {
		final long startMillis = System.currentTimeMillis();
		final IQueryEngine q = indexer.getKnownQueryLanguages().get(type);

		Iterator<? extends IGraphNode> itUnresolved;
		try (IGraphTransaction tx = graph.beginTransaction()) {
			itUnresolved = derivedFeatureNodes.iterator();
			tx.success();
		}

		// Process derived nodes in chunks, to keep the size of the access listener and the transactions bounded
		int count = 0;
		boolean done = false;
		while (!done) {
			try (IGraphTransaction tx = graph.beginTransaction()) {
				final long startChunkMillis = System.currentTimeMillis();

				final List<IGraphNode> chunk = new ArrayList<>(DERIVED_PNODE_TX_SIZE);
				for (int i = 0; i < DERIVED_PNODE_TX_SIZE && itUnresolved.hasNext(); i++) {
					chunk.add(itUnresolved.next());
				}
				done = !itUnresolved.hasNext();

				final IGraphNodeIndex derivedAccessDictionary = graph.getOrCreateNodeIndex(DERIVED_ACCESS_IDXNAME);
				final IAccessListener accessListener = q.calculateDerivedAttributes(indexer, chunk);

				// dump access to Lucene and add hooks on updates
				// TODO - break transactions by accesses, not by derived nodes
				for (IAccess a : accessListener.getAccesses()) {
					final IGraphNode sourceNode = graph.getNodeById(a.getSourceObjectID());
					derivedAccessDictionary.remove(sourceNode);
				}
				for (IAccess a : accessListener.getAccesses()) {
					final IGraphNode sourceNode = graph.getNodeById(a.getSourceObjectID());
					derivedAccessDictionary.add(sourceNode, a.getAccessObjectID(), a.getProperty());
				}

				if (enableDebug) {
					/* high overhead in certain corner cases (modelio -- large workspace -- only enable for debugging) */
					LOGGER.info("accesses: {} ({} nodes)", accessListener.getAccesses().size(), derivedAccessDictionary.query("*", "*").size());
				}

				accessListener.resetAccesses();
				tx.success();

				count += chunk.size();
				final long now = System.currentTimeMillis();
				final long chunkMillis = now - startChunkMillis;
				final long totalMillis = now - startMillis;
				indexer.getCompositeStateListener().info(String.format(
						"Processed %d/%d derived feature nodes of type '%s' (%d s, %d s total)",
								count, nNodes, type, chunkMillis / 1000, totalMillis / 1000));
			}
		}
	}

	public void updateDerivedAttributes(String type, Set<IGraphNode> nodesToBeUpdated) throws Exception {
		final IGraphChangeListener listener = indexer.getCompositeGraphChangeListener();

		// This is done outside any other tx, as we need to be able to break up into smaller tx
		final IQueryEngine q = indexer.getKnownQueryLanguages().get(type);
		if (q == null) {
			throw new IllegalArgumentException("Cannot derive attributes - query engine " + type + " is disabled");
		}
		final IAccessListener accessListener = q.calculateDerivedAttributes(
				indexer, new ReloadNodeCollectionIterable(nodesToBeUpdated));

		try (IGraphTransaction tx = graph.beginTransaction()) {
			listener.changeStart();
			// operations on the graph
			// ...

			// not needed as indexes should be up to date
			// nodesToBeUpdated = graph.retainExisting(nodesToBeUpdated);

			IGraphNodeIndex derivedAccessDictionary = graph.getOrCreateNodeIndex(DERIVED_ACCESS_IDXNAME);
			for (IAccess a : accessListener.getAccesses()) {
				IGraphNode sourceNode = graph.getNodeById(a.getSourceObjectID());
				if (sourceNode != null) {
					derivedAccessDictionary.remove(sourceNode);
				}
			}

			for (IAccess a : accessListener.getAccesses()) {
				IGraphNode sourceNode = graph.getNodeById(a.getSourceObjectID());
				if (sourceNode != null) {
					derivedAccessDictionary.add(sourceNode, a.getAccessObjectID(), a.getProperty());
				}
			}

			tx.success();
			listener.changeSuccess();
		} catch (Exception e) {
			listener.changeFailure();
			throw e;
		}
	}

	private static Iterable<IGraphEdge> allNodesWithFile(final IGraphNode fileNode) {
		if (fileNode != null)
			return fileNode.getIncomingWithType(ModelElementNode.EDGE_LABEL_FILE);
		else
			return null;
	}

	private IGraphNode getFileNode(IGraphDatabase graph, String repositoryURL, String file) {
		if (!file.startsWith("/")) {
			file = "/" + file;
		}
		final IGraphNodeIndex filedictionary = graph.getFileIndex();
		IGraphNode fileNode = null;

		try {
			final String idSameRepo = repositoryURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + file;
			final IGraphIterable<? extends IGraphNode> itNodes = filedictionary.get("id", idSameRepo);
			if (itNodes.size() > 0) {
				fileNode = itNodes.getSingle();
			} else {
				// TODO do this with just the graph - new index?

				// Try looking in another repository
				file = file.replaceFirst("^/", "");
				for (IVcsManager vcs : indexer.getRunningVCSManagers()) {
					if (file.startsWith(vcs.getLocation())) {
						String subpath = file.substring(vcs.getLocation().length());
						if (!subpath.startsWith("/")) {
							subpath = "/" + subpath;
						}
						fileNode = getFileNode(graph, vcs.getLocation(), subpath);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return fileNode;
	}

	public void updateDerivedAttribute(String metamodelUri, String typeName, String attributeName, String attributeType,
			boolean isMany, boolean isOrdered, boolean isUnique, String derivationlanguage, String derivationlogic) {

		final long startMillis = System.currentTimeMillis();
		LOGGER.info("Creating / updating derived attribute {}::{}#{}", metamodelUri, typeName, attributeName);

		// Add the new derived property nodes
		Set<IGraphNode> derivedPropertyNodes = new HashSet<>();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...
			IGraphNode metamodelNode = graph.getMetamodelIndex().get("id", metamodelUri).getSingle();

			IGraphNode typeNode = null;

			for (IGraphEdge e : metamodelNode.getIncomingWithType("epackage")) {
				IGraphNode othernode = e.getStartNode();
				if (othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(typeName)) {
					typeNode = othernode;
					break;
				}
			}

			final Set<IGraphNode> nodes = new HashSet<>();
			for (IGraphEdge e : typeNode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFKIND)) {
				nodes.add(e.getStartNode());
			}

			for (IGraphNode instanceNode : nodes) {
				Iterator<IGraphEdge> derived = instanceNode.getOutgoingWithType(attributeName).iterator();

				final Map<String, Object> m = new HashMap<>();
				m.put("isMany", isMany);
				m.put("isOrdered", isOrdered);
				m.put("isUnique", isUnique);
				m.put("attributetype", attributeType);
				m.put("derivationlanguage", derivationlanguage);
				m.put("derivationlogic", derivationlogic);
				m.put(DERIVED_IDXNAME_NODEPROP, String.format("%s##%s##%s", metamodelUri, typeName, attributeName));
				m.put(attributeName, DirtyDerivedFeaturesListener.NOT_YET_DERIVED_PREFIX + derivationlogic);

				if (derived.hasNext()) {
					// derived node exists -- update derived property
					IGraphNode derivedPropertyNode = derived.next().getEndNode();
					for (String s : m.keySet()) {
						derivedPropertyNode.setProperty(s, m.get(s));
					}

					derivedPropertyNodes.add(derivedPropertyNode);
				} else {
					// derived node does not exist -- create derived property
					IGraphNode derivedPropertyNode = graph.createNode(m, "derivedattribute");

					m.clear();
					m.put(DERIVED_FEATURE_EDGEPROP, true);

					graph.createRelationship(instanceNode, derivedPropertyNode, attributeName, m);

					derivedPropertyNodes.add(derivedPropertyNode);
				}

			}

			if (enableDebug) {
				LOGGER.info("{} instances found.\ncalculating derived attribute for instances...", derivedPropertyNodes.size());
			}

			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		// derive the new property
		try {
			processDerivedFeatureNodes(derivationlanguage, derivedPropertyNodes, derivedPropertyNodes.size());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		LOGGER.info("Finished adding derived feature in {}s", (System.currentTimeMillis() - startMillis) / 1000.0);
	}

	public void updateIndexedAttribute(String metamodeluri, String typename, String attributename) {

		try (IGraphTransaction tx = graph.beginTransaction()) {
			// operations on the graph
			// ...

			IGraphNodeIndex i = graph.getOrCreateNodeIndex(metamodeluri + "##" + typename + "##" + attributename);

			IGraphNode typeNode = null;

			for (IGraphEdge r : graph.getMetamodelIndex().get("id", metamodeluri).getSingle()
					.getIncomingWithType("epackage")) {

				IGraphNode othernode = r.getStartNode();
				if (othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(typename)) {
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
				isPrimitiveOrWrapperType = GraphUtil.isPrimitiveOrWrapperType(c);
			} catch (Exception e) {
				//
				e.printStackTrace();
			}

			final Set<IGraphNode> nodes = new HashSet<>();
			for (IGraphEdge e : typeNode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFKIND)) {
				nodes.add(e.getStartNode());
			}

			for (IGraphNode node : nodes) {
				Map<String, Object> m = new HashMap<>();

				final Object value = node.getProperty(attributename);
				if (!"t".equals(metadata[1])) {
					if (isPrimitiveOrWrapperType) {
						m.put(attributename, value);
					} else if (value instanceof Date) {
						m.put(attributename, formatDate((Date) value));
					} else {
						m.put(attributename, value.toString());
					}
				} else {
					Collection<Object> collection = null;

					if ("t".equals(metadata[3]))
						collection = new LinkedHashSet<Object>();
					else
						collection = new LinkedList<Object>();

					for (Object o : (Collection<?>) value) {
						if (isPrimitiveOrWrapperType) {
							collection.add(o);
						} else if (o instanceof Date) {
							collection.add(formatDate((Date)o));
						} else {
							collection.add(o.toString());
						}
					}

					if (collection.size() > 0) {
						Object r = Array.newInstance(c, collection.size());
						Object ret = collection.toArray((Object[]) r);
						m.put(attributename, ret);
					}
				}

				i.add(node, m);

			}

			tx.success();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected Object formatDate(final Date value) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		return sdf.format(value);
	}

}
