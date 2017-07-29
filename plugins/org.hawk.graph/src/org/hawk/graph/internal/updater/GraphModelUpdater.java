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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hawk.core.IConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkModelResource;

public class GraphModelUpdater implements IModelUpdater {

	private IModelIndexer indexer;
	private IConsole console;
	private TypeCache typeCache = new TypeCache();

	private boolean isActive = false;
	public static final String FILEINDEX_REPO_SEPARATOR = "||||";
	public static final String PROXY_REFERENCE_PREFIX = "hawkProxyRef:";

	/**
	 * Used in URIs of targets when we only know the unique fragment and we
	 * don't know the file.
	 */
	public static final String PROXY_FILE_WILDCARD = "*";

	public static final boolean caresAboutResources = true;

	private Set<IGraphNode> toBeUpdated = new HashSet<>();

	public GraphModelUpdater() {
	}

	@Override
	public void run(IConsole c, IModelIndexer hawk) throws Exception {
		this.indexer = hawk;
		this.console = c;
	}

	@Override
	public boolean updateStore(VcsCommitItem f, IHawkModelResource res) {
		final long start = System.currentTimeMillis();
		boolean success = true;

		/*
		 * We register this listener only for this particular updater and during
		 * this method call. It is used to collect information about which nodes
		 * should have their derived attributes updated, rather than collecting
		 * all changes and working from there (which can use a lot more memory).
		 * Ideally, if we do not have any derived attributes, this shouldn't
		 * require any more memory, and the CPU cost should be the same.
		 */
		final IGraphDatabase g = indexer.getGraph();
		final DirtyDerivedAttributesListener l = new DirtyDerivedAttributesListener(g);
		if (!indexer.getDerivedAttributeNames().isEmpty()) {
			indexer.addGraphChangeListener(l);
		}

		/*
		 * Print more information when we have few commit items: normally this
		 * means we have a few big files instead of many small files.
		 */
		final boolean verbose = f.getCommit().getDelta().getCommits().size() < 100;

		try {
			try {
				// file failed to update to latest version so remove it to
				// maintain consistency
				if (res == null) {
					if (!deleteAll(f)) {
						console.printerrln("warning: failed to delete item: "
								+ f
								+ "\nafter its resource failed to be loaded");
						success = false;
					}
				} else if (!new GraphModelInserter(indexer, typeCache).run(res, f, verbose)) {
					console.printerrln("warning: failed to update item: " + f
							+ "\nmodel resource: " + res);
					success = false;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				success = false;
			}

			if (verbose) {
				long end = System.currentTimeMillis();
				console.println((end - start) / 1000 + "s" + (end - start) % 1000 + "ms [pure insertion]");
			}
		} finally {
			final long s = System.currentTimeMillis();
			if (verbose) {
				console.print("marking any relevant derived attributes for update...");
				indexer.getCompositeStateListener().info("Marking relevant derived attributes for update...");
			}
			toBeUpdated.addAll(l.getNodesToBeUpdated());
			indexer.removeGraphChangeListener(l);

			if (verbose) {
				final long end = System.currentTimeMillis();
				console.println((end - s) / 1000 + "s" + (end - s) % 1000 + "ms");
				indexer.getCompositeStateListener().info("Marked relevant derived attributes for update. "
						+ (end - s) / 1000 + "s" + (end - s) % 1000 + "ms");
			}
		}
		return success;
	}

	@Override
	public void updateProxies() {
		final long start = System.currentTimeMillis();

		console.println("attempting to resolve any leftover cross-file references...");
		try {
			indexer.getCompositeStateListener().info(
					"Resolving any leftover cross-file references...");
			new GraphModelInserter(indexer, typeCache).resolveProxies(indexer.getGraph());
		} catch (Exception e) {
			console.printerrln("Exception in updateStore - resolving proxies, returning 0:");
			console.printerrln(e);
		}

		console.println("attempting to resolve any uninitialized derived attributes...");
		try {
			indexer.getCompositeStateListener().info(
					"Resolving any uninitialized derived attributes...");
			new GraphModelInserter(indexer, typeCache).resolveDerivedAttributeProxies(
					indexer.getDerivedAttributeExecutionEngine());
		} catch (Exception e) {
			console.printerrln("Exception in updateStore - resolving DERIVED proxies, returning 0:");
			console.printerrln(e);
		}

		console.println("attempting to update any relevant derived attributes...");
		try {
			indexer.getCompositeStateListener().info(
					"Updating any affected derived attributes...");
			new GraphModelInserter(indexer, typeCache).updateDerivedAttributes(
					indexer.getDerivedAttributeExecutionEngine(), toBeUpdated);
			toBeUpdated = new HashSet<>();
		} catch (Exception e) {
			toBeUpdated = new HashSet<>();
			console.printerrln("Exception in updateStore - UPDATING DERIVED attributes");
			console.printerrln(e);
		}
		long end = System.currentTimeMillis();
		console.println((end - start) / 1000 + "s" + (end - start) % 1000
				+ "ms [proxy update]");

		// Clear the type cache by creating a new one and letting the old one be GC'ed
		typeCache = new TypeCache();
	}

	public boolean isActive() {
		return isActive;
	}

	@Override
	public void shutdown() {
		//
	}

	@Override
	public boolean caresAboutResources() {
		return caresAboutResources;
	}

	@Override
	public boolean deleteAll(IVcsManager c) throws Exception {

		boolean ret = false;

		IGraphDatabase graph = indexer.getGraph();

		try (IGraphTransaction t = graph.beginTransaction()) {
			IGraphNodeIndex filedictionary = graph.getFileIndex();
			IGraphIterable<IGraphNode> fileNodes = filedictionary.query("id",
					c.getLocation() + "*");

			// Construct a simulated VcsRepositoryDelta with a "-deleted"
			// revision
			final VcsRepositoryDelta delta = new VcsRepositoryDelta();
			final VcsCommit fakeCommit = new VcsCommit();
			delta.setManager(c);
			delta.getCommits().add(fakeCommit);
			delta.setLatestRevision(c.getCurrentRevision() + "-deleted");
			fakeCommit.setAuthor("hawk");
			fakeCommit.setDelta(delta);
			fakeCommit.setJavaDate(new Date());
			fakeCommit.setRevision(c.getCurrentRevision() + "-deleted");
			fakeCommit.setMessage("stopped indexing");

			final DeletionUtils deletionUtils = new DeletionUtils(graph);
			final IGraphChangeListener changeListener = indexer
					.getCompositeGraphChangeListener();

			for (IGraphNode fileNode : fileNodes) {

				VcsCommitItem item = new VcsCommitItem();
				item.setChangeType(VcsChangeType.DELETED);
				item.setCommit(fakeCommit);
				String path = fileNode.getProperty(
						IModelIndexer.IDENTIFIER_PROPERTY).toString();
				item.setPath(path.startsWith("/") ? path : "/" + path);

				fakeCommit.getItems().add(item);
				//
				deletionUtils.deleteAll(fileNode, item, changeListener);
			}

			t.success();
		}

		return ret;
	}

	@Override
	public boolean deleteAll(VcsCommitItem c) throws Exception {
		indexer.getCompositeStateListener().info(
				"Deleting all contents of file: " + c.getPath() + "...");
		boolean ret = false;

		IGraphNode n = new Utils().getFileNodeFromVCSCommitItem(
				indexer.getGraph(), c);
		if (n != null) {

			try (IGraphTransaction t = indexer.getGraph().beginTransaction()) {
				ret = new DeletionUtils(indexer.getGraph()).deleteAll(n, c,
						indexer.getCompositeGraphChangeListener());
				t.success();
			} catch (Exception e) {
				e.printStackTrace();
				ret = false;
			}

		} else
			return true;

		indexer.getCompositeStateListener().info(
				"Deleted all contents of file: " + c.getPath() + ".");
		return ret;
	}

	@Override
	public void updateDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic) {
		new GraphModelInserter(indexer, typeCache).updateDerivedAttribute(metamodeluri,
				typename, attributename, attributetype, isMany, isOrdered,
				isUnique, derivationlanguage, derivationlogic);
	}

	@Override
	public void updateIndexedAttribute(String metamodeluri, String typename,
			String attributename) {
		new GraphModelInserter(indexer, typeCache).updateIndexedAttribute(metamodeluri,
				typename, attributename);
	}

	@Override
	public String getName() {
		return "Default Hawk GraphModelUpdater (v1.0)";
	}

	@Override
	public Set<VcsCommitItem> compareWithLocalFiles(
			Set<VcsCommitItem> reposItems) {
		if (reposItems.isEmpty()) {
			return reposItems;
		}

		final VcsCommitItem firstItem = reposItems.iterator().next();
		final String repositoryURL = firstItem.getCommit().getDelta()
				.getManager().getLocation();
		final Set<VcsCommitItem> changed = new HashSet<VcsCommitItem>();
		changed.addAll(reposItems);

		IGraphDatabase graph = indexer.getGraph();

		if (graph != null) {

			try (IGraphTransaction tx = graph.beginTransaction()) {
				// operations on the graph
				// ...

				IGraphNodeIndex filedictionary = graph.getFileIndex();

				if (filedictionary != null
						&& filedictionary
								.query("id",
										repositoryURL
												+ FILEINDEX_REPO_SEPARATOR
												+ "*").iterator().hasNext()) {
					for (VcsCommitItem r : reposItems) {
						String rev = "-2";
						try {
							IGraphIterable<IGraphNode> ret = filedictionary
									.get("id",
											repositoryURL
													+ FILEINDEX_REPO_SEPARATOR
													+ r.getPath());

							if (ret.iterator().hasNext()) {
								IGraphNode n = ret.getSingle();
								rev = (String) n.getProperty("revision");
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
						if (r.getCommit().getRevision().equals(rev))
							changed.remove(r);

						if (IModelIndexer.VERBOSE)
							console.println("comparing revisions of: "
									+ r.getPath() + " | "
									+ r.getCommit().getRevision() + " | " + rev);

					}
				}

				tx.success();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return changed;
	}
}
