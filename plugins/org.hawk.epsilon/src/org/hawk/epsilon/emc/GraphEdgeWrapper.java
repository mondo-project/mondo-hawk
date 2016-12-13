package org.hawk.epsilon.emc;

import java.util.Set;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.hawk.core.graph.IGraphEdge;

/**
 * Wraps an edge produced during a Hawk query. Has a few useful synonyms
 * to support common names and the literal terms used within Hawk.
 */
public class GraphEdgeWrapper {

	private IGraphEdge edge;
	private EOLQueryEngine containerModel;

	public GraphEdgeWrapper(IGraphEdge r, EOLQueryEngine m) {
		this.edge = r;
		this.containerModel = m;
	}

	public IGraphEdge getEdge() {
		return edge;
	}

	public String getType() {
		return edge.getType();
	}

	public String getName() {
		return getType();
	}

	public Object getEndNode() {
		return new GraphNodeWrapper(edge.getEndNode(), containerModel);
	}

	public Object getTarget() {
		return getEndNode();
	}

	public Object getStartNode() {
		return new GraphNodeWrapper(edge.getStartNode(), containerModel);
	}

	public Object getSource() {
		return getStartNode();
	}

	public EOLQueryEngine getContainerModel() {
		return containerModel;
	}

	public Set<String> getPropertyKeys() {
		return edge.getPropertyKeys();
	}

	public Object getProperty(String name) {
		return edge.getProperty(name);
	}

	public Object getFeature(String name) throws EolRuntimeException {
		return containerModel.getPropertyGetter().invoke(this, name);
	}
}
