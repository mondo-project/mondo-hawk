/*******************************************************************************
 * Copyright (c) 2015 The University of York.
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
package org.hawk.core.runtime;

import java.util.LinkedHashSet;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;

/**
 * Allows treating a set of {@link IGraphChangeListener}s as a single one. By
 * itself, this change listener doesn't do anything, so it can be used as a
 * "null" listener if left empty.
 *
 * Since this is a HashSet, if we want to avoid having "duplicate" listeners,
 * the easiest way is to have the {@link IGraphChangeListener} implementations
 * define hashCode and equals appropriately.
 */
public class CompositeGraphChangeListener extends LinkedHashSet<IGraphChangeListener>
		implements IGraphChangeListener {
	private static final long serialVersionUID = 639097671453202757L;

	@Override
	public void synchroniseStart() {
		for (IGraphChangeListener l : this) {
			try {
				l.synchroniseStart();
			} catch (Exception e) {
				//
			}
		}
	}

	@Override
	public void synchroniseEnd() {
		for (IGraphChangeListener l : this) {
			try {
				l.synchroniseEnd();
			} catch (Exception e) {
				//
			}
		}
	}

	@Override
	public void changeStart() {
		for (IGraphChangeListener l : this) {
			try {
				l.changeStart();
			} catch (Exception e) {
				//
			}
		}
	}

	@Override
	public void changeSuccess() {
		for (IGraphChangeListener l : this) {
			try {
				l.changeSuccess();
			} catch (Exception e) {
				//
			}
		}
	}

	@Override
	public void changeFailure() {
		for (IGraphChangeListener l : this) {
			try {
				l.changeFailure();
			} catch (Exception e) {
				//
			}
		}
	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {
		for (IGraphChangeListener l : this) {
			try {
				l.fileAddition(s, fileNode);
			} catch (Exception e) {
				//
			}

		}
	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {
		for (IGraphChangeListener l : this) {
			try {
				l.metamodelAddition(pkg, pkgNode);
			} catch (Exception e) {
				//
			}

		}
	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {
		for (IGraphChangeListener l : this) {
			try {
				l.classAddition(cls, clsNode);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	@Override
	public void modelElementAddition(VcsCommitItem s, IHawkObject element,
			IGraphNode elementNode, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			try {
				l.modelElementAddition(s, element, elementNode, isTransient);
			} catch (Exception e) {
				//
			}

		}
	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode,
			boolean isTransient) {
		for (IGraphChangeListener l : this) {
			try {
				l.modelElementRemoval(s, elementNode, isTransient);
			} catch (Exception e) {
				//
			}

		}
	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s,
			IHawkObject eObject, String attrName, Object oldValue,
			Object newValue, IGraphNode elementNode, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			try {
				l.modelElementAttributeUpdate(s, eObject, attrName, oldValue,
						newValue, elementNode, isTransient);
			} catch (Exception e) {
				//
			}

		}
	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s,
			IHawkObject eObject, String key, IGraphNode node,
			boolean isTransient) {
		for (IGraphChangeListener l : this) {
			try {
				l.modelElementAttributeRemoval(s, eObject, key, node,
						isTransient);
			} catch (Exception e) {
				//
			}

		}
	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			try {
				l.referenceAddition(s, source, destination, edgelabel,
						isTransient);
			} catch (Exception e) {
				//
			}

		}
	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
		for (IGraphChangeListener l : this) {
			try {
				l.referenceRemoval(s, source, destination, edgelabel,
						isTransient);
			} catch (Exception e) {
				//
			}

		}
	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {
		for (IGraphChangeListener l : this) {
			try {
				l.fileRemoval(s, fileNode);
			} catch (Exception e) {
				//
			}

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

	@Override
	public void setModelIndexer(IModelIndexer m) {
		// not used

	}

}
