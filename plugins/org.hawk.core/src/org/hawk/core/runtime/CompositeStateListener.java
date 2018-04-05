/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core.runtime;

import java.util.LinkedHashSet;

import org.hawk.core.IStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeStateListener extends LinkedHashSet<IStateListener> implements IStateListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(CompositeStateListener.class);
	private static final long serialVersionUID = 6358455874909268099L;
	private HawkState currentState = HawkState.STOPPED;

	@Override
	public boolean add(IStateListener e) {
		final boolean ret = super.add(e);
		e.state(currentState);
		return ret;
	}

	@Override
	public boolean remove(Object e) {
		final boolean ret = super.remove(e);
		if (ret) {
			((IStateListener) e).removed();
		}
		return ret;
	}

	@Override
	public void removed() {
		// nothing to do
	}

	@Override
	public void info(String s) {
		for (IStateListener l : this) {
			l.info(s);
		}
		LOGGER.info(s);
	}

	@Override
	public void error(String s) {
		for (IStateListener l : this) {
			l.error(s);
		}
		LOGGER.error(s);
	}

	@Override
	public synchronized void state(HawkState state) {
		currentState = state;
		for (IStateListener l : this) {
			l.state(state);
		}
		this.notifyAll();
	}

	public HawkState getCurrentState() {
		return currentState;
	}
}
