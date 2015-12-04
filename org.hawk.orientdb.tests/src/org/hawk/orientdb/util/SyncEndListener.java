package org.hawk.orientdb.util;

import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.hawk.core.IModelIndexer;
import org.hawk.core.util.GraphChangeAdapter;

public class SyncEndListener extends GraphChangeAdapter {
	private final Callable<?> r;
	private final Semaphore sem;
	private Throwable ex = null;

	private SyncEndListener(Callable<?> r, Semaphore sem) {
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

	public static void waitForSync(IModelIndexer indexer, final Callable<?> r) throws Throwable {
		final Semaphore sem = new Semaphore(0);
		final SyncEndListener changeListener = new SyncEndListener(r, sem);
		indexer.addGraphChangeListener(changeListener);
		if (!sem.tryAcquire(200, TimeUnit.SECONDS)) {
			fail("Synchronization timed out");
		} else {
			indexer.removeGraphChangeListener(changeListener);
			if (changeListener.getThrowable() != null) {
				throw changeListener.getThrowable();
			}
		}
	}

}