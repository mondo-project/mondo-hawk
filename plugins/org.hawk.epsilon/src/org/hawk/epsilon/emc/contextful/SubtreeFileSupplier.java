/*******************************************************************************
 * Copyright (c) 2018-2019 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc.contextful;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds all the files in scope by going to the root elements of all the files
 * and seeing if they are contained within the root of the subtree.
 */
public class SubtreeFileSupplier implements Supplier<Set<IGraphNode>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SubtreeFileSupplier.class);

	private final String subtreeRootPath;
	private final List<String> rplist;
	private final IGraphDatabase graph;

	public SubtreeFileSupplier(IGraphDatabase graph, String subtreeRootPath, List<String> rplist) {
		this.subtreeRootPath = subtreeRootPath;
		this.rplist = rplist;
		this.graph = graph;
	}

	@Override
	public Set<IGraphNode> get() {
		final GraphWrapper gw = new GraphWrapper(graph);
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final Set<IGraphNode> fileNodes = new HashSet<>();

			final Set<FileNode> allFileNodes = gw.getFileNodes(rplist, null);
			for (FileNode fn : allFileNodes) {
				Iterator<ModelElementNode> itElems = fn.getModelElements().iterator();

				if (itElems.hasNext()) {
					ModelElementNode first = itElems.next();
					if (rplist == null) {
						if (first.isContainedWithin(null, subtreeRootPath)) {
							fileNodes.add(fn.getNode());
						}
					} else {
						for (String repo : rplist) {
							if (first.isContainedWithin(repo, subtreeRootPath)) {
								fileNodes.add(fn.getNode());
								break;
							}
						}
					}
				}
			}

			tx.success();
			return fileNodes;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptySet();
		}
	}
}