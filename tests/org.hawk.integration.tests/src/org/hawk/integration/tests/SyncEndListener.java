/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests;

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