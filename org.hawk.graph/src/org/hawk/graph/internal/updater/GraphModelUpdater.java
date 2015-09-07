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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelUpdater;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.model.IHawkModelResource;

public class GraphModelUpdater implements IModelUpdater {

	private IModelIndexer indexer;
	private IAbstractConsole console;

	private boolean isActive = false;
	public static final String FILEINDEX_REPO_SEPARATOR = "////";
	public static final boolean caresAboutResources = true;

	public GraphModelUpdater() {
	}

	@Override
	public void run(IAbstractConsole c, IModelIndexer hawk) throws Exception {
		this.indexer = hawk;
		this.console = c;
	}

	@Override
	public void updateStore(Map<VcsCommitItem, IHawkModelResource> r) {
		final long start = System.currentTimeMillis();

		/*
		 * We register this listener only for this particular updater and during
		 * this method call. It is used to collect information about which nodes
		 * should have their derived attributes updated, rather than collecting
		 * all changes and working from there (which can use a lot more memory).
		 * Ideally, if we do not have any derived attributes, this shouldn't
		 * require any more memory, and the CPU cost should be the same.
		 */
		final IGraphDatabase g = indexer.getGraph();
		final DirtyDerivedAttributesListener l = new DirtyDerivedAttributesListener(
				g);
		indexer.addGraphChangeListener(l);

		try {
			for (VcsCommitItem f : r.keySet()) {
				try {
					IHawkModelResource res = r.get(f);
					if (!new GraphModelInserter(indexer).run(res, f))
						console.printerrln("warning: failed to update item: "
								+ f + "\nmodel resource: " + res);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			long end = System.currentTimeMillis();
			console.println((end - start) / 1000 + "s" + (end - start) % 1000
					+ "ms [pure insertion]");

			console.println("attempting to resolve any leftover cross-file references...");
			try {
				new GraphModelInserter(indexer).resolveProxies(indexer
						.getGraph());
			} catch (Exception e) {
				console.printerrln("Exception in updateStore - resolving proxies, returning 0:");
				console.printerrln(e);
			}

			console.println("attempting to resolve any derived proxies...");
			try {
				new GraphModelInserter(indexer).resolveDerivedAttributeProxies(
						indexer.getGraph(), indexer,
						"org.hawk.epsilon.emc.EOLQueryEngine");
			} catch (Exception e) {
				console.printerrln("Exception in updateStore - resolving DERIVED proxies, returning 0:");
				console.printerrln(e);
			}

			console.println("attempting to update any relevant derived attributes...");
			try {
				final Set<IGraphNode> toBeUpdated = l.getNodesToBeUpdated();
				new GraphModelInserter(indexer).updateDerivedAttributes(
						"org.hawk.epsilon.emc.EOLQueryEngine", toBeUpdated);
			} catch (Exception e) {
				console.printerrln("Exception in updateStore - UPDATING DERIVED attributes");
				console.printerrln(e);
			}
		} finally {
			indexer.removeGraphChangeListener(l);
		}
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
	public void deleteAll(String repository, String filepath) throws Exception {
		new DeletionUtils(indexer.getGraph()).deleteAll(repository, filepath);
	}

	@Override
	public void updateDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic) {
		new GraphModelInserter(indexer).updateDerivedAttribute(metamodeluri,
				typename, attributename, attributetype, isMany, isOrdered,
				isUnique, derivationlanguage, derivationlogic);
	}

	@Override
	public void updateIndexedAttribute(String metamodeluri, String typename,
			String attributename) {
		new GraphModelInserter(indexer).updateIndexedAttribute(metamodeluri,
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
				.getRepository().getUrl();
		final Set<VcsCommitItem> changed = new HashSet<VcsCommitItem>();
		changed.addAll(reposItems);

		IGraphDatabase graph = indexer.getGraph();

		if (graph != null) {

			try (IGraphTransaction tx = graph.beginTransaction()) {
				// operations on the graph
				// ...

				IGraphNodeIndex filedictionary = null;

				filedictionary = graph.getFileIndex();

				// TODO: this class shouldn't have to know how we've set up the
				// file index.
				// Also, why is the Neo4j backend implementing this bit of
				// functionality?
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
