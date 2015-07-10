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
package org.hawk.core.graph;

import java.util.List;

public interface IGraphChangeDescriptor {

	public abstract int getUnresolvedReferences();

	public abstract int getUnresolvedDerivedProperties();

	public abstract void setUnresolvedDerivedProperties(int i);

	public abstract void setUnresolvedReferences(int i);

	public abstract boolean getErrorState();

	void setErrorState(boolean error);

	void addChanges(List<IGraphChange> e);
	
	void addChanges(IGraphChange e);

	List<IGraphChange> getChanges();

	public String getName();

	public void setName(String name);

}