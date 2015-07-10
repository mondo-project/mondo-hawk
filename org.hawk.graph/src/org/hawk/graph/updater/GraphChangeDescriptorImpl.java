/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.updater;

import java.util.LinkedList;
import java.util.List;

import org.hawk.core.graph.IGraphChange;
import org.hawk.core.graph.IGraphChangeDescriptor;

public class GraphChangeDescriptorImpl implements IGraphChangeDescriptor {

	private boolean error = false;
	private int unr = -1;
	private int undp = -1;
	private String name;

	private List<IGraphChange> changes = new LinkedList<>();

	public GraphChangeDescriptorImpl(String name) {
		setName(name);
	}

	@Override
	public int getUnresolvedReferences() {
		return unr;
	}

	@Override
	public int getUnresolvedDerivedProperties() {
		return undp;
	}

	@Override
	public void setErrorState(boolean error) {
		this.error = error;
	}

	@Override
	public void setUnresolvedDerivedProperties(int i) {
		undp = i;

	}

	@Override
	public void setUnresolvedReferences(int i) {
		unr = i;
	}

	@Override
	public boolean getErrorState() {
		return error;
	}

	@Override
	public void addChanges(List<IGraphChange> e) {
		changes.addAll(e);
	}

	@Override
	public void addChanges(IGraphChange e) {
		changes.add(e);
	}

	@Override
	public List<IGraphChange> getChanges() {
		return changes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;

	}

}
