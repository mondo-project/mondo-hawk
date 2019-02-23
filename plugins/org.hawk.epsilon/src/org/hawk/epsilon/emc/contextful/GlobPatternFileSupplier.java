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
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds all the relevant files through glob-like patterns on the file index.
 */
public class GlobPatternFileSupplier implements Supplier<Set<IGraphNode>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobPatternFileSupplier.class);

	private final List<String> fplist, rplist;
	private final IGraphDatabase graph;

	public GlobPatternFileSupplier(IGraphDatabase graph, List<String> fplist, List<String> rplist) {
		this.graph = graph;
		this.fplist = fplist;
		this.rplist = rplist;
	}

	@Override
	public synchronized Set<IGraphNode> get() {
		final GraphWrapper gw = new GraphWrapper(graph);
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final Set<FileNode> fileNodes = gw.getFileNodes(rplist, fplist);
			final Set<IGraphNode> rawFileNodes = new HashSet<>();
			for (FileNode fn : fileNodes) {
				rawFileNodes.add(fn.getNode());
			}
			tx.success();
			return rawFileNodes;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptySet();
		}
	}
}