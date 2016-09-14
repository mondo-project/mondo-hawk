package org.hawk.arangodb;

import java.util.Iterator;

import org.hawk.core.graph.IGraphEdge;

import com.arangodb.CursorResult;
import com.arangodb.entity.BaseDocument;

public class PlainEdgeEntityIterable implements Iterable<IGraphEdge> {

	private CursorResult<BaseDocument> cursor;
	private ArangoDatabase db;

	public PlainEdgeEntityIterable(ArangoDatabase db, CursorResult<BaseDocument> results) {
		this.db = db;
		this.cursor = results;
	}

	@Override
	public Iterator<IGraphEdge> iterator() {
		Iterator<BaseDocument> it = cursor.iterator();
		return new Iterator<IGraphEdge>(){

			@Override
			public boolean hasNext() {
				// TODO Auto-generated method stub
				return it.hasNext();
			}

			@Override
			public IGraphEdge next() {
				BaseDocument e = it.next();
				return new ArangoEdge(db, e);
			}
			
		};
	}

}
