package org.hawk.greycat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.greycat.GreycatNode.NodeReader;
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
		try (NodeReader rStart = from.getNodeReader(); NodeReader rEnd = to.getNodeReader()) {
			final GreycatNode gHeavyEdgeNode = from.getGraph().createNode(props, NODETYPE);

			try (GreycatNode.NodeReader rEdge = gHeavyEdgeNode.getNodeReader()) {
				final Node heavyEdgeNode = rEdge.get();

				heavyEdgeNode.set(GreycatHeavyEdge.TYPE_PROP, Type.STRING, type);
				heavyEdgeNode.addToRelation(GreycatHeavyEdge.START_REL, rStart.get());
				heavyEdgeNode.addToRelation(GreycatHeavyEdge.END_REL, rEnd.get());
				GreycatNode.addOutgoing(type, rStart, rEdge);
				GreycatNode.addIncoming(type, rEnd, rEdge);
				rEdge.markDirty();
			}

			return new GreycatHeavyEdge(gHeavyEdgeNode, type);
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatHeavyEdge.class);

	protected static final String NODETYPE = "h_heavyEdge";
	protected static final String END_REL = "h_end";
	protected static final String START_REL = "h_start";
	protected static final String TYPE_PROP = "h_type";

	private final GreycatNode node;

	/**
	 * The type is already known from the in/out edge of the target/source node, but
	 * we keep it in the intermediate node as well in case the user takes advantage
	 * of a node visualizer.
	 */
	private final String type;

	public GreycatHeavyEdge(GreycatNode node, String type) {
		this.node = node;
		this.type = type;
	}

	@Override
	public Object getId() {
		return node.getId();
	}

	@Override
	public String getType() {
		return type;
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
		final GreycatNode startNode = getStartNode();
		final GreycatNode endNode = getEndNode();

		try (
			NodeReader rEdge = node.getNodeReader();
			NodeReader rStart = startNode.getNodeReader();
			NodeReader rEnd = endNode.getNodeReader()
		) {
			final String type = getType();

			GreycatNode.removeOutgoing(type, rStart, rEdge);
			GreycatNode.removeIncoming(type, rEnd, rEdge);
			node.delete();
		}
	}

	@Override
	public void removeProperty(String name) {
		node.removeProperty(name);
	}

	protected GreycatNode getRawTarget(final String relationName) {
		try (NodeReader reader = node.getNodeReader()) {
			CompletableFuture<GreycatNode> result = new CompletableFuture<>();
			reader.get().traverse(relationName, (Node[] targets) -> {
				final Node target = targets[0];
				final GreycatNode newNode = new GreycatNode(node.getGraph(),
					target.world(), target.time(), target.id());
				target.free();
				result.complete(newNode);
			});

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

	@Override
	public String toString() {
		return "GreycatHeavyEdge [node=" + node + ", getStartNode()=" + getStartNode() + ", getEndNode()="
				+ getEndNode() + "]";
	}
	
}
