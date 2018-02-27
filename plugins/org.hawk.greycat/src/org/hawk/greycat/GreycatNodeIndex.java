package org.hawk.greycat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greycat.Graph;
import greycat.NodeIndex;

public class GreycatNodeIndex implements IGraphNodeIndex {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatNodeIndex.class);

	private final class LazyNodeIndex implements Supplier<NodeIndex> {
		private Graph parentGraph;
		private CompletableFuture<NodeIndex> nodeIndex;

		@Override
		public NodeIndex get() {
			if (nodeIndex == null || parentGraph != db.getGraph()) {
				nodeIndex = new CompletableFuture<>();
				db.getGraph().declareIndex(db.getWorld(), name,
					index -> nodeIndex.complete(index));
			}

			try {
				return nodeIndex.get();
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error(e.getMessage(), e);
				return null;
			}
		}
	}

	private final GreycatDatabase db;
	private final String name;
	private final Supplier<NodeIndex> nodeIndex = new LazyNodeIndex();

	public GreycatNodeIndex(GreycatDatabase db, String name) {
		this.db = db;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IGraphIterable<IGraphNode> query(String key, Object valueExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IGraphIterable<IGraphNode> query(String key, Number from, Number to, boolean fromInclusive,
			boolean toInclusive) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IGraphIterable<IGraphNode> get(String key, Object valueExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(IGraphNode n, Map<String, Object> derived) {
		// TODO Auto-generated method stub

	}

	@Override
	public void add(IGraphNode n, String s, Object derived) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(IGraphNode n) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(String key, Object value, IGraphNode n) {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// nothing to do
	}

	@Override
	public void delete() {
		CompletableFuture<Boolean> done = new CompletableFuture<>();
		nodeIndex.get().drop(result -> done.complete(true));
		done.join();
	}

}
