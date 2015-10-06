package org.hawk.graph;

import java.util.Iterator;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

public class PackageNode {

	private IGraphNode node;

	public PackageNode(IGraphNode n) {
		this.node = n;
	}

	public Iterable<TypeNode> getTypes() {
		final Iterable<IGraphEdge> iterableEdges = node.getIncomingWithType("epackage");
		return new Iterable<TypeNode>() {
			@Override
			public Iterator<TypeNode> iterator() {
				final Iterator<IGraphEdge> itEdges = iterableEdges.iterator();
				return new Iterator<TypeNode>() {

					@Override
					public boolean hasNext() {
						return itEdges.hasNext();
					}

					@Override
					public TypeNode next() {
						return new TypeNode(itEdges.next().getStartNode());
					}

					@Override
					public void remove() {
						itEdges.remove();
					}};
			}
		};
	}

}
