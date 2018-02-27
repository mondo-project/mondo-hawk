package org.hawk.greycat;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.hawk.core.graph.IGraphEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greycat.Node;

/**
 * "Heavy" edge that can have its own properties. Emulated in Greycat
 * through documents, like in Orient.
 */
public class GreycatEdge implements IGraphEdge {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatEdge.class);

	protected static final String HEAVYEDGE_END_REL = "h_end";
	protected static final String HEAVYEDGE_START_REL = "h_start";
	protected static final String HEAVYEDGE_TYPE_PROP = "h_type";

	private final GreycatNode node;

	public GreycatEdge(GreycatNode node) {
		this.node = node;
	}

	@Override
	public Object getId() {
		return node.getId();
	}

	@Override
	public String getType() {
		return node.getNode().get(HEAVYEDGE_TYPE_PROP).toString();
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
		return getRawTarget(HEAVYEDGE_START_REL);
	}

	@Override
	public GreycatNode getEndNode() {
		return getRawTarget(HEAVYEDGE_END_REL);
	}

	public GreycatNode getBackingNode() {
		return node;
	}

	@Override
	public void delete() {
		getStartNode().removeHeavyEdge(this);
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

}
