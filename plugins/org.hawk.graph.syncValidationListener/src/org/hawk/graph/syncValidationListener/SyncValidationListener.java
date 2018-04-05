/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
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
package org.hawk.graph.syncValidationListener;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.graph.internal.updater.GraphModelBatchInjector;
import org.hawk.graph.internal.updater.GraphModelUpdater;

public class SyncValidationListener implements IGraphChangeListener {

	private ModelIndexerImpl hawk;
	private int removedProxies, totalErrors, deleted, malformed, singletonCount, totalGraphSize, totalResourceSizes;
	private Set<String> changed = new HashSet<>();

	private IGraphNodeIndex singletonIndex;
	private boolean singletonIndexIsEmpty;

	public SyncValidationListener() {
		// osgi constructor
	}

	@Override
	public void setModelIndexer(IModelIndexer hawk) {
		this.hawk = (ModelIndexerImpl) hawk;

		System.err.println("SyncValidationListener: hawk.setSyncMetricsEnabled(true) called, performance will suffer!");
		hawk.setSyncMetricsEnabled(true);
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void synchroniseStart() {
		// nothing to do
	}

	@Override
	public void synchroniseEnd() {
		try {
			validateChanges();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		deleted = 0;
		changed.clear();
	}

	public int getTotalErrors() {
		return totalErrors;
	}

	private void validateChanges() throws URISyntaxException {
		System.err.println("sync metrics:");
		System.err.println("interesting\t" + hawk.getInterestingFiles());
		System.err.println("deleted\t\t" + hawk.getDeletedFiles());
		System.err.println("changed\t\t" + hawk.getCurrChangedItems());
		System.err.println("loaded\t\t" + hawk.getLoadedResources());
		System.err.println("c elems\t\t" + latestChangedElements());
		System.err.println("d elems\t\t" + latestDeletedElements());
		System.err.println("time\t\t~" + hawk.getLatestSynctime() / 1000 + "s");

		System.err.println("validating changes...");

		removedProxies = 0;
		totalErrors = 0;
		malformed = 0;
		singletonCount = 0;
		totalResourceSizes = 0;
		totalGraphSize = 0;

		final URI tempURI = new File(hawk.getGraph().getTempDir()).toURI();

		// for all non-null resources
		if (hawk.getFileToResourceMap() != null) {
			for (VcsCommitItem c : hawk.getFileToResourceMap().keySet()) {
				validateChanges(tempURI, c);
			}
		}

		System.err.println("changed resource size: " + totalResourceSizes);

		System.err.println("relevant graph size: "
				+ totalGraphSize
				+ (singletonCount > 0 ? (" + singleton count: " + singletonCount) : ""));

		if (totalGraphSize + singletonCount != totalResourceSizes) {
			totalErrors++;
		}

		System.err.println("validated changes... "
				+ (totalErrors == 0 ? "true"
						: ((totalErrors == malformed) + " (with "
								+ totalErrors + " total and "
								+ malformed + " malformed errors)"))
				+ (removedProxies == 0 ? "" : " [" + removedProxies
						+ "] unresolved hawk proxies matched"));
	}

	protected void validateChanges(final URI tempURI, final VcsCommitItem c) throws URISyntaxException {
		final String repository = c.getCommit().getDelta().getManager().getLocation();
		final String repoURL = repository;

		final IHawkModelResource r = hawk.getFileToResourceMap().get(c);
		if (r == null) {
			/*
			 * file didnt get parsed so no changes are made -- any way to verify this
			 * further?
			 */
			return;
		}

		System.out.println("validating file " + c.getChangeType() + " for " + c.getPath());

		final IGraphDatabase graph = hawk.getGraph();
		try (IGraphTransaction t = graph.beginTransaction()) {
			singletonIndex = graph.getOrCreateNodeIndex(GraphModelBatchInjector.FRAGMENT_DICT_NAME);
			singletonIndexIsEmpty = !singletonIndex.query("*", "*").iterator().hasNext();

			String file = null;
			IGraphNode filenode = null;
			try {
				file = repository + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + c.getPath();
				filenode = graph.getFileIndex().get("id", file).getSingle();
			} catch (Exception ee) {
				System.err.println("expected file " + file
						+ " but it did not exist (maybe metamodel not registered, if so expect +1 errors)");
				totalErrors++;
				return;
			}

			// cache model elements in current resource
			Map<String, IHawkObject> eobjectCache = new HashMap<>();
			// cache of malformed object identifiers (to ignore references)
			Set<String> malformedObjectCache = new HashSet<>();
			cacheModelElements(c, r, eobjectCache, malformedObjectCache);

			// go through all nodes in graph from the file the resource is in
			for (IGraphEdge instanceEdge : filenode.getIncomingWithType("file")) {
				final IGraphNode instance = instanceEdge.getStartNode();
				totalGraphSize++;

				final IHawkObject eobject = eobjectCache.get(instance.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

				// if a node cannot be found in the model cache
				if (eobject == null) {
					System.err.println("error in validating: graph contains node with identifier:"
							+ instance.getProperty(IModelIndexer.IDENTIFIER_PROPERTY) + " but resource does not!");
					// this triggers when a malformed model has 2 identical identifiers
					totalErrors++;
				} else {
					eobjectCache.remove(instance.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

					if (!malformedObjectCache.contains(eobject.getUri())) {
						compareAttributes(instance, eobject);
						compareReferences(tempURI, repoURL, instance, eobject);
					}
				}
			}

			// if there are model elements not found in nodes
			if (eobjectCache.size() > 0) {
				System.err.println("error in validating: the following objects were not found in the graph:");
				System.err.println(eobjectCache.keySet());
				totalErrors++;
			}

			t.success();
		} catch (Exception e) {
			System.err.println("syncValidationListener transaction error:");
			e.printStackTrace();
		}
	}

	protected void cacheModelElements(VcsCommitItem commitItem, IHawkModelResource modelResource,
			Map<String, IHawkObject> eobjectCache, Set<String> malformedObjectCache) {
		for (IHawkObject content : modelResource.getAllContents()) {
			IHawkObject old = eobjectCache.put(content.getUriFragment(), content);
			if (old != null) {
				if (!singletonIndexIsEmpty && singletonIndex.get("id", content.getUriFragment()).iterator().hasNext()) {
					singletonCount++;
				} else {
					System.err.println("warning (" + commitItem.getPath() + ") eobjectCache replaced:");
					System.err.println(old.getUri() + " | " + old.getUriFragment() + " | ofType: " + old.getType().getName());
					System.err.println("with:");
					System.err.println(content.getUri() + " | " + content.getUriFragment() + " | ofType: " + content.getType().getName());

					malformedObjectCache.add(old.getUri());
					malformed++;

					System.err
							.println("WARNING: MALFORMED MODEL RESOURCE (multiple identical identifiers for:\n"
									+ old.getUri()
									+ "),\nexpect "
									+ malformed
									+ " objects in validation.");

				}
			}

			totalResourceSizes++;
		}
	}

	protected void compareReferences(final URI tempURI, final String repoURL, final IGraphNode instance, final IHawkObject eobject) throws URISyntaxException {
		final Map<String, Set<String>> modelReferences = computeModelReferences(tempURI, repoURL, eobject);
		final Map<String, Set<String>> nodereferences = computeNodeReferences(repoURL, instance);

		// compare model and graph reference maps
		final Iterator<Entry<String, Set<String>>> rci = modelReferences.entrySet().iterator();
		while (rci.hasNext()) {
			final Entry<String, Set<String>> modelRef = rci.next();
			final String modelRefName = modelRef.getKey();
			if (!nodereferences.containsKey(modelRefName)) {
				continue;
			}

			final Set<String> noderefvalues = new HashSet<>(nodereferences.get(modelRefName));
			final Set<String> modelrefvalues = new HashSet<>(modelReferences.get(modelRefName));

			final Set<String> noderefvaluesclone = new HashSet<>(noderefvalues);
			noderefvaluesclone.removeAll(modelrefvalues);

			Set<String> modelrefvaluesclone = new HashSet<>(modelrefvalues);
			modelrefvaluesclone.removeAll(noderefvalues);
			modelrefvaluesclone = removeHawkProxies(instance, modelrefvaluesclone);

			filterFragmentBasedReferences(noderefvaluesclone, modelrefvaluesclone);

			if (noderefvaluesclone.size() > 0) {
				System.err.println("error in validating: reference " + modelRefName + " of node: "
						+ instance.getProperty(IModelIndexer.IDENTIFIER_PROPERTY) + "\nlocated: "
						+ instance.getOutgoingWithType("file").iterator().next().getEndNode()
								.getProperty(IModelIndexer.IDENTIFIER_PROPERTY));
				System.err.println(noderefvaluesclone);
				System.err.println("the above ids were found in the graph but not the model");
				totalErrors++;
			}

			if (modelrefvaluesclone.size() > 0) {
				System.err.println("error in validating: reference " + modelRefName + " of model element: "
						+ eobject.getUriFragment() + "\nlocated: " + eobject.getUri());
				System.err.println(modelrefvaluesclone);
				System.err.println("the above ids were found in the model but not the graph");
				totalErrors++;
			}

			nodereferences.remove(modelRefName);
			// rci.remove();
		}

		if (nodereferences.size() > 0) {
			System.err.println("error in validating: references " + nodereferences.keySet()
					+ " had targets in the graph but not in the model: ");
			System.err.println(nodereferences);
			totalErrors++;
		}
	}

	protected void filterFragmentBasedReferences(final Set<String> noderefvaluesclone, Set<String> modelrefvaluesclone) {
		// Take into account fragment-based references (Modelio)
		modelRefs: for (Iterator<String> itModelRefs = modelrefvaluesclone.iterator(); itModelRefs.hasNext();) {
			final String modelref = itModelRefs.next();

			final int idxHash = modelref.indexOf("#");
			final String path = modelref.substring(modelref.indexOf(GraphModelUpdater.FILEINDEX_REPO_SEPARATOR)
					+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR.length(), idxHash);
			if (path.equals("/*")) {
				final String fragment = modelref.substring(idxHash + 1);
				for (Iterator<String> itNodeRefs = noderefvaluesclone.iterator(); itNodeRefs.hasNext();) {
					final String noderef = itNodeRefs.next();
					if (noderef.endsWith("#" + fragment)) {
						itModelRefs.remove();
						itNodeRefs.remove();
						continue modelRefs;
					}
				}
			}
		}
	}

	protected Map<String, Set<String>> computeNodeReferences(String repoURL, final IGraphNode instance) {
		Map<String, Set<String>> nodereferences = new HashMap<>();

		for (IGraphEdge reference : instance.getOutgoing()) {
			if (reference.getType().equals("file")
				|| reference.getType().equals("ofType")
				|| reference.getType().equals("ofKind")
				|| reference.getPropertyKeys().contains("isDerived")) {
				continue;
			}

			final Set<String> refvals = new HashSet<>();
			if (nodereferences.containsKey(reference.getType())) {
				refvals.addAll(nodereferences.get(reference.getType()));
			}
			final IGraphNode refEndNode = reference.getEndNode();
			final String refEndNodeId = refEndNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();

			if (!singletonIndexIsEmpty && singletonIndex.get("id", refEndNodeId).iterator().hasNext()) {
				refvals.add(refEndNodeId);
			} else {
				final IGraphNode targetFileNode = refEndNode.getOutgoingWithType("file").iterator().next().getEndNode();
				final Object targetFileID = targetFileNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
				refvals.add(repoURL	+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + targetFileID + "#" + refEndNodeId);
			}

			nodereferences.put(reference.getType(), refvals);
		}
		return nodereferences;
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Set<String>> computeModelReferences(final URI tempURI, final String repoURL, final IHawkObject eobject) throws URISyntaxException {
		final URI repoURI = new URI(repoURL);

		// full reference uri in order to properly compare with hawk proxies
		Map<String, Set<String>> modelReferences = new HashMap<>();

		for (IHawkReference ref : ((IHawkClass) eobject.getType()).getAllReferences()) {
			if (eobject.isSet(ref)) {
				final Object refval = eobject.get(ref, false);
				final Set<String> vals = new HashSet<>();

				if (refval instanceof Iterable<?>) {
					for (Object val : ((Iterable<IHawkObject>) refval)) {
						vals.add(parseValue(val, repoURI, tempURI));
					}
				} else {
					vals.add(parseValue(refval, repoURI, tempURI));
				}

				if (!vals.isEmpty()) {
					modelReferences.put(ref.getName(), vals);
				}
			}
		}

		return modelReferences;
	}

	protected void compareAttributes(final IGraphNode node, final IHawkObject modelElement) {
		// cache model element attributes and references by name
		final Map<String, Object> modelAttributes = new HashMap<>();
		for (IHawkAttribute a : ((IHawkClass) modelElement.getType()).getAllAttributes()) {
			if (modelElement.isSet(a)) {
				modelAttributes.put(a.getName(), modelElement.get(a));
			}
		}

		for (String propertykey : node.getPropertyKeys()) {
			if (!propertykey.equals(IModelIndexer.SIGNATURE_PROPERTY)
					&& !propertykey.equals(IModelIndexer.IDENTIFIER_PROPERTY)
					&& !propertykey.startsWith(GraphModelUpdater.PROXY_REFERENCE_PREFIX)) {

				Object dbattr = node.getProperty(propertykey);
				Object attr = modelAttributes.get(propertykey);

				if (!flattenedStringEquals(dbattr, attr)) {
					totalErrors++;
					System.err.println("error in validating, attribute: " + propertykey + " has values:");
					final String cla1 = dbattr != null ? dbattr.getClass().toString() : "null attr";
					System.err.println(String.format("database:\t%s JAVATYPE: %s IN NODE: %s WITH ID: %s",
						(dbattr instanceof Object[] ? (Arrays.asList((Object[]) dbattr)) : dbattr),
						cla1, node.getId(), node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY)));

					String cla2 = attr != null ? attr.getClass().toString() : "null attr";
					System.err.println(String.format("model:\t\t%s JAVATYPE: %s IN ELEMENT WITH ID %s",
						(attr instanceof Object[] ? (Arrays.asList((Object[]) attr)) : attr),
						cla2, modelElement.getUriFragment()));
				}

				modelAttributes.remove(propertykey);
			}
		}
		if (modelAttributes.size() > 0) {
			System.err.println(String.format(
				"error in validating, the following attributes were "
				+ "not found in the graph node %s: %s",
				node.getId(), modelAttributes.keySet()));

			totalErrors++;
		}
	}

	private String parseValue(Object val, URI repo, URI temp) throws URISyntaxException {
		String ret;
		IHawkObject o = (IHawkObject) val;

		// System.err.println("-checking uniqueness of " + o.getUriFragment());
		if (!singletonIndexIsEmpty
				&& singletonIndex.get("id", o.getUriFragment()).iterator()
						.hasNext()) {

			// System.err.println("-singleton: " + o.getUriFragment()
			// + " (isfragunique: " + o.isFragmentUnique() + ")");
			ret = o.getUriFragment();

		} else {

			final URI objURI = new URI(o.getUri());
			ret = objURI.getPath().replace(repo.getPath(), "").replace(temp.getPath(), "").replace("+", "%2B");
			if (objURI.getFragment() != null) {
				ret += "#" + objURI.getFragment();
			}
			if (!ret.startsWith("/")) {
				ret = "/" + ret;
			}

			try {
				ret = URLDecoder.decode(ret, "UTF-8");
			} catch (Exception ex) {
				// might not be decodable that way (Modelio can produce something like '#//%Objing%')
			}

			ret = (repo + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + ret);

		}

		return ret;
	}

	private int latestChangedElements() {
		return changed.size();
	}

	private int latestDeletedElements() {
		return deleted;
	}

	private Set<String> removeHawkProxies(IGraphNode instance,
			Set<String> modelrefvaluesclone) {

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

			if (propertykey.startsWith(GraphModelUpdater.PROXY_REFERENCE_PREFIX)) {

				String[] proxies = (String[]) instance.getProperty(propertykey);

				for (int i = 0; i < proxies.length; i = i + 4)
					if (modelrefvaluesclone.remove(proxies[i]))
						removedProxies++;
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
		changed.add(elementNode.getId().toString());
	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode,
			boolean isTransient) {
		deleted++;
	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s,
			IHawkObject eObject, String attrName, Object oldValue,
			Object newValue, IGraphNode elementNode, boolean isTransient) {
		changed.add(elementNode.getId().toString());
	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s,
			IHawkObject eObject, String attrName, IGraphNode elementNode,
			boolean isTransient) {
		changed.add(elementNode.getId().toString());
	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
		changed.add(source.getId().toString());
	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
		changed.add(source.getId().toString());
	}

}
