/*******************************************************************************
 * Copyright (c) 2015 The University of York.
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
package org.hawk.graph;

import java.util.Iterator;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.graph.updater.GraphModelBatchInjector;

/**
 * Read-only abstraction of a file within the graph populated by this updater.
 */
public class FileNode {
	protected static class EdgeIterator implements Iterator<ModelElementNode> {
		private final Iterator<IGraphEdge> it;

		protected EdgeIterator(Iterator<IGraphEdge> it) {
			this.it = it;
		}

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
	}

	public static final String FILE_NODE_LABEL = "file";
	public static final String PROP_REPOSITORY = "repository";

	private final IGraphNode node;

	public FileNode(IGraphNode node) {
		this.node = node;
	}

	public IGraphNode getNode() {
		return node;
	}
	
	public String getFilePath() {
		return (String)node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
	}

	public String getRepositoryURL() {
		return (String)node.getProperty(PROP_REPOSITORY);
	}

	/**
	 * Returns all the {@link ModelElementNode}s representing model elements for
	 * this file node. Tries to reuse the more efficient IGraphIterable 
	 */
	public Iterable<ModelElementNode> getModelElements() {
		final Iterable<IGraphEdge> incomingWithType = node.getIncomingWithType(ModelElementNode.EDGE_LABEL_FILE);

		if (incomingWithType instanceof IGraphIterable) {
			return new IGraphIterable<ModelElementNode>() {
				@Override
				public Iterator<ModelElementNode> iterator() {
					return new EdgeIterator(incomingWithType.iterator());
				}

				@Override
				public int size() {
					return ((IGraphIterable<?>) incomingWithType).size();
				}

				@Override
				public ModelElementNode getSingle() {
					return new ModelElementNode(incomingWithType.iterator().next().getStartNode());
				}
			};
		}

		return new Iterable<ModelElementNode>() {
			@Override
			public Iterator<ModelElementNode> iterator() {
				return new EdgeIterator(incomingWithType.iterator());
			}
		};
	}

	/**
	 * Returns <code>true</code> if the model element is a root (not contained by
	 * any other, not even in another file).
	 */
	public boolean isRoot(ModelElementNode men) {
		for (ModelElementNode root : getRootModelElements()) {
			if (root.getNodeId().equals(men.getNodeId())) {
				return true;
			}
		}
		return false;
	}

	public Iterable<ModelElementNode> getRootModelElements() {
		final IGraphNodeIndex rootDictionary = node.getGraph()
			.getOrCreateNodeIndex(GraphModelBatchInjector.ROOT_DICT_NAME);
		final Iterable<? extends IGraphNode> roots = rootDictionary
			.get(GraphModelBatchInjector.ROOT_DICT_FILE_KEY, node.getId().toString());

		return new Iterable<ModelElementNode>() {
			@Override
			public Iterator<ModelElementNode> iterator() {
				final Iterator<? extends IGraphNode> it = roots.iterator();

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
		return "FileNode [node=" + node + ", fileName=" + getFilePath() + "]";
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