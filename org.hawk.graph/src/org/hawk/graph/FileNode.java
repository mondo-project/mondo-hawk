/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.graph.internal.updater.GraphModelBatchInjector;

/**
 * Read-only abstraction of a file within the graph populated by this updater.
 */
public class FileNode {
	private final IGraphNode node;

	public FileNode(IGraphNode node) {
		this.node = node;
	}

	public IGraphNode getNode() {
		return node;
	}
	
	public String getFileName() {
		return (String)node.getProperty("id");
	}

	/**
	 * Returns all the {@link ModelElementNode}s representing model elements for
	 * this file node.
	 */
	public Iterable<ModelElementNode> getModelElements() {
		return new Iterable<ModelElementNode>() {
			@Override
			public Iterator<ModelElementNode> iterator() {
				final Iterable<IGraphEdge> incomingWithType = node.getIncomingWithType("file");
				final Iterator<IGraphEdge> it = incomingWithType.iterator();

				return new Iterator<ModelElementNode>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public ModelElementNode next() {
						return new ModelElementNode(it.next().getStartNode());
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}


	public Iterable<ModelElementNode> getRootModelElements() {
		final IGraphNodeIndex rootDictionary = node.getGraph()
			.getOrCreateNodeIndex(GraphModelBatchInjector.ROOT_DICT_NAME);
		final Iterable<IGraphNode> roots = rootDictionary
			.get(GraphModelBatchInjector.ROOT_DICT_FILE_KEY, node.getId().toString());

		return new Iterable<ModelElementNode>() {
			@Override
			public Iterator<ModelElementNode> iterator() {
				final Iterator<IGraphNode> it = roots.iterator();

				return new Iterator<ModelElementNode>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public ModelElementNode next() {
						return new ModelElementNode(it.next());
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	@Override
	public String toString() {
		return "FileNode [node=" + node + ", fileName=" + getFileName() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileNode other = (FileNode) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}
}