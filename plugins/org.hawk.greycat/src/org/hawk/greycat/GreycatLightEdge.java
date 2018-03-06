package org.hawk.greycat;

import java.util.Collections;
import java.util.Set;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

/**
 * Light edge that cannot have any properties. This is the default edge
 * implemented by Greycat: heavy edges need to emulated with extra nodes.
 */
public class GreycatLightEdge implements IGraphEdge {

	public static GreycatLightEdge create(String type, GreycatNode from, GreycatNode to) {
		from.addOutgoing(type, to);
		to.addIncoming(type, from);
		from.save();

		return new GreycatLightEdge(from, to, type);
	}

	private GreycatNode start, end;
	private String type;

	public GreycatLightEdge(GreycatNode start, GreycatNode end, String type) {
		this.start = start;
		this.end = end;
		this.type = type;
	}

	@Override
	public Object getId() {
		throw new UnsupportedOperationException("Light edges do not have an identifier");
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public Set<String> getPropertyKeys() {
		return Collections.emptySet();
	}

	@Override
	public Object getProperty(String name) {
		// we have no properties
		return null;
	}

	@Override
	public void setProperty(String name, Object value) {
		throw new UnsupportedOperationException("Light edges cannot set properties");
	}

	@Override
	public IGraphNode getStartNode() {
		return start;
	}

	@Override
	public IGraphNode getEndNode() {
		return end;
	}

	@Override
	public void delete() {
		start.removeOutgoing(type, end);
		end.removeIncoming(type, start);
		start.save();
	}

	@Override
	public void removeProperty(String name) {
		throw new UnsupportedOperationException("Light edges cannot set properties");
	}

	@Override
	public String toString() {
		return "GreycatLightEdge [start=" + start + ", end=" + end + ", type=" + type + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		GreycatLightEdge other = (GreycatLightEdge) obj;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
}
