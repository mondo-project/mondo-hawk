package org.hawk.arangodb;

import java.util.HashSet;
import java.util.Set;

import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

import com.arangodb.ArangoException;
import com.arangodb.entity.BaseDocument;

public class ArangoEdge implements IGraphEdge {

	private static final String TYPE_ATTR = "__type";
	private BaseDocument edgeEntity;
	private ArangoDatabase db;

	public ArangoEdge(ArangoDatabase db, BaseDocument baseDocument) {
		this.db = db;
		this.edgeEntity = baseDocument;
	}

	@Override
	public String getId() {
		return edgeEntity.getDocumentHandle();
	}

	@Override
	public String getType() {
		return edgeEntity.getAttribute(TYPE_ATTR).toString();
	}

	@Override
	public Set<String> getPropertyKeys() {
		Set<String> properties = new HashSet<>(edgeEntity.getProperties().keySet());
		properties.remove(TYPE_ATTR);
		return properties;
	}

	@Override
	public Object getProperty(String name) {
		return edgeEntity.getAttribute(name);
	}

	@Override
	public void setProperty(String name, Object value) {
		edgeEntity.addAttribute(name, value);
		save();
	}

	@Override
	public IGraphNode getStartNode() {
		return db.getNodeById(edgeEntity.getAttribute("_from"));
	}

	@Override
	public IGraphNode getEndNode() {
		return db.getNodeById(edgeEntity.getAttribute("_to"));
	}

	@Override
	public void delete() {
		try {
			db.getGraph().deleteDocument(edgeEntity.getDocumentKey());
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void removeProperty(String name) {
		edgeEntity.getProperties().remove(name);
		save();
	}

	protected void save() {
		try {
			db.getGraph().replaceDocument(getId(), edgeEntity);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
