/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
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