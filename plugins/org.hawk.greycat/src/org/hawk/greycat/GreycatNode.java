package org.hawk.greycat;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphDatabase.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.plugin.NodeState;
import greycat.plugin.Resolver;

public class GreycatNode implements IGraphNode {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatNode.class);

	private final GreycatDatabase db;
	private final long world, time, id;
	private Node node;
	private Graph parentGraph;

	public GreycatNode(GreycatDatabase db, Node node) {
		this.db = db;

		this.world = node.world();
		this.time = node.time();
		this.id = node.id();

		this.node = node;
		this.parentGraph = node.graph();
	}

	@Override
	public Object getId() {
		return id;
	}

	@Override
	public Set<String> getPropertyKeys() {
		final Resolver resolver = getNode().graph().resolver();
		NodeState state = resolver.resolveState(node);

		final Set<String> results = new HashSet<>();
		state.each((attributeKey, elemType, elem) -> {
            final String resolveName = resolver.hashToString(attributeKey);
            if (resolveName != null) {
            	results.add(resolveName);
            }
		});

		results.remove(GreycatDatabase.NODE_LABEL_IDX);
		results.remove(GreycatDatabase.SOFT_SAFE_KEY);
		return results;
	}

	public Node getNode() {
		if (db.getGraph() == parentGraph) {
			return node;
		} else {
			// there was a reconnection: reload the Node
			CompletableFuture<Node> c = new CompletableFuture<>();
			db.getGraph().lookup(world, time, id, node -> {
				c.complete(node);
			});

			try {
				node = c.get();
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error(e.getMessage(), e);
				node = null;
			}
		}

		return node;
	}

	@Override
	public Object getProperty(String name) {
		return getNode().get(name);
	}

	@Override
	public void setProperty(String name, Object value) {
		getNode().set(name, getType(value), value);
		saveOutsideTx();
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() {
		if (db.currentMode() == Mode.NO_TX_MODE) {
			CompletableFuture<Boolean> cSaved = new CompletableFuture<>();
			db.hardDelete(getNode(), dropped -> cSaved.complete(true));
			cSaved.join();
		} else {
			db.softDelete(this);
		}
	}

	@Override
	public IGraphDatabase getGraph() {
		return db;
	}

	@Override
	public void removeProperty(String name) {
		getNode().remove(name);
		saveOutsideTx();
	}

	protected void saveOutsideTx() {
		db.saveOutsideTx(new CompletableFuture<>()).join();
	}

	private static int getType(Object value) {
		if (value == null) {
			return Type.STRING;
		}

		switch (value.getClass().getSimpleName()) {
		case "Short":
		case "Byte":
		case "Integer":
		case "Long":
			return Type.INT;
		case "Float":
		case "Double":
			return Type.DOUBLE;
		case "String":
			return Type.STRING;
		}

		LOGGER.warn("Unknown type: {}, returning Type.STRING", value.getClass().getSimpleName());
		return Type.STRING;
	}

	/**
	 * Returns <code>true</code> if this element has been soft deleted.
	 * If so, it should be ignored by any queries and iterables.
	 */
	public boolean isSoftDeleted() {
		final Boolean softDeleted = (Boolean) getNode().get(GreycatDatabase.SOFT_SAFE_KEY);
		return softDeleted == Boolean.TRUE;
	}
}
