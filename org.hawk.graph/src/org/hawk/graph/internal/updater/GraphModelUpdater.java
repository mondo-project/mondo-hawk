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
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelUpdater;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChange;
import org.hawk.core.graph.IGraphChangeDescriptor;
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

	// private GraphDatabase graph;

	public static final boolean caresAboutResources = true;

	public GraphModelUpdater() {
	}

	@Override
	public void run(IAbstractConsole c, IModelIndexer hawk) throws Exception {

		this.indexer = hawk;
		// graph = indexer.getGraph();
		console = c;

	}

	@Override
	public IGraphChangeDescriptor updateStore(
			Map<VcsCommitItem, IHawkModelResource> r) {

		long start = System.currentTimeMillis();

		// for (VCSFileRevision s : currreposchangeditems) {
		// try {
		// // if (s.getPath().endsWith(".ecore")) {
		// String[] path = s.getPath().replaceAll("\\\\", "/").split("/");
		// String[] file = path[path.length - 1].split("\\.");
		// String extension = file[file.length - 1];
		// if (parser.getMetamodelExtensions().contains(extension)) {
		// new Neo4JMonitorMMInsertBatch().run(this, s, parser);
		// } else if (!parser.getModelExtensions().contains(extension))
		// syserr("Warning: updatestore given a file with an unparsable extension ("
		// + s.getPath()
		// + "), it is ignored (should have been caught before this warning)");
		// } catch (Exception e) {
		// }
		// }
		// FIXMEd do we want to then remove all models in database not found
		// here?
		// new Neo4JMonitorMInsert().retainAll(graph, currrepositems);

		IGraphChangeDescriptor ret = new GraphChangeDescriptorImpl(
				"Default Hawk GraphModelUpdater");

		for (VcsCommitItem f : r.keySet()) {
			// System.err.println(s);
			LinkedList<IGraphChange> t = new GraphModelInserter(indexer).run(
					r.get(f), f);
			if (t == null) {
				ret.setErrorState(true);
			} else
				ret.addChanges(t);

		}

		// System.err.println("change: " + change)

		long end = System.currentTimeMillis();
		console.println((end - start) / 1000 + "s" + (end - start) % 1000
				+ "ms [pure insertion]");

		// System.err.println("ret[0]: " + ret[0]);

		// if (error) {
		console.println("attempting to resolve any leftover cross-file references...");
		try {
			GraphModelInserter.resolveProxies(indexer.getGraph(), ret);
		} catch (Exception e) {
			console.printerrln("Exception in updateStore - resolving proxies, returning 0:");
			console.printerrln(e);
		}

		console.println("attempting to resolve any derived proxies...");
		try {
			ret.setUnresolvedDerivedProperties((new GraphModelInserter(indexer)
					.resolveDerivedAttributeProxies(indexer.getGraph(),
							indexer, "org.hawk.epsilon.emc.EOLQueryEngine")));
		} catch (Exception e) {
			console.printerrln("Exception in updateStore - resolving DERIVED proxies, returning 0:");
			console.printerrln(e);
		}

		console.println("attempting to update any relevant derived attributes...");
		try {
			new GraphModelInserter(indexer).updateDerivedAttributes(ret,
					"org.hawk.epsilon.emc.EOLQueryEngine");
		} catch (Exception e) {
			console.printerrln("Exception in updateStore - UPDATING DERIVED attributes");
			console.printerrln(e);
		}

		return ret;

	}

	public boolean isActive() {
		return isActive;
	}

	// @Override
	// public void runGrabatsBasic() {
	// try {
	// basicQueries.run(graph, console);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	// @Override
	// public void runGrabatsAdvanced() {
	// try {
	// // sysout((console==null)+"");
	// // sysout(console+"");
	// advancedQueries.run(graph, console, parser);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

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
	public Set<VcsCommitItem> compareWithLocalFiles(Set<VcsCommitItem> reposItems) {
		if (reposItems.isEmpty()) {
			return reposItems;
		}

		final VcsCommitItem firstItem = reposItems.iterator().next();
		final String repositoryURL = firstItem.getCommit().getDelta().getRepository().getUrl();
		final Set<VcsCommitItem> changed = new HashSet<VcsCommitItem>();
		changed.addAll(reposItems);

		IGraphDatabase graph = indexer.getGraph();
		
		if (graph != null) {

			try (IGraphTransaction tx = graph.beginTransaction()) {
				// operations on the graph
				// ...

				IGraphNodeIndex filedictionary = null;

				filedictionary = graph.getFileIndex();

				// TODO: this class shouldn't have to know how we've set up the file index.
				// Also, why is the Neo4j backend implementing this bit of functionality?
				if (filedictionary != null && filedictionary.query("id", repositoryURL + FILEINDEX_REPO_SEPARATOR + "*").iterator().hasNext()) {
					for (VcsCommitItem r : reposItems) {
						String rev = "-2";
						try {
							IGraphIterable<IGraphNode> ret = filedictionary.get("id", repositoryURL + FILEINDEX_REPO_SEPARATOR + r.getPath());

							IGraphNode n = ret.getSingle();

							rev = (String) n.getProperty("revision");

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
