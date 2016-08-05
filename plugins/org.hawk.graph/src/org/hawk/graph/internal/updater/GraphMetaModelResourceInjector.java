/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - protect against null EPackage nsURIs
 ******************************************************************************/
package org.hawk.graph.internal.updater;

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

public class GraphMetaModelResourceInjector {

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

		// try {
		System.out.println("ADDING METAMODELS: ");
		System.out.print("ADDING: ");
		System.out.println(parseResource(set) + " METAMODEL NODES! (took ~"
				+ (System.nanoTime() - startTime) / 1000000000 + "sec)");
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	public GraphMetaModelResourceInjector(IModelIndexer hawk, CompositeGraphChangeListener listener) {
		this.hawk = hawk;
		this.graph = hawk.getGraph();
		this.listener = listener;
	}

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

							System.err.println("Metamodel: " + ((IHawkPackage) child).getName() + " with uri: "
									+ ((IHawkPackage) child).getNsURI() + " not indexed. Nothing happened.");

						} else {

							IGraphNode epn = it.next();

							System.err.println("Removing metamodel: " + ((IHawkPackage) child).getName() + " with uri: "
									+ ((IHawkPackage) child).getNsURI());

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
			System.err.println("error in removing metamodels (ALL removal changes reverted):");
			e1.printStackTrace();
		}

	}

	private Set<String> removeAll(Set<IGraphNode> epns) throws Exception {

		Set<String> affectedRepositories = new HashSet<>();

		DeletionUtils del = new DeletionUtils(graph);

		for (IGraphNode epn : epns)
			for (IGraphEdge rel : epn.getIncomingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE)) {
				System.err
						.println("dependency from: " + rel.getStartNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
								+ " to: " + epn.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

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

			System.out.println("deleting nodes from metamodel: " + epn.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

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

				System.out.println("deleting nodes from relevant models...");

				Set<IGraphNode> toBeUpdated = new HashSet<>();
				final DirtyDerivedAttributesListener l = new DirtyDerivedAttributesListener(graph);
				listener.add(l);

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

				System.out.println("attempting to update any relevant derived attributes...");
				try {
					new GraphModelInserter(hawk, new TypeCache())
							.updateDerivedAttributes(hawk.getDerivedAttributeExecutionEngine(), toBeUpdated);
					toBeUpdated = new HashSet<>();
				} catch (Exception e) {
					toBeUpdated = new HashSet<>();
					System.err.println("Exception in updateStore - UPDATING DERIVED attributes");
					System.err.println(e);
				}

			}
			//

			transaction.success();
		}

		System.out.println("deleted all, took: " + (System.currentTimeMillis() - start) / 1000 + "s"
				+ (System.currentTimeMillis() - start) / 1000 + "ms");

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
	private int parseResource(Set<IHawkMetaModelResource> metamodels) throws Exception {

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
				System.err.println("e1");
				e.printStackTrace();
				listener.changeFailure();
			}

			if (!success) {
				try (IGraphTransaction t2 = graph.beginTransaction()) {
					IGraphNode ePackageNode = epackagedictionary.get("id", epackage.getNsURI()).iterator().next();
					new DeletionUtils(graph).delete(ePackageNode);
					t2.success();

				} catch (Exception e) {
					System.err.println("e2");
					e.printStackTrace();
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

			if (child instanceof IHawkClass)
				success = success && addMetaClass((IHawkClass) child);

			else if (child instanceof IHawkDataType) {
				// FIXME need to handle datatypes?
				// System.err.println("datatype! (" + child.getName() +
				// ") -- handle it.");
			}

			else
				System.err.println("unknown classifier: (" + child.getName() + "): " + child.getClass());

		}

		return success;

	}

	private void addEPackage(IHawkPackage ePackage, IHawkMetaModelResource metamodelResource) throws IOException {

		final String uri = ePackage.getNsURI();
		if (uri == null) {
			System.err.println("WARNING: ePackage " + ePackage + " has null nsURI, ignoring");
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
			System.err.println("metamodel: " + (ePackage).getName() + " (" + uri
					+ ") already in store, updating it instead (NYI) -- doing nothing!");
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

		// System.out.println(new
		// ToString().toString(epackagedictionary.query("id","*")));

		graph.createRelationship(node, metamodelNode, "epackage");

		for (IHawkClass e : eClass.getSuperTypes()) {
			final String uri = e.getPackageNSURI();

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				System.err.println("EClass " + eClass.getName() + "has supertype "
						+ (e.getName() == null ? e.getUri() : e.getName())
						+ " which is in a package not registered yet, reverting all changes to this package registration, please register package with uri: "
						+ uri + " first");
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

					if (!metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE).iterator().hasNext()
							|| !alreadythere) {

						System.err.println("supertype dependency from " + eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(metamodelNode, supertypeepackage,
								IModelIndexer.METAMODEL_DEPENDENCY_EDGE);

					}
				}

			}

		}

		// System.err.println(eClass.getName()+":");
		for (IHawkAttribute e : eClass.getAllAttributes()) {
			String uri = null;
			try {
				uri = e.getType().getPackageNSURI();
			} catch (Exception ex) {
				// attribute does not have a type - derived - other errors
				uri = eClass.getPackageNSURI();
			}

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				System.err.println("EAttribute " + e.getName() + " has type "
						+ (e.getType().getName() == null ? e.getType().getUri() : e.getType().getName())
						+ " which is in a package not registered yet, reverting all changes to this package registration, please register package with uri: "
						+ uri + " first");
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

					if (!metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE).iterator().hasNext()
							|| !alreadythere) {

						System.err.println("attribute dependency from " + eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(metamodelNode, supertypeepackage,
								IModelIndexer.METAMODEL_DEPENDENCY_EDGE);

					}
				}

			}

			// System.err.println("a : " + e.getName());

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
					System.err.println(
							"warning: unknown (null) type NAME found in metamodel parsing into db for attribute: "
									+ e.getName() + "of type: " + e.getType());
					metadata[4] = "unknown";
				}
			} else {
				System.err.println("warning: unknown (null) type found in metamodel parsing into db for attribute: "
						+ e.getName());
				metadata[4] = "unknown";
			}
			// isIndexed
			metadata[5] = "f";
			map.put(e.getName(), metadata);

			// "a."+(e.isMany()?"t":"f")+"."+(e.isOrdered()?"t":"f")+"."+(e.isUnique()?"t":"f")

		}

		for (IHawkReference r : eClass.getAllReferences()) {
			final String uri = r.getType().getPackageNSURI();

			if (epackagedictionary.get("id", uri).iterator().hasNext() == false) {
				System.err.println("EReference " + r.getName() + " has type "
						+ (r.getType().getName() == null ? r.getType().getUri() : r.getType().getName())
						+ " which is in a package not registered yet, reverting all changes to this package registration, please register package with uri: "
						+ uri + " first");
				return false;
			} else {

				// dependency to package
				if (!uri.equals(eClass.getPackageNSURI())) {

					IGraphNode supertypeepackage = ((IGraphIterable<IGraphNode>) epackagedictionary.get("id", uri))
							.getSingle();

					boolean alreadythere = false;

					for (IGraphEdge rr : metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE))
						if (rr.getEndNode().equals(supertypeepackage))
							alreadythere = true;

					if (!metamodelNode.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE).iterator().hasNext()
							|| !alreadythere) {

						System.err.println("reference dependency from " + eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(metamodelNode, supertypeepackage,
								IModelIndexer.METAMODEL_DEPENDENCY_EDGE);

					}
				}

			}

			// System.err.println("r : "+r.getName());

			String[] metadata = new String[6];
			// XXX if the property is already there, this means that the
			// metamodel
			// supports having a name being both a reference and attribute (aka
			// mixed mode)
			metadata[0] = map.containsKey(r.getName()) ? "m" : "r";
			metadata[1] = (r.isMany() ? "t" : "f");
			metadata[2] = (r.isOrdered() ? "t" : "f");
			metadata[3] = (r.isUnique() ? "t" : "f");
			if (r.getType() != null) {
				if (r.getType().getName() != null) {
					// System.err.println(e.getType().getName());
					metadata[4] = r.getType().getInstanceType();
				} else {
					System.err.println(
							"warning: unknown (null) type NAME found in metamodel parsing into db for attribute: "
									+ r.getName() + "of type: " + r.getType());
					metadata[4] = "unknown";
				}
			} else {
				System.err.println("warning: unknown (null) type found in metamodel parsing into db for attribute: "
						+ r.getName());
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
		String id = (eClass).getName();
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
				System.err.println("type: " + typename + " in: " + metamodeluri
						+ " does not exist, aborting operation: addDerivedAttribute");
			} else {
				// at least one instance already present so derived attribute
				// needs
				// to be reconfigured for each element already present (whether
				// the
				// derived attribute is new or existed already and is being
				// updated)
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
					System.err.println("attribute already derived, nothing happened!");
					requiresPropagationToInstances = false;
				} else {
					typenode.setProperty(attributename, metadata);
					System.err.println("derived attribute added: " + metamodeluri + ":" + typename + " " + attributename
							+ "(isMany=" + isMany + "|isOrdered=" + isOrdered + "|isUnique=" + isUnique + "|type="
							+ attributetype + ") " + derivationlanguage + " #\n"
							+ (derivationlogic.length() > 100
									? derivationlogic.substring(0, 100) + "\n[! long script, snipped !]"
									: derivationlogic));
				}
			}
			t.success();
			listener.changeSuccess();
		} catch (Exception e1) {
			System.err.println("error in adding a derived attribute to the metamodel");
			e1.printStackTrace();
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
				System.err.println("type: " + typename + " in: " + metamodeluri
						+ " does not exist, aborting operation: addIndexedAttribute");
			} else {

				// at least one instance already present so indexed attribute
				// needs
				// to be reconfigured for each element already present (whether
				// the
				// indexed attribute is new or existed already and is being
				// updated)

				String[] metadata = (String[]) typenode.getProperty(attributename);

				if (metadata == null) {
					System.err.println("attribute: " + attributename + " in: " + metamodeluri + "#" + typename
							+ " does not exist, aborting operation: addIndexedAttribute");
				} else if (!metadata[0].equals("a")) {
					// System.err.println(Arrays.toString(metadata));
					System.err.println(metamodeluri + "#" + typename
							+ " is a reference not an attribute, aborting operation: addIndexedAttribute");
				} else {

					if (typenode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator().hasNext()
							|| typenode.getIncomingWithType(ModelElementNode.EDGE_LABEL_OFKIND).iterator().hasNext())
						requiresPropagationToInstances = true;

					if (metadata.length == 6) {
						if (metadata[5] == "t") {
							System.err.println("attribute already indexed, nothing happened!");
							requiresPropagationToInstances = false;
						} else {
							metadata[5] = "t";
							typenode.setProperty(attributename, metadata);
							//
							graph.getOrCreateNodeIndex(metamodeluri + "##" + typename + "##" + attributename);
							//
							System.err.println(
									"indexed attribute added: " + metamodeluri + ":" + typename + " " + attributename);
						}
					} else if (metadata.length == 7)
						System.err.println("derived attributes are already indexed, nothing happened.");
					else
						System.err
								.println("unknown exception in addIndexedAttribute of GraphMetamodelResourceInjector");
				}
			}
			t.success();
			listener.changeSuccess();
		} catch (Exception e) {
			System.err.println("error in adding an indexed attribute:");
			e.printStackTrace();
			listener.changeFailure();
		}

		return requiresPropagationToInstances;

	}

	// FIXME: why do we have two removeMetamodels(...) methods?
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
					System.err.println("Metamodel with uri: " + mmuri + " not indexed. Nothing happened.");
					System.err.println(e.getMessage());
				}
			}
			if (epns.size() > 0) {
				System.err.println("Removing metamodels with uris: " + Arrays.toString(mmuris));
				ret = removeAll(epns);
			}

			for (IGraphNodeIndex i : markedForRemoval) {
				System.err.println("deleting index: " + i.getName() + " as its metamodel was removed.");
				i.delete();
			}

			t.success();
			listener.changeSuccess();

		} catch (Exception e) {
			listener.changeFailure();
			System.err.println(
					"error in removing metamodels: " + Arrays.toString(mmuris) + "\n(ALL removal changes reverted):");
			e.printStackTrace();

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
				System.err.println("type: " + typename + " in: " + metamodelUri
						+ " does not exist, aborting operation: removeIndexedAttribute");
				listener.changeFailure();
			} else {
				String[] metadata = (String[]) typenode.getProperty(attributename);

				if (metadata == null) {
					System.err.println("attribute: " + attributename + " in: " + metamodelUri + "#" + typename
							+ " does not exist, aborting operation: removeIndexedAttribute");
					listener.changeFailure();
				} else if (!metadata[0].equals("a")) {
					// System.err.println(Arrays.toString(metadata));
					System.err.println(metamodelUri + "#" + typename
							+ " is a reference not an attribute, aborting operation: removeIndexedAttribute");
					listener.changeFailure();
				} else {

					if (metadata.length == 6) {
						if (metadata[5] == "t") {
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
							System.err.println("attribute was not indexed, nothing happened!");
							listener.changeFailure();
						}

					} else {
						System.err.println("error in removeIndexedAttribute (metadata.length!=6), nothing happened!");
						listener.changeFailure();
					}
				}

			}

		} catch (Exception e) {
			System.err.println("error in removing an indexed attribute:");
			e.printStackTrace();
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
				System.err.println("type: " + typeName + " in: " + metamodelUri
						+ " does not exist, aborting operation: removeDerivedAttribute");
				listener.changeFailure();
			} else {

				String[] metadata = (String[]) typenode.getProperty(attributeName);
				if (metadata != null) {

					if (metadata.length == 7 && metadata[0].equals("d")) {

						System.err.println("derived attribute removed: " + metamodelUri + ":" + typeName);
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
						System.err.println("error in removeDerivedAttribute, attribute metadata not valid");
						listener.changeFailure();
					}
				} else {
					System.err.println("attribute was not already derived, nothing happened!");
					listener.changeFailure();
				}
			}

		} catch (Exception e1) {
			System.err.println("error in removing a derived attribute to the metamodel");
			e1.printStackTrace();
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
				System.err.println("multiple edges found for derived attribute: " + attributeName + " in node " + n);
				dae = null;
				break;
			}
		}

		if (dae == null)
			System.err.println("derived attribute (" + attributeName + ") not found for node " + n);
		else {
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
