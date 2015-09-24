package runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
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
import org.hawk.graph.internal.updater.GraphModelUpdater;

public class SyncChangeListener implements IGraphChangeListener {

	private Git git;
	private LinkedList<RevCommit> orderedCommits = new LinkedList<>();
	ModelIndexerImpl hawk;

	public SyncChangeListener(Git git,
			LinkedHashMap<String, RevCommit> commits, IModelIndexer hawk) {
		this.hawk = (ModelIndexerImpl) hawk;
		this.git = git;
		for (RevCommit c : commits.values())
			orderedCommits.add(0, c);
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
		if (orderedCommits.size() > 0) {
			RevCommit first = orderedCommits.getFirst();
			orderedCommits.remove();
			System.out.println("changing repo to commit with time: "
					+ first.getCommitTime());
			// update git to this revision
			//
			// Repository r = git.getRepository();
			// git.
			CheckoutCommand c = git.checkout();
			c.setStartPoint(first);
			c.setAllPaths(true);
			try {
				c.call();
			} catch (Exception e) {
				System.err.println(e.getMessage());
				// e.printStackTrace();
			}
		} else {
			System.out.println("done -- we are at latest marked commit");
		}
	}

	@SuppressWarnings("unchecked")
	private void validateChanges() {
		System.err.println("validating changes...");
		boolean allValid = true;
		int totalResourceSizes = 0;
		int totalGraphSize = 0;
		// for all non-null resources
		if (hawk.fileToResourceMap != null)
			for (VcsCommitItem c : hawk.fileToResourceMap.keySet()) {

				IHawkModelResource r = hawk.fileToResourceMap.get(c);

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
												+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR
												+ c.getPath()).getSingle();

						Iterable<IGraphEdge> instancesEdges = filenode
								.getIncomingWithType("file");

						// cache model elements in current resource
						HashMap<String, IHawkObject> eobjectCache = new HashMap<>();
						for (IHawkObject content : r.getAllContentsSet())
							eobjectCache.put(content.getUriFragment(), content);

						// go through all nodes in graph from the file the
						// resource
						// is in
						for (IGraphEdge instanceEdge : instancesEdges) {
							IGraphNode instance = instanceEdge.getStartNode();
							totalGraphSize++;

							IHawkObject eobject = eobjectCache.get(instance
									.getProperty("id"));

							// if a node cannot be found in the model cache
							if (eobject == null) {
								System.err
										.println("error in validating: graph contains node with identifier:"
												+ instance.getProperty("id")
												+ " but resource does not!");
								allValid = false;
							} else {
								eobjectCache.remove(instance.getProperty("id"));

								// cache model element attributes and references
								// by
								// name
								HashMap<String, Object> attributeCache = new HashMap<>();
								for (IHawkAttribute a : ((IHawkClass) eobject
										.getType()).getAllAttributes())
									if (eobject.isSet(a))
										attributeCache.put(a.getName(),
												eobject.get(a));

								HashMap<String, Set<String>> referenceCache = new HashMap<>();
								for (IHawkReference ref : ((IHawkClass) eobject
										.getType()).getAllReferences()) {
									if (eobject.isSet(ref)) {
										Object refval = eobject.get(ref, true);
										HashSet<String> vals = new HashSet<>();
										if (refval instanceof Iterable<?>) {
											for (Object val : ((Iterable<IHawkObject>) refval))
												vals.add(((IHawkObject) val)
														.getUriFragment());

										} else {
											vals.add(((IHawkObject) refval)
													.getUriFragment());

										}
										referenceCache.put(ref.getName(), vals);
									}
								}

								for (String propertykey : instance
										.getPropertyKeys()) {
									//
									Object dbattr = instance
											.getProperty(propertykey);
									Object attr = attributeCache
											.get(propertykey);

									if (!dbattr.equals(attr)) {
										System.err
												.println("error in validating, attribute: "
														+ propertykey
														+ " has values:");
										System.err.println("database:\t"
												+ dbattr);
										System.err.println("model:\t" + attr);
										//
									} else
										attributeCache.remove(propertykey);
								}

								if (attributeCache.size() > 0) {
									System.err
											.println("error in validating, the following attributes were not found in the graph node:");
									System.err.println(attributeCache.keySet());
								}

								HashMap<String, Set<String>> nodereferences = new HashMap<>();

								for (IGraphEdge reference : instance
										.getOutgoing()) {

									if (reference.getType().equals("file")
											|| reference.getType().equals(
													"typeOf")
											|| reference.getType().equals(
													"kindOf")
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
										refvals.add(reference.getEndNode()
												.getProperty("id").toString());
										nodereferences.put(reference.getType(),
												refvals);
										//
									}
								}
								// compare model and graph reference maps
								for (String modelRefName : referenceCache
										.keySet()) {
									if (!nodereferences
											.containsKey(modelRefName))
										System.err
												.println("error in validating: reference "
														+ modelRefName
														+ " had targets in the model but not in the graph: ");
									else {
										Set<String> noderefvalues = nodereferences
												.get(modelRefName);
										Set<String> modelrefvalues = referenceCache
												.get(modelRefName);

										Set<String> noderefvaluesclone = nodereferences
												.get(modelRefName);
										noderefvaluesclone
												.removeAll(modelrefvalues);

										Set<String> modelrefvaluesclone = referenceCache
												.get(modelRefName);
										modelrefvaluesclone
												.removeAll(noderefvalues);

										if (noderefvaluesclone.size() > 0) {
											System.err
													.println("error in validating: reference "
															+ modelRefName);
											System.err
													.println(noderefvaluesclone);
											System.err
													.println("the above ids were found in the graph but not the model");
										}

										if (modelrefvaluesclone.size() > 0) {
											System.err
													.println("error in validating: reference "
															+ modelRefName);
											System.err
													.println(modelrefvaluesclone);
											System.err
													.println("the above ids were found in the model but not the graph");
										}

										// nodereferences.remove(modelRefName);
										referenceCache.remove(modelRefName);
									}
								}

								if (referenceCache.size() > 0) {
									System.err
											.println("error in validating: references "
													+ referenceCache.keySet()
													+ " had targets in the graph but not in the model: ");
								}

							}
							// if there are model elements not found in nodes
							if (eobjectCache.size() > 0) {
								System.err
										.println("error in validating: the following objects were not found in the graph:");
								System.err.println(eobjectCache.keySet());
								allValid = false;
							}

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
		else
			System.err
					.println("filetoresourcemap was empty -- maybe a metamodel addition happened?");

		if (hawk.deleteditems != null)
			for (@SuppressWarnings("unused")
			VcsCommitItem c : hawk.deleteditems) {
				// any other check needed other than totalsize match?
			}
		else
			System.err
					.println("deleteditems was empty -- maybe a metamodel addition happened?");
		//
		System.err.println("changed resource size: " + totalResourceSizes);

		System.err.println("relevant graph size: " + totalGraphSize);

		if (totalGraphSize != totalResourceSizes)
			allValid = false;
		System.err.println("validated changes..." + allValid);
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
