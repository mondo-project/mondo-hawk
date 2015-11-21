/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core.runtime;

import java.util.LinkedHashSet;

import org.hawk.core.IStateListener;

public class CompositeStateListener extends LinkedHashSet<IStateListener> implements IStateListener {
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
			((IStateListener)e).removed();
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
		System.out.println(s);
	}

	@Override
	public void error(String s) {
		for (IStateListener l : this) {
			l.error(s);
		}
		System.err.println(s);
	}

	@Override
	public void state(HawkState state) {
		currentState = state;
		for (IStateListener l : this) {
			l.state(state);
		}
	}
}
