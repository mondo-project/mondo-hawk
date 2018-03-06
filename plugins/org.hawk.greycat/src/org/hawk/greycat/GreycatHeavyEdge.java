package org.hawk.greycat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.hawk.core.graph.IGraphEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greycat.Node;
import greycat.Type;

/**
 * "Heavy" edge that can have its own properties. Emulated in Greycat
 * through documents, like in Orient.
 */
public class GreycatHeavyEdge implements IGraphEdge {

	public static GreycatHeavyEdge create(String type, GreycatNode from, GreycatNode to, Map<String, Object> props) {
		final Node startNode = from.getNode();
		final Node endNode = to.getNode();
		final GreycatNode gHeavyEdgeNode = from.getGraph().createNode(props, NODETYPE);

		final Node heavyEdgeNode = gHeavyEdgeNode.getNode();
		heavyEdgeNode.set(GreycatHeavyEdge.TYPE_PROP, Type.STRING, type);
		heavyEdgeNode.addToRelation(GreycatHeavyEdge.START_REL, startNode);
		heavyEdgeNode.addToRelation(GreycatHeavyEdge.END_REL, endNode);
		from.addOutgoing(type, gHeavyEdgeNode);
		to.addIncoming(type, gHeavyEdgeNode);
		gHeavyEdgeNode.save();

		return new GreycatHeavyEdge(gHeavyEdgeNode);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatHeavyEdge.class);

	protected static final String NODETYPE = "h_heavyEdge";
	protected static final String END_REL = "h_end";
	protected static final String START_REL = "h_start";
	protected static final String TYPE_PROP = "h_type";

	private final GreycatNode node;

	public GreycatHeavyEdge(GreycatNode node) {
		this.node = node;
	}

	@Override
	public Object getId() {
		return node.getId();
	}

	@Override
	public String getType() {
		return node.getNode().get(TYPE_PROP).toString();
	}

	@Override
	public Set<String> getPropertyKeys() {
		return node.getPropertyKeys();
	}

	@Override
	public Object getProperty(String name) {
		return node.getProperty(name);
	}

	@Override
	public void setProperty(String name, Object value) {
		node.setProperty(name, value);
	}

	@Override
	public GreycatNode getStartNode() {
		return getRawTarget(START_REL);
	}

	@Override
	public GreycatNode getEndNode() {
		return getRawTarget(END_REL);
	}

	public GreycatNode getBackingNode() {
		return node;
	}

	@Override
	public void delete() {
		final String type = getType();

		final GreycatNode startNode = getStartNode();
		startNode.removeOutgoing(type, node);
		getEndNode().removeIncoming(type, node);
		node.delete();

		startNode.save();
	}

	@Override
	public void removeProperty(String name) {
		node.removeProperty(name);
	}

	protected GreycatNode getRawTarget(final String relationName) {
		CompletableFuture<GreycatNode> result = new CompletableFuture<>(); 
		node.getNode().traverse(relationName, (Node[] targets) -> {
			result.complete(new GreycatNode(node.getGraph(), targets[0]));
		});
	
		try {
			return result.get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
			return null;
		}
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
		GreycatHeavyEdge other = (GreycatHeavyEdge) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		return true;
	}
	
}
