package org.hawk.arangodb;

import java.util.Collections;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

import com.arangodb.ArangoException;
import com.arangodb.CursorResult;
import com.arangodb.entity.BaseDocument;
import com.arangodb.util.MapBuilder;

public class ArangoNode implements IGraphNode {

	private String handle;
	private BaseDocument doc;
	private ArangoDatabase db;

	public ArangoNode(ArangoDatabase db, BaseDocument doc) {
		this.handle = doc.getDocumentHandle();
		this.doc = doc;
		this.db = db;
	}

	@Override
	public String getId() {
		return handle;
	}

	@Override
	public Set<String> getPropertyKeys() {
		return doc.getProperties().keySet();
	}

	@Override
	public Object getProperty(String name) {
		return doc.getAttribute(name);
	}

	@Override
	public void setProperty(String name, Object value) {
		if (value == null) {
			removeProperty(name);
		} else {
			doc.getProperties().put(name, value);
			save();
		}
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		return getEdges("ANY");
	}

	protected Iterable<IGraphEdge> getEdges(final String direction) {
		try {
			CursorResult<BaseDocument> results = db.getGraph().executeAqlQuery("FOR v, e IN " + direction + " @start RETURN e", new MapBuilder().put("start", getId()).get(), null, BaseDocument.class);
			return new PlainEdgeEntityIterable(db, results);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		return getEdgesWithType(type, "ANY");
	}

	protected Iterable<IGraphEdge> getEdgesWithType(String type, final String direction) {
		try {
			CursorResult<BaseDocument> results = db.getGraph().executeAqlQuery("FOR v, e IN " + direction + " @start FILTER e.__type = @type RETURN e", new MapBuilder().put("start", getId()).put("type", type).get(), null, BaseDocument.class);
			return new PlainEdgeEntityIterable(db, results);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		return getEdgesWithType(type, "OUTBOUND");
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		return getEdgesWithType(type, "INBOUND");
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		return getEdges("INBOUND");
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		return getEdges("OUTBOUND");
	}

	@Override
	public void delete() {
		try {
			db.getGraph().deleteDocument(getId());
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public IGraphDatabase getGraph() {
		return db;
	}

	@Override
	public void removeProperty(String name) {
		doc.getProperties().remove(name);
		save();
	}

	protected void save() {
		try {
			db.getGraph().replaceDocument(getId(), doc);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
