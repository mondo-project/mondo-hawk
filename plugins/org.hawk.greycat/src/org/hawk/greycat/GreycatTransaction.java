package org.hawk.greycat;

import java.util.concurrent.CompletableFuture;

import org.hawk.core.graph.IGraphTransaction;

public class GreycatTransaction implements IGraphTransaction {

	private final GreycatDatabase db;

	public GreycatTransaction(GreycatDatabase db) {
		this.db = db;
	}

	@Override
	public void success() {
		CompletableFuture<Boolean> cSaved = new CompletableFuture<>();
		db.commitLuceneIndex();
		db.save(cSaved);
		cSaved.join();
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
