package org.hawk.greycat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.hawk.core.graph.IGraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreycatTransaction implements IGraphTransaction {

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatTransaction.class);
	private final GreycatDatabase db;

	public GreycatTransaction(GreycatDatabase db) {
		this.db = db;
	}

	@Override
	public void success() {
		CompletableFuture<Boolean> cSaved = new CompletableFuture<>();
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
