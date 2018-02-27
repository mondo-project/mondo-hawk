package org.hawk.greycat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.hawk.core.graph.IGraphDatabase.Mode;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.plugin.NodeState;
import greycat.plugin.Resolver;
import greycat.struct.Relation;

public class GreycatNode implements IGraphNode {
	
	private enum Direction {
		IN {
			@Override
			public String getPrefix() {
				return "in_";
			}
	
			public IGraphEdge convertToEdge(String type, GreycatNode current, GreycatNode other) {
				if (GreycatHeavyEdge.NODETYPE.equals(other.getNodeLabel())) {
					return new GreycatHeavyEdge(other);
				}
				return new GreycatLightEdge(other, current, type);
			}
		}, OUT {
			@Override
			public String getPrefix() {
				return "out_";
			}
	
			@Override
			public IGraphEdge convertToEdge(String type, GreycatNode current, GreycatNode other) {
				if (GreycatHeavyEdge.NODETYPE.equals(other.getNodeLabel())) {
					return new GreycatHeavyEdge(other);
				}
				return new GreycatLightEdge(current, other, type);
			}
		};
	
		public abstract String getPrefix();
		public abstract IGraphEdge convertToEdge(String type, GreycatNode current, GreycatNode other);
	}

	protected final class LazyNode implements Supplier<Node> {
		private Graph _graph;
		private Node _node;

		public LazyNode(Graph graph, Node node) {
			this._graph = graph;
			this._node = node;
		}

		@Override
		public Node get() {
			if (db.getGraph() == _graph) {
				return _node;
			} else {
				// there was a reconnection: reload the Node
				CompletableFuture<Node> c = new CompletableFuture<>();
				db.getGraph().lookup(world, time, id, node -> {
					c.complete(node);
				});

				try {
					_node = c.get();
				} catch (InterruptedException | ExecutionException e) {
					LOGGER.error(e.getMessage(), e);
					_node = null;
				}
			}

			return _node;
		}
	}

	/** Prefix for all attribute names. Prevents clashes with reserved names. */
	private static final String ATTRIBUTE_PREFIX = "a_";

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatNode.class);

	private final GreycatDatabase db;
	private final long world, time, id;
	private final Supplier<Node> node;

	private static int getValueType(Object value) {
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

	public GreycatNode(GreycatDatabase db, Node node) {
		this.db = db;

		this.world = node.world();
		this.time = node.time();
		this.id = node.id();
		this.node = new LazyNode(node.graph(), node);
	}

	@Override
	public Object getId() {
		return id;
	}

	public String getNodeLabel() {
		return getNode().get(GreycatDatabase.NODE_LABEL_IDX).toString();
	}

	@Override
	public Set<String> getPropertyKeys() {
		final Node n = getNode();
		final Resolver resolver = n.graph().resolver();
		NodeState state = resolver.resolveState(n);

		final Set<String> results = new HashSet<>();
		state.each((attributeKey, elemType, elem) -> {
            final String resolveName = resolver.hashToString(attributeKey);
            if (resolveName != null && resolveName.startsWith(ATTRIBUTE_PREFIX)) {
            	results.add(resolveName.substring(ATTRIBUTE_PREFIX.length()));
            }
		});

		return results;
	}

	@Override
	public Object getProperty(String name) {
		return getNode().get(ATTRIBUTE_PREFIX + name);
	}

	@Override
	public void setProperty(String name, Object value) {
		setPropertyRaw(name, value);
		saveOutsideTx();
	}

	/**
	 * Allows for setting multiple properties at once, in a slightly more efficient way.
	 */
	public void setProperties(Map<String, Object> props) {
		for (Entry<String, Object> entry : props.entrySet()) {
			setPropertyRaw(entry.getKey(), entry.getValue());
		}
		saveOutsideTx();
	}

	/**
	 * Saves the property, without saving.
	 */
	private void setPropertyRaw(String name, Object value) {
		getNode().set(ATTRIBUTE_PREFIX + name, getValueType(value), value);
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		return getAllEdges(getAllEdges(new ArrayList<>(), Direction.OUT), Direction.IN);
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		return getEdgesWithType(getEdgesWithType(new ArrayList<>(), Direction.IN, type), Direction.OUT, type);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		return getEdgesWithType(new ArrayList<>(), Direction.OUT, type);
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		return getEdgesWithType(new ArrayList<>(), Direction.IN, type);
	}

	private List<IGraphEdge> getEdgesWithType(final List<IGraphEdge> results, final Direction dir, String type) {
		final CompletableFuture<Boolean> done = new CompletableFuture<>();
		node.get().traverse(dir.getPrefix() + type, (Node[] targets) -> {
			for (Node target : targets) {
				results.add(dir.convertToEdge(type, this,
					new GreycatNode(getGraph(), target)));
			}
			done.complete(true);
		});
		done.join();
	
		return results;
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		return getAllEdges(new ArrayList<>(), Direction.IN);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		return getAllEdges(new ArrayList<>(), Direction.OUT);
	}

	private List<IGraphEdge> getAllEdges(final List<IGraphEdge> results, final Direction dir) {
		final Node n = getNode();
		final Resolver resolver = n.graph().resolver();
		final NodeState state = resolver.resolveState(n);
	    final String prefix = dir.getPrefix();
	
		state.each((attributeKey, elemType, elem) -> {
			if (elemType == Type.RELATION) {
	            final String resolveName = resolver.hashToString(attributeKey);
				if (resolveName.startsWith(prefix)) {
	            	final String edgeType = resolveName.substring(prefix.length());
	                Relation castedRelArr = (Relation) elem;
	                for (int j = 0; j < castedRelArr.size(); j++) {
	                	GreycatNode targetNode = db.getNodeById(castedRelArr.get(j));
	                	results.add(dir.convertToEdge(edgeType, this, targetNode));
	                }
	            }
			}
		});
		return results;
	}

	@Override
	public void delete() {
		if (db.currentMode() == Mode.NO_TX_MODE) {
			CompletableFuture<Boolean> cSaved = new CompletableFuture<>();
			db.hardDelete(this, dropped -> cSaved.complete(true));
			cSaved.join();
		} else {
			db.softDelete(this);
		}
	}

	@Override
	public GreycatDatabase getGraph() {
		return db;
	}

	@Override
	public void removeProperty(String name) {
		node.get().remove(ATTRIBUTE_PREFIX + name);
		saveOutsideTx();
	}

	protected void saveOutsideTx() {
		db.saveOutsideTx(new CompletableFuture<>()).join();
	}

	public Node getNode() {
		return node.get();
	}

	/**
	 * Returns <code>true</code> if this element has been soft deleted.
	 * If so, it should be ignored by any queries and iterables.
	 */
	protected boolean isSoftDeleted() {
		final Boolean softDeleted = (Boolean) node.get().get(GreycatDatabase.SOFT_DELETED_KEY);
		return softDeleted == Boolean.TRUE;
	}

	protected IGraphEdge addEdge(String type, GreycatNode end, Map<String, Object> props) {
		if (props == null || props.isEmpty()) {
			return GreycatLightEdge.create(type, this, end);
		} else {
			return GreycatHeavyEdge.create(type, this, end, props);
		}
	}

	protected void addOutgoing(String type, final GreycatNode endNode) {
		getNode().addToRelation(Direction.OUT.getPrefix() + type, endNode.getNode());
	}

	protected void addIncoming(String type, final GreycatNode endNode) {
		getNode().addToRelation(Direction.IN.getPrefix() + type, endNode.getNode());
	}

	protected void removeOutgoing(String type, final GreycatNode endNode) {
		getNode().removeFromRelation(Direction.OUT.getPrefix() + type, endNode.getNode());
	}

	protected void removeIncoming(String type, final GreycatNode endNode) {
		getNode().removeFromRelation(Direction.IN.getPrefix() + type, endNode.getNode());
	}

	@Override
	public String toString() {
		return "GreycatNode [world=" + world + ", time=" + time + ", id=" + id + ", getNode()=" + getNode() + "]";
	}
}
