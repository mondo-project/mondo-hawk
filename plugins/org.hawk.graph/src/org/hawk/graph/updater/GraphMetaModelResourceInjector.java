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
 *     Antonio Garcia-Dominguez - protect against null EPackage nsURIs
 ******************************************************************************/
package org.hawk.graph.updater;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkDataType;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.runtime.CompositeGraphChangeListener;
import org.hawk.graph.FileNode;
import org.hawk.graph.ModelElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphMetaModelResourceInjector {
	private static final Logger LOGGER = LoggerFactory.getLogger(GraphMetaModelResourceInjector.class);

	// integer array containing the current number of added elements:
	// (element,((ofType)M->MM)reference,((ofKind)M->MM)reference,(unset(M->M))reference)
	private int objectCount = 0;
	private int unset;

	private IModelIndexer hawk;
	private IGraphDatabase graph;
	private IGraphNodeIndex epackagedictionary;

	private final long startTime = System.nanoTime();
	private final HashSet<IHawkPackage> addedepackages = new HashSet<>();
	private final CompositeGraphChangeListener listener;

	public GraphMetaModelResourceInjector(IModelIndexer hawk, Set<IHawkMetaModelResource> set,
			CompositeGraphChangeListener listener) throws Exception {
		this.hawk = hawk;
		this.graph = hawk.getGraph();
		this.listener = listener;

		LOGGER.info("ADDING METAMODELS: ");
		LOGGER.info("ADDING: ");
		int nodes = insertMetamodels(set);
		LOGGER.info("{} METAMODEL NODES! (took ~{} sec)", nodes, (System.nanoTime() - startTime) / 1_000_000_000);
	}

	public GraphMetaModelResourceInjector(IModelIndexer hawk, CompositeGraphChangeListener listener) {
		this.hawk = hawk;
		this.graph = hawk.getGraph();
		this.listener = listener;
	}

	/**
	 * Removes all the metamodel resources and their dependent model elements from the index.
	 */
	public void removeMetamodels(Set<IHawkMetaModelResource> set) {

		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.changeStart();
			epackagedictionary = graph.getMetamodelIndex();
			Set<IGraphNode> epns = new HashSet<>();

			for (IHawkMetaModelResource metamodelResource : set) {
				// if (resourceset == null)
				// resourceset = metamodelResource.getResourceSet();

				Set<IHawkObject> children = metamodelResource.getAllContents();

				for (IHawkObject child : children) {

					// if (child.eIsProxy()) {
					// throw new Exception("FAILED. PROXY UNRESOLVED: "
					// + ((InternalEObject) child).eProxyURI()
					// .fragment());
					// }

					// add the element
					if (child instanceof IHawkPackage) {

						Iterator<IGraphNode> it = epackagedictionary.get("id", ((IHawkPackage) child).getNsURI())
								.iterator();

						if (!it.hasNext()) {
							LOGGER.warn("Metamodel: {} with uri: {} not indexed. Nothing happened.",
									((IHawkPackage) child).getName(), ((IHawkPackage) child).getNsURI());
						} else {
							IGraphNode epn = it.next();
							LOGGER.info("Removing metamodel: {} with uri: {}", ((IHawkPackage) child).getName(), ((IHawkPackage) child).getNsURI());
							epns.add(epn);
						}
					}
				}
			}

			removeAll(epns);

			t.success();
			listener.changeSuccess();

		} catch (Exception e1) {
			listener.changeFailure();
			LOGGER.error("Error in removing metamodels (all removal changes reverted)", e1);
		}

	}

	/**
	 * Removes all the metamodels and models dependent on the <code>epsn</code> metamodel nodes.
	 * 
	 * @return URIs of the repositories impacted by the removal.
	 */
	private Set<String> removeAll(Set<IGraphNode> epns) throws Exception {

		Set<String> affectedRepositories = new HashSet<>();

		DeletionUtils del = new DeletionUtils(graph);

		for (IGraphNode epn : epns)
			for (IGraphEdge rel : epn.getIncomingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE)) {
				LOGGER.debug("dependency from {} to {}",
						rel.getStartNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY),
						epn.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

				// delete all dependent metamodels and models
				IGraphNode depmm = rel.getStartNode();
				del.delete(rel);
				epns.add(depmm);
			}

		Set<IGraphNode> files = new HashSet<>();

		for (IGraphNode n : epns)
			files.addAll(remove(n));

		for (IGraphNode file : files)
			affectedRepositories.add(file.getProperty(FileNode.PROP_REPOSITORY).toString());

		for (IGraphNode file : files)
			del.delete(file);

		return affectedRepositories;

	}

	private Set<IGraphNode> remove(IGraphNode epn) throws Exception {

		long start = System.currentTimeMillis();

		HashSet<IGraphNode> fileNodes = new HashSet<IGraphNode>();

		try (IGraphTransaction transaction = graph.beginTransaction()) {
			LOGGER.info("Deleting nodes from metamodel: {}", epn.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

			HashSet<IGraphNode> metaModelElements = new HashSet<IGraphNode>();
			HashSet<IGraphNode> modelElements = new HashSet<IGraphNode>();

			DeletionUtils del = new DeletionUtils(graph);

			for (IGraphEdge rel : epn.getIncomingWithType("epackage")) {
				metaModelElements.add(rel.getStartNode());
				del.delete(rel);
			}

			for (IGraphEdge rel : epn.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE)) {
				del.delete(rel);
			}

			del.delete(epn);

			for (IGraphNode metamodelelement : metaModelElements) {
				for (IGraphEdge rel : metamodelelement.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFTYPE)) {
					modelElements.add(rel.getStartNode());
					del.delete(rel);
				}
			}
			for (IGraphNode metamodelelement : metaModelElements) {
				for (IGraphEdge rel : metamodelelement.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFKIND)) {
					modelElements.add(rel.getStartNode());
					del.delete(rel);
				}
			}

			for (IGraphNode metaModelElement : metaModelElements)
				del.delete(metaModelElement);

			// remove model elements and update derived attributes

			if (modelElements.size() > 0) {
				LOGGER.info("Deleting nodes from relevant models...");

				Set<IGraphNode> toBeUpdated = new HashSet<>();
				final DirtyDerivedAttributesListener l = new DirtyDerivedAttributesListener(graph);
				if (!hawk.getDerivedAttributes().isEmpty()) {
					hawk.addGraphChangeListener(l);
				}

				for (IGraphNode modelElement : modelElements) {
					Iterator<IGraphEdge> it = modelElement.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE)
							.iterator();
					if (it.hasNext()) {
						IGraphEdge e = it.next();
						fileNodes.add(e.getEndNode());
						del.delete(e);

					}

					del.dereference(modelElement, listener, null);
				}

				for (IGraphNode modelElement : modelElements) {

					del.delete(modelElement);

				}

				toBeUpdated.addAll(l.getNodesToBeUpdated());
				listener.remove(l);

				LOGGER.info("Updating any relevant derived attributes...");
				try {
					new GraphModelInserter(hawk, new TypeCache())
							.updateDerivedAttributes(hawk.getDerivedAttributeExecutionEngine(), toBeUpdated);
					toBeUpdated = new HashSet<>();
				} catch (Exception e) {
					toBeUpdated = new HashSet<>();
					LOGGER.error("Exception while updating derived attributes", e);
				}

			}

			transaction.success();
		}

		LOGGER.info("deleted all, took: {}s", (System.currentTimeMillis() - start) / 1000.0);
		return fileNodes;
	}

	/**
	 * Adds the resource to the graph according to whether it is a model or a
	 * metamodel resource
	 * 
	 * @param parseOption
	 * @param metamodelResources
	 * @param graph
	 * @return
	 */
	private int insertMetamodels(Set<IHawkMetaModelResource> metamodels) throws Exception {

		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.changeStart();
			epackagedictionary = graph.getMetamodelIndex();

			for (IHawkMetaModelResource metamodelResource : metamodels) {
				Set<IHawkObject> children = metamodelResource.getAllContents();

				for (IHawkObject child : children) {
					// add the element
					if (child instanceof IHawkPackage)
						addEPackage((IHawkPackage) child, metamodelResource);
				}
			}

			t.success();
			listener.changeSuccess();
		} catch (Throwable ex) {
			listener.changeFailure();
			throw ex;
		}

		Iterator<IHawkPackage> it = addedepackages.iterator();
		while (it.hasNext()) {
			IHawkPackage epackage = it.next();

			boolean success = false;
			try (IGraphTransaction t = graph.beginTransaction()) {
				listener.changeStart();
				success = addEClasses(epackage);

				if (success) {
					t.success();

					listener.changeSuccess();
				} else {
					it.remove();
					t.failure();
					listener.changeFailure();
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				listener.changeFailure();
			}

			if (!success) {
				try (IGraphTransaction t2 = graph.beginTransaction()) {
					IGraphNode ePackageNode = epackagedictionary.get("id", epackage.getNsURI()).iterator().next();
					new DeletionUtils(graph).delete(ePackageNode);
					t2.success();

				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}

		try (IGraphTransaction t = graph.beginTransaction()) {

			for (IHawkPackage ePackage : addedepackages) {

				IGraphNode epackagenode = ((IGraphIterable<IGraphNode>) epackagedictionary.get("id",
						ePackage.getNsURI())).getSingle();

				// add resource to package
				final String s = ePackage.getResource().getMetaModelResourceFactory().dumpPackageToString(ePackage);

				epackagenode.setProperty(IModelIndexer.METAMODEL_RESOURCE_PROPERTY, s);

			}

			t.success();
		}

		return objectCount;
	}

	private boolean addEClasses(IHawkPackage ePackage) {

		boolean success = true;

		for (IHawkClassifier child : ePackage.getClasses()) {

			if (!success)
				break;

			if (child instanceof IHawkClass) {
				success = success && addMetaClass((IHawkClass) child);
			} else if (child instanceof IHawkDataType) {
				// FIXME need to handle datatypes?
				// System.err.println("datatype! (" + child.getName() +
				// ") -- handle it.");
			} else {
				LOGGER.error("Unknown classifier: ({}): {}", child.getName(), child.getClass());
			}
		}

		return success;
	}

	private void addEPackage(IHawkPackage ePackage, IHawkMetaModelResource metamodelResource) throws IOException {

		final String uri = ePackage.getNsURI();
		if (uri == null) {
			LOGGER.warn("ePackage {} has null nsURI, ignoring", ePackage);
			return;
		}

		if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {

			Map<String, Object> map4 = new HashMap<>();
			map4.put(IModelIndexer.IDENTIFIER_PROPERTY, uri);
			map4.put(IModelIndexer.METAMODEL_TYPE_PROPERTY, metamodelResource.getMetaModelResourceFactory().getType());

			IGraphNode epackagenode = graph.createNode(new HashMap<String, Object>(), "epackage");
			for (String s : map4.keySet()) {
				epackagenode.setProperty(s, map4.get(s));
			}

			epackagedictionary.add(epackagenode, "id", uri);
			addedepackages.add(ePackage);
			listener.metamodelAddition(ePackage, epackagenode);
		} else {
			LOGGER.warn(
				"metamodel: {} ({}) already in store, updating it instead NYI -- doing nothing!",
				(ePackage).getName(), uri);

			// add to a list called changed epackages etc?
			// XXX IDEA: handle metamodel updates -- not breaking = migrate --
			// else
			// do not and warn users
		}

	}

	/**
	 * 
	 * @param eClass
	 * @return the URI ID of an eClass
	 */
	public String getEObjectId(IHawkClass eClass) {
		return eClass.getPackageNSURI() + "/" + eClass.getName();
	}

	/**
	 * Creates a node in the graph database with the given eClass's attributes
	 * in it.
	 * 
	 * @param eObject
	 * @param id
	 * @return the Node
	 */
	private boolean createEClassNode(IHawkClass eClass, String id) {

		boolean success = true;

		Map<String, Object> map = new HashMap<>();
		map.put(IModelIndexer.IDENTIFIER_PROPERTY, id);

		IGraphNode node = graph.createNode(new HashMap<String, Object>(), "eclass");

		IGraphNode metamodelNode = ((IGraphIterable<IGraphNode>) epackagedictionary.get("id", eClass.getPackageNSURI()))
				.getSingle();

		graph.createRelationship(node, metamodelNode, "epackage");

		for (IHawkClass e : eClass.getAllSuperTypes()) {
			final String uri = e.getPackageNSURI();

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				LOGGER.error(
						"EClass {} has supertype {} which is in a package not registered yet, "
						+ "reverting all changes to this package registration, please register "
						+ "package with URI {} first",
						eClass.getName(), e.getName() == null ? e.getUri() : e.getName(), uri);

				return false;
			} else {

				// dependency to package
				if (!uri.equals(eClass.getPackageNSURI())) {

					IGraphNode supertypeepackage = ((IGraphIterable<IGraphNode>) epackagedictionary.get("id", uri))
							.getSingle();

					boolean alreadythere = false;

					for (IGraphEdge r : metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE))
						if (r.getEndNode().equals(supertypeepackage))
							alreadythere = true;

					if (!metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE).iterator().hasNext() || !alreadythere) {
						LOGGER.debug("supertype dependency from {} to {}", eClass.getPackageNSURI(), uri);

						graph.createRelationship(metamodelNode, supertypeepackage,
								IModelIndexer.METAMODEL_DEPENDENCY_EDGE);
					}
				}

			}

		}

		for (IHawkAttribute e : eClass.getAllAttributes()) {
			String uri = null;
			try {
				uri = e.getType().getPackageNSURI();
			} catch (Exception ex) {
				// attribute does not have a type - derived - other errors
				uri = eClass.getPackageNSURI();
			}

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				LOGGER.error("EAttribute {} has type {} which is in a package not registered yet, "
						+ "reverting all changes to this package registration, please register package "
						+ "with URI: {} first",
						e.getName(), (e.getType().getName() == null ? e.getType().getUri() : e.getType().getName()), uri);
				return false;
			} else {
				// dependency to package
				if (!uri.equals(eClass.getPackageNSURI())) {
					IGraphNode supertypeepackage = ((IGraphIterable<IGraphNode>) epackagedictionary.get("id", uri)).getSingle();
					boolean alreadythere = false;
					for (IGraphEdge r : metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE)) {
						if (r.getEndNode().equals(supertypeepackage)) {
							alreadythere = true;
						}
					}

					if (!metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE).iterator().hasNext() || !alreadythere) {
						LOGGER.debug("attribute dependency from {} to {}", eClass.getPackageNSURI(), uri);
						graph.createRelationship(metamodelNode, supertypeepackage, IModelIndexer.METAMODEL_DEPENDENCY_EDGE);
					}
				}
			}

			String[] metadata = new String[6];
			metadata[0] = "a";
			metadata[1] = (e.isMany() ? "t" : "f");
			metadata[2] = (e.isOrdered() ? "t" : "f");
			metadata[3] = (e.isUnique() ? "t" : "f");
			if (e.getType() != null) {
				if (e.getType().getName() != null) {
					// System.err.println(e.getType().getName());
					metadata[4] = e.getType().getInstanceType();
				} else {
					LOGGER.warn(
						"warning: unknown (null) type NAME found in metamodel parsing into db for attribute: {} of type: {}",
						e.getName(), e.getType());
					metadata[4] = "unknown";
				}
			} else {
				LOGGER.warn("warning: unknown (null) type found in metamodel parsing into db for attribute: {}", e.getName());
				metadata[4] = "unknown";
			}

			// isIndexed
			metadata[5] = "f";
			map.put(e.getName(), metadata);
		}

		for (IHawkReference r : eClass.getAllReferences()) {
			final String uri = r.getType().getPackageNSURI();

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				LOGGER.error(
					"EReference {} has type {} which is in a package not registered yet, reverting all changes "
					+ "to this package registration, please register package with uri: {} first",
					r.getName(), (r.getType().getName() == null ? r.getType().getUri() : r.getType().getName()), uri);
				return false;
			} else {
				// dependency to package
				if (!uri.equals(eClass.getPackageNSURI())) {
					IGraphNode supertypeepackage = ((IGraphIterable<IGraphNode>) epackagedictionary.get("id", uri)).getSingle();

					boolean alreadythere = false;
					for (IGraphEdge rr : metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE)) {
						if (rr.getEndNode().equals(supertypeepackage)) {
							alreadythere = true;
						}
					}

					if (!metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE).iterator().hasNext() || !alreadythere) {
						LOGGER.debug("reference dependency from {} to {}", eClass.getPackageNSURI(), uri);
						graph.createRelationship(metamodelNode, supertypeepackage,
								IModelIndexer.METAMODEL_DEPENDENCY_EDGE);
					}
				}
			}

			String[] metadata = new String[6];
			/*
			 * XXX if the property is already there, this means that the
			 * metamodel supports having a name being both a reference and
			 * attribute (aka mixed mode)
			 */
			metadata[0] = map.containsKey(r.getName()) ? "m" : "r";
			metadata[1] = (r.isMany() ? "t" : "f");
			metadata[2] = (r.isOrdered() ? "t" : "f");
			metadata[3] = (r.isUnique() ? "t" : "f");
			if (r.getType() != null) {
				if (r.getType().getName() != null) {
					// System.err.println(e.getType().getName());
					metadata[4] = r.getType().getInstanceType();
				} else {
					LOGGER.warn(
						"Unknown (null) type NAME found in metamodel parsing into db for attribute: {} of type: {}",
						r.getName(), r.getType());
					metadata[4] = "unknown";
				}
			} else {
				LOGGER.warn("Unknown (null) type found in metamodel parsing into db for attribute: {}", r.getName());
				metadata[4] = "unknown";
			}
			metadata[5] = "f";

			map.put(r.getName(), metadata);
		}

		for (String s : map.keySet()) {
			node.setProperty(s, map.get(s));
		}

		listener.classAddition(eClass, node);

		return success;
	}

	/**
	 * Creates a node with the eClass parameters, adds it to the metatracker and
	 * to the metacdictionary index
	 * 
	 * @param eClass
	 */
	private boolean addMetaClass(IHawkClass eClass) {
		String id = eClass.getName();
		objectCount++;
		return createEClassNode(eClass, id);
	}

	public int getUnset() {
		return unset;
	}

	public void setUnset(int unset) {
		this.unset = unset;
	}

	/**
	 * Adds a new derived attribute to a type. Returns whether propagation to
	 * current instances of this type is necessary.
	 * 
	 * @param metamodeluri
	 * @param typename
	 * @param attributename
	 * @param isMany
	 * @param isOrdered
	 * @param isUnique
	 * @param attributetype
	 * @param derivationlanguage
	 * @param derivationlogic
	 * @param graph
	 * @return
	 */
	public static boolean addDerivedAttribute(String metamodeluri, String typename, String attributename,
			boolean isMany, boolean isOrdered, boolean isUnique, String attributetype, String derivationlanguage,
			String derivationlogic, IGraphDatabase graph, IGraphChangeListener listener) {

		boolean requiresPropagationToInstances = false;

		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.changeStart();
			IGraphIterable<IGraphNode> ep = graph.getMetamodelIndex().get("id", metamodeluri);

			IGraphNode packagenode = null;

			if (ep.size() == 1) {
				packagenode = ep.getSingle();
			} else {
				throw new Exception("metamodel not found:" + metamodeluri);
			}

			IGraphNode typenode = null;

			for (IGraphEdge e : packagenode.getIncomingWithType("epackage")) {
				if (e.getStartNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(typename)) {
					typenode = e.getStartNode();
					break;
				}
			}

			if (typenode == null) {
				LOGGER.error("type: {} in: {} does not exist, aborting operation: addDerivedAttribute", typename, metamodeluri);
			} else {
				/*
				 * at least one instance already present so derived attribute
				 * needs to be reconfigured for each element already present
				 * (whether the derived attribute is new or existed already and
				 * is being updated)
				 */
				if (typenode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator().hasNext()
						|| typenode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFKIND).iterator().hasNext())
					requiresPropagationToInstances = true;

				String[] metadata = new String[7];
				metadata[0] = "d";
				metadata[1] = (isMany ? "t" : "f");
				metadata[2] = (isOrdered ? "t" : "f");
				metadata[3] = (isUnique ? "t" : "f");
				metadata[4] = attributetype;
				metadata[5] = derivationlanguage;
				metadata[6] = derivationlogic;

				if (typenode.getProperty(attributename) != null) {
					LOGGER.warn("Attribute already derived, nothing happened!");
					requiresPropagationToInstances = false;
				} else {
					typenode.setProperty(attributename, metadata);
					if (LOGGER.isInfoEnabled()) {
						final String logic = (derivationlogic.length() > 100
								? derivationlogic.substring(0, 100) + "\n[! long script, snipped !]"
								: derivationlogic);

						LOGGER.info(
								"Derived attribute added: {}::{}#{} (isMany={}|isOrdered={}|isUnique={}|type={}) {}#\n{}",
								metamodeluri, typename, attributename,
								isMany, isOrdered, isUnique, attributetype, derivationlanguage,
								logic);
					}
				}

				/** Must create the empty index so it will be known by the {@link ModelIndexerImpl} */
				graph.getOrCreateNodeIndex(metamodeluri + "##" + typename + "##" + attributename);
			}
			t.success();
			listener.changeSuccess();
		} catch (Exception e1) {
			LOGGER.error("error in adding a derived attribute to the metamodel", e1);
			listener.changeFailure();
		}

		return requiresPropagationToInstances;
	}

	/**
	 * Adds a new indexed attribute to a type. Returns whether propagation to
	 * current instances of this type is necessary.
	 * 
	 * @param metamodeluri
	 * @param typename
	 * @param attributename
	 * @param graph
	 * @param listener2
	 * @return
	 */
	public static boolean addIndexedAttribute(String metamodeluri, String typename, String attributename,
			IGraphDatabase graph, IGraphChangeListener listener) {

		boolean requiresPropagationToInstances = false;

		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.changeStart();
			IGraphNode packagenode = graph.getMetamodelIndex().get("id", metamodeluri).getSingle();

			IGraphNode typenode = null;

			for (IGraphEdge e : packagenode.getIncomingWithType("epackage")) {
				if (e.getStartNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(typename)) {
					typenode = e.getStartNode();
					break;
				}
			}

			if (typenode == null) {
				LOGGER.error("type: {} in: {} does not exist, aborting operation: addIndexedAttribute", typename, metamodeluri);
			} else {

				// at least one instance already present so indexed attribute
				// needs
				// to be reconfigured for each element already present (whether
				// the
				// indexed attribute is new or existed already and is being
				// updated)

				String[] metadata = (String[]) typenode.getProperty(attributename);

				if (metadata == null) {
					LOGGER.error("attribute: {} in: {}#{} does not exist, aborting operation: addIndexedAttribute", attributename, metamodeluri, typename);
				} else if (!metadata[0].equals("a")) {
					LOGGER.error("{}#{} is not an attribute, aborting operation: addIndexedAttribute", metamodeluri, typename);
				} else {
					if (typenode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator().hasNext()
							|| typenode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFKIND).iterator().hasNext())
						requiresPropagationToInstances = true;

					if (metadata.length == 6) {
						if ("t".equals(metadata[5])) {
							LOGGER.warn("attribute already indexed, nothing happened!");
							requiresPropagationToInstances = false;
						} else {
							metadata[5] = "t";
							typenode.setProperty(attributename, metadata);
							//
							graph.getOrCreateNodeIndex(metamodeluri + "##" + typename + "##" + attributename);
							//
							LOGGER.info("indexed attribute added: {}::{}#{}", metamodeluri, typename, attributename);
						}
					} else if (metadata.length == 7) {
						LOGGER.warn("derived attributes are already indexed, nothing happened.");
					} else {
						// FIXME log actual exception
						LOGGER.error("unknown exception in addIndexedAttribute of GraphMetamodelResourceInjector");
					}
				}
			}
			t.success();
			listener.changeSuccess();
		} catch (Exception e) {
			LOGGER.error("Error in adding an indexed attribute", e);
			listener.changeFailure();
		}

		return requiresPropagationToInstances;

	}

	/**
	 * Removes all the metamodels with the specified URIs.
	 * 
	 * @return URIs of the repositories impacted by the removal.
	 */
	public Set<String> removeMetamodels(String[] mmuris) {

		Set<String> ret = new HashSet<>();

		try (IGraphTransaction t = graph.beginTransaction()) {

			Set<String> indexNames = graph.getNodeIndexNames();
			Set<IGraphNodeIndex> markedForRemoval = new HashSet<>();

			listener.changeStart();
			epackagedictionary = graph.getMetamodelIndex();

			Set<IGraphNode> epns = new HashSet<>();
			for (String mmuri : mmuris) {
				try {

					for (Iterator<String> it = indexNames.iterator(); it.hasNext();) {
						String s = it.next();
						if (s.startsWith(mmuri + "##")) {
							IGraphNodeIndex i = graph.getOrCreateNodeIndex(s);
							markedForRemoval.add(i);
							it.remove();
						}
					}

					epns.add(epackagedictionary.get("id", mmuri).getSingle());
				} catch (Exception e) {
					LOGGER.error("Metamodel with URI " + mmuri + " not indexed. Nothing happened.", e);
				}
			}
			if (epns.size() > 0) {
				LOGGER.info("Removing metamodels with URIs {}", Arrays.toString(mmuris));
				ret = removeAll(epns);
			}

			for (IGraphNodeIndex i : markedForRemoval) {
				LOGGER.info("Deleting index {} as its metamodel was removed.", i.getName());
				i.delete();
			}

			t.success();
			listener.changeSuccess();
		} catch (Exception e) {
			listener.changeFailure();
			LOGGER.error("Error in removing metamodels " + Arrays.toString(mmuris) + "\n(ALL removal changes reverted)", e);
		}
		return ret;
	}

	public static boolean removeIndexedAttribute(String metamodelUri, String typename, String attributename,
			IGraphDatabase graph, CompositeGraphChangeListener listener) {

		boolean found = false;

		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.changeStart();

			IGraphNode packagenode = null;
			IGraphIterable<IGraphNode> ep = graph.getMetamodelIndex().get("id", metamodelUri);

			if (ep.size() == 1) {
				packagenode = ep.getSingle();
			} else {
				throw new Exception("metamodel not found:" + metamodelUri);
			}

			IGraphNode typenode = null;

			for (IGraphEdge e : packagenode.getIncomingWithType("epackage")) {
				if (e.getStartNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(typename)) {
					typenode = e.getStartNode();
					break;
				}
			}

			if (typenode == null) {
				LOGGER.error(
					"type: {} in: {} does not exist, aborting operation: removeIndexedAttribute",
					typename, metamodelUri
				);
				listener.changeFailure();
			} else {
				String[] metadata = (String[]) typenode.getProperty(attributename);

				if (metadata == null) {
					LOGGER.error("attribute: {} in: {}::{} does not exist, aborting operation: removeIndexedAttribute",
							attributename, metamodelUri, typename);
					listener.changeFailure();
				} else if (!metadata[0].equals("a")) {
					// System.err.println(Arrays.toString(metadata));
					LOGGER.error("{}::{} is a reference not an attribute, aborting operation: removeIndexedAttribute",
							metamodelUri, typename);
					listener.changeFailure();
				} else {

					if (metadata.length == 6) {
						if ("t".equals(metadata[5])) {
							metadata[5] = "f";
							typenode.setProperty(attributename, metadata);
							//
							String indexname = metamodelUri + "##" + typename + "##" + attributename;

							if (graph.nodeIndexExists(indexname)) {
								graph.getOrCreateNodeIndex(indexname).delete();
								found = true;
							}

							t.success();
							listener.changeSuccess();
						} else {
							LOGGER.error("attribute was not indexed, nothing happened!");
							listener.changeFailure();
						}

					} else {
						LOGGER.error("error in removeIndexedAttribute (metadata.length!=6), nothing happened!");
						listener.changeFailure();
					}
				}

			}

		} catch (Exception e) {
			LOGGER.error("Error while removing an indexed attribute", e);
			listener.changeFailure();
		}
		return found;

	}

	public static boolean removeDerivedAttribute(String metamodelUri, String typeName, String attributeName,
			IGraphDatabase graph, CompositeGraphChangeListener listener) {

		boolean found = false;

		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.changeStart();
			IGraphIterable<IGraphNode> ep = graph.getMetamodelIndex().get("id", metamodelUri);

			IGraphNode packagenode = null;

			if (ep.size() == 1) {
				packagenode = ep.getSingle();
			} else {
				throw new Exception("metamodel not found:" + metamodelUri);
			}

			IGraphNode typenode = null;

			for (IGraphEdge e : packagenode.getIncomingWithType("epackage")) {
				if (e.getStartNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(typeName)) {
					typenode = e.getStartNode();
					break;
				}
			}

			if (typenode == null) {
				LOGGER.error(
					"type: {} in: {} does not exist, aborting operation: removeDerivedAttribute",
					typeName, metamodelUri);
				listener.changeFailure();
			} else {

				String[] metadata = (String[]) typenode.getProperty(attributeName);
				if (metadata != null) {

					if (metadata.length == 7 && metadata[0].equals("d")) {
						LOGGER.info("derived attribute removed: {}::{}", metamodelUri, typeName);
						IGraphNodeIndex derivedAccessDictionary = graph.getOrCreateNodeIndex("derivedaccessdictionary");
						IGraphNodeIndex derivedProxyDictionary = graph.getOrCreateNodeIndex("derivedproxydictionary");

						typenode.removeProperty(attributeName);
						graph.getOrCreateNodeIndex(metamodelUri + "##" + typeName + "##" + attributeName).delete();

						boolean noerror = true;

						for (IGraphEdge e : (typenode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFTYPE)))
							noerror = noerror && removeDerivedAttribute(derivedAccessDictionary, derivedProxyDictionary,
									attributeName, e);

						for (IGraphEdge e : (typenode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFKIND)))
							noerror = noerror && removeDerivedAttribute(derivedAccessDictionary, derivedProxyDictionary,
									attributeName, e);

						if (noerror)
							found = true;

						t.success();
						listener.changeSuccess();
					} else {
						LOGGER.error("Error in removeDerivedAttribute, attribute metadata not valid");
						listener.changeFailure();
					}
				} else {
					LOGGER.error("Attribute was not already derived, nothing happened!");
					listener.changeFailure();
				}
			}

		} catch (Exception e1) {
			LOGGER.error("Error in removing a derived attribute to the metamodel", e1);
			listener.changeFailure();
		}

		return found;

	}

	/**
	 * Internal, to remove instance derived attributes
	 */
	private static boolean removeDerivedAttribute(IGraphNodeIndex derivedAccessDictionary,
			IGraphNodeIndex derivedProxyDictionary, String attributeName, IGraphEdge instanceToTypeEdge) {

		boolean error = true;

		IGraphNode n = instanceToTypeEdge.getStartNode();

		IGraphEdge dae = null;

		for (IGraphEdge ed : n.getOutgoingWithType(attributeName)) {
			if (dae == null)
				dae = ed;
			else {
				LOGGER.error("multiple edges found for derived attribute: {} in node {}", attributeName, n);
				dae = null;
				break;
			}
		}

		if (dae == null) {
			LOGGER.error("derived attribute ({}) not found for node {}", attributeName, n);
		} else {
			IGraphNode dan = dae.getEndNode();
			dae.delete();
			derivedAccessDictionary.remove(dan);
			derivedProxyDictionary.remove(dan);
			dan.delete();
			error = false;
		}

		return !error;

	}
}
