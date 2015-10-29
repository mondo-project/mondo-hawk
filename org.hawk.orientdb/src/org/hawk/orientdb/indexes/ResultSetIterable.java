package org.hawk.orientdb.indexes;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hawk.core.graph.IGraphIterable;
import org.hawk.orientdb.OrientDatabase;

import com.orientechnologies.orient.core.db.record.OIdentifiable;

final class ResultSetIterable<T> implements IGraphIterable<T> {
	private final Collection<OIdentifiable> resultSet;
	private final Class<T> klass;
	private final OrientDatabase db;

	public ResultSetIterable(Collection<OIdentifiable> resultSet, OrientDatabase db, Class<T> klass) {
		this.resultSet = resultSet;
		this.klass = klass;
		this.db = db;
	}

	@Override
	public Iterator<T> iterator() {
		if (resultSet == null || resultSet.isEmpty()) {
			return Collections.emptyListIterator();
		} else {
			return Collections.singleton(getSingle()).iterator();
		}
	}

	@Override
	public int size() {
		return resultSet.size();
	}

	@Override
	public T getSingle() {
		final Iterator<OIdentifiable> iterator = resultSet.iterator();
		if (iterator.hasNext()) {
			return db.getElementById(iterator.next().getIdentity(), klass);
		}
		return null;
	}
}