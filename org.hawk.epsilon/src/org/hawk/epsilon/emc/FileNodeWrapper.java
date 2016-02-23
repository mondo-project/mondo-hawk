/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.util.ArrayList;
import java.util.List;

import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.query.IQueryEngine;
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
