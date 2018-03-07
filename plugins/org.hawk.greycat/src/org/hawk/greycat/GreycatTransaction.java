package org.hawk.greycat;

import org.hawk.core.graph.IGraphTransaction;

public class GreycatTransaction implements IGraphTransaction {

	private final GreycatDatabase db;

	public GreycatTransaction(GreycatDatabase db) {
		this.db = db;
	}

	@Override
	public void success() {
		db.commitLuceneIndex();
		db.save();
	}

	@Override
	public void failure() {
		db.reconnect();
	}

	@Override
	public void close() {
		// nothing to do?
	}

}
