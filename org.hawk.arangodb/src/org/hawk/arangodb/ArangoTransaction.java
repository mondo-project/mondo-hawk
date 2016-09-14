package org.hawk.arangodb;

import org.hawk.core.graph.IGraphTransaction;

/**
 * ArangoDB's transactions are JavaScript functions that are sent to the server
 * to be run at once: Hawk has no way of using them, however.
 */
public class ArangoTransaction implements IGraphTransaction {

	@Override
	public void success() {
		// nothing
	}

	@Override
	public void failure() {
		// nothing
	}

	@Override
	public void close() {
		// nothing
	}

}
