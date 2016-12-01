package org.hawk.neo4j_v2;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import org.hawk.core.util.GraphChangeAdapter;

/**
 * Simple listener that allows for blocking another thread until a
 * synchronisation has been completed.
 */
public class SyncEndListener extends GraphChangeAdapter {
	private final Callable<?> r;
	private final Semaphore sem;
	private Throwable ex = null;

	public SyncEndListener(Callable<?> r, Semaphore sem) {
		this.r = r;
		this.sem = sem;
	}

	@Override
	public void synchroniseEnd() {
		try {
			r.call();
		} catch (Throwable e) {
			ex = e;
		}
		sem.release();
	}

	public Throwable getThrowable() {
		return ex;
	}
}