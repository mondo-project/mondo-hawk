/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.internal.updater;

import java.util.Iterator;
import java.util.Set;

import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.IGraphDatabase.Mode;

public class Utils {

	protected String makeRelative(Set<String> bases, String extension) {
		for (final String base : bases) {
			if (extension.startsWith(base)) {
				return extension.substring(base.length());
			}
		}
		System.err
				.println(String.format(
						"WARNING: could not make '%s' into a relative path",
						extension));
		return extension;
	}

	protected String[] addToElementProxies(String[] proxies,
			String fullPathURI, String edgelabel, boolean isContainment,
			boolean isContainer) {

		// System.err.println("addtoelementproxies: " +
		// Arrays.toString(proxies));
		// System.err.println("fullpathuri " + fullPathURI);
		// System.err.println("edgelabel " + edgelabel);

		if (proxies != null) {

			String[] ret = new String[proxies.length + 4];

			for (int i = 0; i < proxies.length; i = i + 4) {

				ret[i] = proxies[i];
				ret[i + 1] = proxies[i + 1];
				ret[i + 2] = proxies[i + 2];
				ret[i + 3] = proxies[i + 3];

			}

			ret[proxies.length] = fullPathURI;
			ret[proxies.length + 1] = edgelabel;
			ret[proxies.length + 2] = isContainment + "";
			ret[proxies.length + 3] = isContainer + "";

			proxies = null;

			// System.err.println("ret " + Arrays.toString(ret));

			return ret;

		} else {
			String[] ret = new String[] { fullPathURI, edgelabel,
					isContainment + "", isContainer + "" };
			return ret;
		}
	}

	/**
	 * returns the node corresponding to the changed file in the VcsCommitItem,
	 * or null if there are non. Only uses transactions if the current mode of
	 * the database is transactional.
	 * 
	 * @param graph
	 * @param s
	 * @return
	 */
	protected IGraphNode getFileNodeFromVCSCommitItem(IGraphDatabase graph,
			VcsCommitItem s) {

		final String repository = s.getCommit().getDelta().getManager()
				.getLocation();
		final String filepath = s.getPath();

		final String fullFileID = repository
				+ GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + filepath;

		IGraphNode ret = null;
		IGraphTransaction t = null;

		try {

			if (graph.currentMode().equals(Mode.TX_MODE))
				t = graph.beginTransaction();

			final Iterator<IGraphNode> itFile = graph.getFileIndex()
					.get("id", fullFileID).iterator();

			if (itFile.hasNext()) {
				ret = itFile.next();
			} else
				System.err.println("WARNING: no file node found for: "
						+ s.getPath());

			if (graph.currentMode().equals(Mode.TX_MODE)) {
				t.success();
				t.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;

	}
}