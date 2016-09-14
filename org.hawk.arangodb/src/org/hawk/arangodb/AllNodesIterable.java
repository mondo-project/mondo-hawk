package org.hawk.arangodb;

import java.util.Collections;
import java.util.Iterator;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;

import com.arangodb.ArangoException;
import com.arangodb.CursorResult;
import com.arangodb.entity.BaseDocument;

public class AllNodesIterable implements IGraphIterable<IGraphNode> {

	private final ArangoDatabase db;
	private final String label;

	public AllNodesIterable(ArangoDatabase arangoDatabase, String label) {
		this.db = arangoDatabase;
		this.label = label;
	}

	@Override
	public Iterator<IGraphNode> iterator() {
		CursorResult<BaseDocument> docs;
		try {
			docs = db.getGraph().executeAqlQuery("FOR d IN " + label + " RETURN d", Collections.emptyMap(), null, BaseDocument.class);
		} catch (ArangoException e1) {
			return Collections.emptyIterator();
		}

		final Iterator<BaseDocument> it = docs.iterator();
		return new Iterator<IGraphNode>() {
			@Override
			public boolean hasNext() {
				boolean ret = it.hasNext();
				if (!ret) {
					try {
						docs.close();
					} catch (ArangoException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return ret;
			}

			@Override
			public IGraphNode next() {
				return new ArangoNode(db, it.next());
			}
			
		};
	}

	@Override
	public int size() {
		int size = 0;
		for (Iterator<IGraphNode> it = iterator(); it.hasNext(); ) {
			it.next();
			size++;
		}
		return size;
	}

	@Override
	public IGraphNode getSingle() {
		return iterator().next();
	}

}
