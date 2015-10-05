package org.hawk.graph.syncValidationListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.runtime.ModelIndexerImpl;

public class SyncValidationListener implements IGraphChangeListener {

	ModelIndexerImpl hawk;

	public SyncValidationListener() {
		// osgi constructor
	}

	@Override
	public void setModelIndexer(IModelIndexer hawk) {
		this.hawk = (ModelIndexerImpl) hawk;
		System.err
				.println("SyncValidationListener: hawk.setSyncMetricsEnabled(true) called, performance will suffer!");
		hawk.setSyncMetricsEnabled(true);
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void synchroniseStart() {
		// System.out.println("a");

	}

	@Override
	public void synchroniseEnd() {
		validateChanges();
	}

	// clone from GraphModelUpdater.FILEINDEX_REPO_SEPARATOR as it is not
	// exported
	public static final String FILEINDEX_REPO_SEPARATOR = "////";

	@SuppressWarnings("unchecked")
	private void validateChanges() {

		System.err.println("sync metrics:");
		System.err.println("interesting\t" + hawk.getInterestingFiles());
		System.err.println("deleted\t" + hawk.getDeletedFiles());
		System.err.println("changed\t" + hawk.getCurrChangedItems());
		System.err.println("loaded\t" + hawk.getLoadedResources());
		System.err.println("time\t~" + hawk.getLatestSynctime() / 1000 + "s");

		System.err.println("validating changes...");

		boolean allValid = true;
		int totalResourceSizes = 0;
		int totalGraphSize = 0;
		// for all non-null resources
		if (hawk.getFileToResourceMap() != null)
			for (VcsCommitItem c : hawk.getFileToResourceMap().keySet()) {

				IHawkModelResource r = hawk.getFileToResourceMap().get(c);

				if (r == null) {
					// file didnt get parsed so no changes are made -- any way
					// to
					// verify this further?
				}

				else {
					System.out.println("validating file " + c.getChangeType()
							+ " for " + c.getPath());

					totalResourceSizes += r.getAllContentsSet().size();

					IGraphDatabase graph = hawk.getGraph();

					try (IGraphTransaction t = graph.beginTransaction()) {

						IGraphNode filenode = graph
								.getFileIndex()
								.get("id",
										c.getCommit().getDelta()
												.getRepository().getUrl()
												+ FILEINDEX_REPO_SEPARATOR
												+ c.getPath()).getSingle();

						Iterable<IGraphEdge> instancesEdges = filenode
								.getIncomingWithType("file");

						// cache model elements in current resource
						HashMap<String, IHawkObject> eobjectCache = new HashMap<>();
						for (IHawkObject content : r.getAllContentsSet()) {
							IHawkObject old = eobjectCache.put(
									content.getUriFragment(), content);
							if (old != null) {
								System.err.println("warning (" + c.getPath()
										+ ") eobjectCache replaced:");
								System.err.println(old.getUri() + " | "
										+ old.getUriFragment() + " | ofType: "
										+ old.getType());
								System.err.println("with:");
								System.err.println(content.getUri() + " | "
										+ content.getUriFragment()
										+ " | ofType: " + content.getType());
							}
						}

						// go through all nodes in graph from the file the
						// resource
						// is in
						for (IGraphEdge instanceEdge : instancesEdges) {
							IGraphNode instance = instanceEdge.getStartNode();
							totalGraphSize++;

							IHawkObject eobject = eobjectCache
									.get(instance
											.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

							// if a node cannot be found in the model cache
							if (eobject == null) {
								System.err
										.println("error in validating: graph contains node with identifier:"
												+ instance
														.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)
												+ " but resource does not!");
								allValid = false;
							} else {
								eobjectCache
										.remove(instance
												.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

								// cache model element attributes and references
								// by
								// name
								HashMap<String, Object> attributeCache = new HashMap<>();
								for (IHawkAttribute a : ((IHawkClass) eobject
										.getType()).getAllAttributes())
									if (eobject.isSet(a))
										attributeCache.put(a.getName(),
												eobject.get(a));

								HashMap<String, Set<String>> modelreferences = new HashMap<>();
								for (IHawkReference ref : ((IHawkClass) eobject
										.getType()).getAllReferences()) {
									if (eobject.isSet(ref)) {
										Object refval = eobject.get(ref, false);
										HashSet<String> vals = new HashSet<>();
										if (refval instanceof Iterable<?>) {
											for (Object val : ((Iterable<IHawkObject>) refval))
												// if (!((IHawkObject)
												// val).isProxy())
												vals.add(((IHawkObject) val)
														.getUriFragment());

										} else {
											// if (!((IHawkObject)
											// refval).isProxy())
											vals.add(((IHawkObject) refval)
													.getUriFragment());

										}
										if (vals.size() > 0)
											modelreferences.put(ref.getName(),
													vals);
									}
								}

								for (String propertykey : instance
										.getPropertyKeys()) {
									if (!propertykey.equals("hashCode")
											&& !propertykey
													.equals(IModelIndexer.IDENTIFIER_PROPERTY)
											&& !propertykey
													.startsWith("_proxyRef")) {
										//
										Object dbattr = instance
												.getProperty(propertykey);
										Object attr = attributeCache
												.get(propertykey);

										if (!flattenedStringEquals(dbattr, attr)) {
											allValid = false;
											System.err
													.println("error in validating, attribute: "
															+ propertykey
															+ " has values:");
											String cla1 = dbattr != null ? dbattr
													.getClass().toString()
													: "null attr";
											System.err
													.println("database:\t"
															+ (dbattr instanceof Object[] ? (Arrays
																	.asList((Object[]) dbattr))
																	: dbattr)
															+ " JAVATYPE: "
															+ cla1);
											String cla2 = attr != null ? attr
													.getClass().toString()
													: "null attr";
											System.err
													.println("model:\t\t"
															+ (attr instanceof Object[] ? (Arrays
																	.asList((Object[]) attr))
																	: attr)
															+ " JAVATYPE: "
															+ cla2);
											//
										} else
											attributeCache.remove(propertykey);
									}
								}

								if (attributeCache.size() > 0) {
									System.err
											.println("error in validating, the following attributes were not found in the graph node:");
									System.err.println(attributeCache.keySet());
									allValid = false;
								}

								HashMap<String, Set<String>> nodereferences = new HashMap<>();

								for (IGraphEdge reference : instance
										.getOutgoing()) {

									if (reference.getType().equals("file")
											|| reference.getType().equals(
													"ofType")
											|| reference.getType().equals(
													"ofKind")
											|| reference.getPropertyKeys()
													.contains("derived")) {
										// ignore
									} else {
										//
										HashSet<String> refvals = new HashSet<>();
										if (nodereferences
												.containsKey(reference
														.getType())) {
											refvals.addAll(nodereferences
													.get(reference.getType()));
										}
										refvals.add(reference
												.getEndNode()
												.getProperty(
														IModelIndexer.IDENTIFIER_PROPERTY)
												.toString());
										nodereferences.put(reference.getType(),
												refvals);
										//
									}
								}
								// compare model and graph reference maps
								Iterator<Entry<String, Set<String>>> rci = modelreferences
										.entrySet().iterator();
								while (rci.hasNext()) {
									Entry<String, Set<String>> modelRef = rci
											.next();
									String modelRefName = modelRef.getKey();

									if (!nodereferences
											.containsKey(modelRefName)) {
										// no need for this?
										// System.err.println("error in validating: reference "+
										// modelRefName+
										// " had targets in the model but none in the graph: ");
										// System.err.println(modelRef.getValue());
										// allValid = false;
									} else {
										Set<String> noderefvalues = new HashSet<>();
										noderefvalues.addAll(nodereferences
												.get(modelRefName));

										Set<String> modelrefvalues = new HashSet<>();
										modelrefvalues.addAll(modelreferences
												.get(modelRefName));

										Set<String> noderefvaluesclone = new HashSet<>();
										noderefvaluesclone
												.addAll(nodereferences
														.get(modelRefName));
										noderefvaluesclone
												.removeAll(modelrefvalues);

										Set<String> modelrefvaluesclone = new HashSet<>();
										modelrefvaluesclone
												.addAll(modelreferences
														.get(modelRefName));
										modelrefvaluesclone
												.removeAll(noderefvalues);
										modelrefvaluesclone = removeHawkProxies(
												instance, modelRefName,
												modelrefvaluesclone);

										if (noderefvaluesclone.size() > 0) {
											System.err
													.println("error in validating: reference "
															+ modelRefName);
											System.err
													.println(noderefvaluesclone);
											System.err
													.println("the above ids were found in the graph but not the model");
											allValid = false;
										}

										if (modelrefvaluesclone.size() > 0) {

											System.err
													.println("error in validating: reference "
															+ modelRefName);
											System.err
													.println(modelrefvaluesclone);
											System.err
													.println("the above ids were found in the model but not the graph");
											allValid = false;
										}

										nodereferences.remove(modelRefName);
										// rci.remove();
									}
								}

								if (nodereferences.size() > 0) {
									System.err
											.println("error in validating: references "
													+ nodereferences.keySet()
													+ " had targets in the graph but not in the model: ");
									System.err.println(nodereferences);
									allValid = false;
								}

							}
						}
						// if there are model elements not found in nodes
						if (eobjectCache.size() > 0) {
							System.err
									.println("error in validating: the following objects were not found in the graph:");
							System.err.println(eobjectCache.keySet());
							allValid = false;
						}

						//

						t.success();
					} catch (Exception e) {
						System.err
								.println("syncChangeListener transaction error:");
						e.printStackTrace();
					}

				}

			}
		// else
		// System.err
		// .println("filetoresourcemap was empty -- maybe a metamodel addition happened?");

		System.err.println("changed resource size: " + totalResourceSizes);

		System.err.println("relevant graph size: " + totalGraphSize);

		if (totalGraphSize != totalResourceSizes)
			allValid = false;
		System.err.println("validated changes..." + allValid);
	}

	private Set<String> removeHawkProxies(IGraphNode instance,
			String modelRefName, Set<String> modelrefvaluesclone) {

		// String repoURL = "";
		// String destinationObjectRelativePathURI = "";
		//
		// String destinationObjectRelativeFileURI =
		// destinationObjectRelativePathURI
		// .substring(0, destinationObjectRelativePathURI.indexOf("#"));
		// String destinationObjectFullFileURI = repoURL
		// + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
		// + destinationObjectRelativeFileURI;

		for (String propertykey : instance.getPropertyKeys()) {
			// temporary hack ignoring actual file path as it is not known --
			// not fullproof as an object with the same id can be in another
			// file and it will still be removed
			if (propertykey.startsWith("_proxyRef:")) {

				String[] proxies = (String[]) instance.getProperty(propertykey);

				for (int i = 0; i < proxies.length; i = i + 2)
					modelrefvaluesclone.remove(proxies[i].substring(proxies[i]
							.indexOf("#") + 1));

			}
		}

		return modelrefvaluesclone;
	}

	private boolean flattenedStringEquals(Object dbattr, Object attr) {

		String newdbattr = dbattr == null ? "null" : dbattr.toString();
		if (dbattr instanceof int[])
			newdbattr = Arrays.toString((int[]) dbattr);
		else if (dbattr instanceof long[])
			newdbattr = Arrays.toString((long[]) dbattr);
		else if (dbattr instanceof String[])
			newdbattr = Arrays.toString((String[]) dbattr);
		else if (dbattr instanceof boolean[])
			newdbattr = Arrays.toString((boolean[]) dbattr);
		else if (dbattr instanceof Object[])
			newdbattr = Arrays.toString((Object[]) dbattr);

		String newattr = attr == null ? "null" : attr.toString();
		if (attr instanceof int[])
			newattr = Arrays.toString((int[]) attr);
		else if (attr instanceof long[])
			newattr = Arrays.toString((long[]) attr);
		else if (attr instanceof String[])
			newattr = Arrays.toString((String[]) attr);
		else if (attr instanceof boolean[])
			newattr = Arrays.toString((boolean[]) attr);
		else if (attr instanceof Object[])
			newattr = Arrays.toString((Object[]) attr);

		return newdbattr.equals(newattr);

	}

	@Override
	public void changeStart() {

	}

	@Override
	public void changeSuccess() {

	}

	@Override
	public void changeFailure() {

	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {

	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {

	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {

	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {

	}

	@Override
	public void modelElementAddition(VcsCommitItem s, IHawkObject element,
			IGraphNode elementNode, boolean isTransient) {

	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode,
			boolean isTransient) {

	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s,
			IHawkObject eObject, String attrName, Object oldValue,
			Object newValue, IGraphNode elementNode, boolean isTransient) {

	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s,
			IHawkObject eObject, String attrName, IGraphNode elementNode,
			boolean isTransient) {

	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {

	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {

	}

}
