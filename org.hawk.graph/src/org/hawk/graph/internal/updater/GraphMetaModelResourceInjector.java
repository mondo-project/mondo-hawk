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
import org.hawk.graph.ModelElementNode;

public class GraphMetaModelResourceInjector {

	// integer array containing the current number of added elements:
	// (element,((ofType)M->MM)reference,((ofKind)M->MM)reference,(unset(M->M))reference)
	private int objectCount = 0;
	private int unset;

	private IGraphDatabase graph;
	private IGraphNodeIndex epackagedictionary;

	private final long startTime = System.nanoTime();
	private final HashSet<IHawkPackage> addedepackages = new HashSet<>();
	private final IGraphChangeListener listener;

	public GraphMetaModelResourceInjector(IGraphDatabase database, Set<IHawkMetaModelResource> set,
			IGraphChangeListener listener) {
		this.graph = database;
		this.listener = listener;

		try {
			System.out.println("ADDING METAMODELS: ");
			System.out.print("ADDING: ");
			System.out.println(parseResource(set) + " METAMODEL NODES! (took ~"
					+ (System.nanoTime() - startTime) / 1000000000 + "sec)");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public GraphMetaModelResourceInjector(IGraphDatabase database, IGraphChangeListener listener) {
		this.graph = database;
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

	private void removeAll(Set<IGraphNode> epns) throws Exception {
		DeletionUtils del = new DeletionUtils(graph);

		for (IGraphNode epn : epns)
			for (IGraphEdge rel : epn.getIncomingWithType("dependency")) {
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
			del.delete(file);

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

			for (IGraphEdge rel : epn.getOutgoingWithType("dependency")) {
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

			//
			for (IGraphNode modelElement : modelElements) {
				Iterator<IGraphEdge> it = modelElement.getOutgoingWithType(ModelElementNode.EDGE_LABEL_FILE).iterator();
				if (it.hasNext()) {
					IGraphEdge e = it.next();
					fileNodes.add(e.getEndNode());
					del.delete(e);

				}

				del.dereference(modelElement);
			}

			for (IGraphNode modelElement : modelElements)
				del.delete(modelElement);

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

		// metamodelResources = metamodels;

		// EObject[] contents = (EObject[]) resource.getContents().toArray();

		// for (EObject content : contents) {
		// List<EObject> children = new LinkedList<EObject>();
		// children.add(content);

		// while (!children.isEmpty()) {
		// EObject child = children.remove(0);

		// children.addAll(0, child.eContents());

		try (IGraphTransaction t = graph.beginTransaction()) {
			listener.changeStart();
			epackagedictionary = graph.getMetamodelIndex();

			for (IHawkMetaModelResource metamodelResource : metamodels) {

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
					if (child instanceof IHawkPackage)
						addEPackage((IHawkPackage) child, metamodelResource);
					// )
					// break;

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

			try (IGraphTransaction t = graph.beginTransaction()) {
				listener.changeStart();
				final boolean success = addEClasses(epackage);

				if (success) {
					t.success();
					t.close();
					listener.changeSuccess();
				} else {
					it.remove();
					t.failure();
					t.close();
					listener.changeFailure();
					try (IGraphTransaction t2 = graph.beginTransaction()) {
						IGraphNode ePackageNode = epackagedictionary.get("id", epackage.getNsURI()).iterator().next();

						new DeletionUtils(graph).delete(ePackageNode);

						t2.success();

					} catch (Exception e) {
						System.err.println("e1");
						e.printStackTrace();
					}
				}

			} catch (Exception e) {
				System.err.println("e2");
				e.printStackTrace();
				listener.changeFailure();
			}

		}

		try (IGraphTransaction t = graph.beginTransaction()) {

			HashMap<IGraphNode, IHawkMetaModelResource> map = new HashMap<>();

			for (IHawkPackage ePackage : addedepackages) {

				IGraphNode epackagenode = ((IGraphIterable<IGraphNode>) epackagedictionary.get("id",
						ePackage.getNsURI())).getSingle();

				// add resource to package
				final String s = ePackage.getResource().getMetaModelResourceFactory().dumpPackageToString(ePackage);

				epackagenode.setProperty("resource", s);

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
			map4.put("type", metamodelResource.getMetaModelResourceFactory().getType());

			IGraphNode epackagenode = graph.createNode(new HashMap<String, Object>(), "epackage");

			listener.metamodelAddition(ePackage, epackagenode);

			for (String s : map4.keySet()) {
				epackagenode.setProperty(s, map4.get(s));
			}

			epackagedictionary.add(epackagenode, "id", uri);
			addedepackages.add(ePackage);
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

		listener.classAddition(eClass, node);

		// hash.put(eClass, node);

		IGraphNode node2 = ((IGraphIterable<IGraphNode>) epackagedictionary.get("id", eClass.getPackageNSURI()))
				.getSingle();

		// System.out.println(new
		// ToString().toString(epackagedictionary.query("id","*")));

		graph.createRelationship(node, node2, "epackage");

		for (IHawkClass e : eClass.getSuperTypes()) {

			String uri = null;

			if (e.isProxy())
				uri = e.getUri()
						// ((InternalEObject) e)
						// .eProxyURI()
						// .toString()
						.substring(0,
								e.getUri()
										// ((InternalEObject)
										// e).eProxyURI().toString()
										.indexOf("#"));

			else
				uri = e.getPackageNSURI();

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

					for (IGraphEdge r : node2.getOutgoingWithType("dependency"))
						if (r.getEndNode().equals(supertypeepackage))
							alreadythere = true;

					if (!node2.getOutgoingWithType("dependency").iterator().hasNext() || !alreadythere) {

						System.err.println("supertype dependency from " + eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(node2, supertypeepackage, "dependency");

					}
				}

			}

		}

		// System.err.println(eClass.getName()+":");
		for (IHawkAttribute e : eClass.getAllAttributes()) {

			String uri = null;

			if (e.isProxy())

				uri = e.getUri()
						// ((InternalEObject) e)
						// .eProxyURI()
						// .toString()
						.substring(0,
								// ((InternalEObject) e).eProxyURI().toString()
								e.getUri().indexOf("#"));

			else

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

					for (IGraphEdge r : node2.getOutgoingWithType("dependency"))
						if (r.getEndNode().equals(supertypeepackage))
							alreadythere = true;

					if (!node2.getOutgoingWithType("dependency").iterator().hasNext() || !alreadythere) {

						System.err.println("attribute dependency from " + eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(node2, supertypeepackage, "dependency");

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

			String uri = null;

			if (r.isProxy())
				uri = r.getUriFragment()
						// ((InternalEObject) r)
						// .eProxyURI()
						// .toString()
						.substring(0,
								r.getUriFragment()
										// ((InternalEObject)
										// r).eProxyURI().toString()
										.indexOf("#"));

			else
				uri = r.getType().getPackageNSURI();

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

					for (IGraphEdge rr : node2.getOutgoingWithType("dependency"))
						if (rr.getEndNode().equals(supertypeepackage))
							alreadythere = true;

					if (!node2.getOutgoingWithType("dependency").iterator().hasNext() || !alreadythere) {

						System.err.println("reference dependency from " + eClass.getPackageNSURI() + " to " + uri);

						graph.createRelationship(node2, supertypeepackage, "dependency");

					}
				}

			}

			// System.err.println("r : "+r.getName());

			String[] metadata = new String[5];
			metadata[0] = "r";
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
			// metadata[4] = r.getType().getName();

			map.put(r.getName(), metadata);

			// map.put(r.getName(),"r."+(r.isMany()?"t":"f")+"."+(r.isOrdered()?"t":"f")+"."+(r.isUnique()?"t":"f"));

		}

		// n.field("nUri", ((EClass)child).getEPackage()
		// .getNsURI());

		// System.err.println(">>>"+map.keySet());

		for (String s : map.keySet()) {
			node.setProperty(s, map.get(s));
		}

		// System.err.println(node.getPropertyKeys());

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
							+ attributetype + ") " + derivationlanguage + " # " + derivationlogic);
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

}
