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

import java.util.HashSet;

import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;

/**
 * Allows treating a set of {@link IGraphChangeListener}s as a single one. By itself,
 * this change listener doesn't do anything, so it can be used as a "null" listener
 * if left empty.
 */
public class CompositeGraphChangeListener extends HashSet<IGraphChangeListener> implements IGraphChangeListener {
	private static final long serialVersionUID = 639097671453202757L;

	@Override
	public void synchroniseStart() {
		for (IGraphChangeListener l : this) {
			l.synchroniseStart();
		}
	}

	@Override
	public void synchroniseEnd() {
		for (IGraphChangeListener l : this) {
			l.synchroniseEnd();
		}
	}

	@Override
	public void changeStart() {
		for (IGraphChangeListener l : this) {
			l.changeStart();
		}
	}

	@Override
	public void changeSuccess() {
		for (IGraphChangeListener l : this) {
			l.changeSuccess();
		}
	}

	@Override
	public void changeFailure() {
		for (IGraphChangeListener l : this) {
			l.changeFailure();
		}
	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {
		for (IGraphChangeListener l : this) {
			l.fileAddition(s, fileNode);
		}
	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {
		for (IGraphChangeListener l : this) {
			l.metamodelAddition(pkg, pkgNode);
		}
	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {
		for (IGraphChangeListener l : this) {
			l.classAddition(cls, clsNode);
		}
	}

	@Override
	public void modelElementAddition(VcsCommitItem s, IHawkObject element,
			IGraphNode elementNode, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			l.modelElementAddition(s, element, elementNode, isTransient);
		}
	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			l.modelElementRemoval(s, elementNode, isTransient);
		}
	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s,
			IHawkObject eObject, String attrName, Object oldValue,
			Object newValue, IGraphNode elementNode, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			l.modelElementAttributeUpdate(s, eObject, attrName, oldValue, newValue, elementNode, isTransient);
		}
	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s,
			IHawkObject eObject, String key, IGraphNode node, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			l.modelElementAttributeRemoval(s, eObject, key, node, isTransient);
		}
	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			l.referenceAddition(s, source, destination, edgelabel, isTransient);
		}
	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			l.referenceRemoval(s, source, destination, edgelabel, isTransient);
		}
	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {
		for (IGraphChangeListener l : this) {
			l.fileRemoval(s, fileNode);
		}
	}

	@Override
	public String getName() {
		final StringBuffer sbuf = new StringBuffer("composite(");
		boolean first = false;
		for (IGraphChangeListener l : this) {
			if (first) {
				first = false;
			} else {
				sbuf.append(", ");
			}
			sbuf.append(l.getName());
		}
		sbuf.append(")");
		return sbuf.toString();
	}

}
