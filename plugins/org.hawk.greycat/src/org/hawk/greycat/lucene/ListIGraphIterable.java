package org.hawk.greycat.lucene;

import java.util.Iterator;
import java.util.List;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;

final class ListIGraphIterable implements IGraphIterable<IGraphNode> {
	private final List<IGraphNode> nodes;

	protected ListIGraphIterable(List<IGraphNode> nodes) {
		this.nodes = nodes;
	}

	@Override
	public Iterator<IGraphNode> iterator() {
		return nodes.iterator();
	}

	@Override
	public int size() {
		return nodes.size();
	}

	@Override
	public IGraphNode getSingle() {
		return nodes.iterator().next();
	}
}