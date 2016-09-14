package org.hawk.arangodb;

import java.util.Collections;
import java.util.Iterator;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;

import com.arangodb.DocumentCursor;
import com.arangodb.entity.BaseDocument;

public class DocumentCursorIterable implements IGraphIterable<IGraphNode> {

	private final DocumentCursor<BaseDocument> cursor;
	private final ArangoDatabase db;

	public DocumentCursorIterable(ArangoDatabase db, DocumentCursor<BaseDocument> docs) {
		this.db = db;
		this.cursor = docs;
	}

	public DocumentCursorIterable() {
		this.cursor = null;
		this.db = null;
	}

	@Override
	public Iterator<IGraphNode> iterator() {
		if (cursor == null) {
			return Collections.emptyIterator();
		}

		Iterator<BaseDocument> it = cursor.entityIterator();
		return new Iterator<IGraphNode>(){
			@Override
			public boolean hasNext() {
				// TODO Auto-generated method stub
				return it.hasNext();
			}

			@Override
			public IGraphNode next() {
				return new ArangoNode(db, it.next());
			}
		};
	}

	@Override
	public int size() {
		if (cursor == null) {
			return 0;
		}

		int size = 0;
		for (Iterator<BaseDocument> it = cursor.entityIterator(); it.hasNext(); ) {
			it.next();
			size++;
		}
		return size;
	}

	@Override
	public IGraphNode getSingle() {
		return new ArangoNode(db, cursor.entityIterator().next());
	}

}
