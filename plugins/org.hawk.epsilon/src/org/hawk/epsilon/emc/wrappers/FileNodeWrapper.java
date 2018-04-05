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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc.wrappers;

import java.util.ArrayList;
import java.util.List;

import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.query.IQueryEngine;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.FileNode;
import org.hawk.graph.ModelElementNode;

public class FileNodeWrapper implements IGraphNodeReference {

	private FileNode fileNode;
	private EOLQueryEngine model;

	public FileNodeWrapper(FileNode fileNode, EOLQueryEngine eolQueryEngine) {
		this.fileNode = fileNode;
		this.model = eolQueryEngine;
	}

	@Override
	public String getId() {
		return fileNode.getNode().getId().toString();
	}

	@Override
	public IGraphNode getNode() {
		return fileNode.getNode();
	}

	@Override
	public IQueryEngine getContainerModel() {
		return model;
	}

	@Override
	public String getTypeName() {
		return "_hawkFileNode";
	}

	public String getPath() {
		return fileNode.getFilePath();
	}

	public String getRepository() {
		return fileNode.getRepositoryURL();
	}

	public List<GraphNodeWrapper> getRoots() {
		List<GraphNodeWrapper> results = new ArrayList<>();
		for (ModelElementNode n : fileNode.getRootModelElements()) {
			results.add(new GraphNodeWrapper(n.getNode(), model));
		}
		return results;
	}

	public List<GraphNodeWrapper> getContents() {
		List<GraphNodeWrapper> results = new ArrayList<>();
		for (ModelElementNode n : fileNode.getModelElements()) {
			results.add(new GraphNodeWrapper(n.getNode(), model));
		}
		return results;
	}

	@Override
	public String toString() {
		return String.format("FNW|id:%s|type:%s", getId(), getTypeName());
	}
}
