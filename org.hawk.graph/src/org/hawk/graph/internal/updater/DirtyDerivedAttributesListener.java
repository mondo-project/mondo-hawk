/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - original idea and implementation
 *     Antonio Garcia-Dominguez - rearrange as graph change listener
 ******************************************************************************/
package org.hawk.graph.internal.updater;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;

/**
 * Graph change listener that collects the graph nodes whose derived attributes
 * should be updated, and marks them as dirty on the fly with
 * <code>_NYD##...</code> properties.
 */
public class DirtyDerivedAttributesListener implements IGraphChangeListener {

	private final class PendingEntry implements Entry<String, String> {
		String key;
		String value;

		public PendingEntry(String k, String v) {
			key = k;
			value = v;
		}

		@Override
		public String setValue(String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public int hashCode() {
			final int prime = 29;
			int result = Integer.MIN_VALUE;
			result += key.hashCode() * prime ^ 1;
			result += value.hashCode() * prime ^ 2;
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Entry<?, ?>)
				return ((Entry<?, ?>) o).getKey().equals(key)
						&& ((Entry<?, ?>) o).getValue().equals(value);
			else
				return false;
		}

	}

	private final Set<IGraphNode> nodesToBeUpdated = new HashSet<>();
	private IGraphDatabase db;
	private Set<IGraphNode> markedForRemoval = new HashSet<>();
	private Set<Entry<String, String>> pending = new HashSet<>();

	public DirtyDerivedAttributesListener(IGraphDatabase graph) {
		this.db = graph;

	}

	/**
	 * needs to be in transaction
	 * 
	 * @return
	 */
	public Set<IGraphNode> getNodesToBeUpdated() {
		nodesToBeUpdated.addAll(resolvePending());
		nodesToBeUpdated.removeAll(markedForRemoval);
		return nodesToBeUpdated;
	}

	private Set<IGraphNode> resolvePending() {

		Set<IGraphNode> resolved = new HashSet<>();

		for (Entry<String, String> e : pending)
			//
			markDependentToBeUpdated(e.getKey(), e.getValue());

		pending.clear();
		return resolved;

	}

	@Override
	public String getName() {
		return "Internal listener for finding dirty attributes";
	}

	@Override
	public void synchroniseStart() {
		// nothing to do
	}

	@Override
	public void synchroniseEnd() {
		// nothing to do
	}

	@Override
	public void changeStart() {
		// nothing to do
	}

	@Override
	public void changeSuccess() {
		// nothing to do
	}

	@Override
	public void changeFailure() {
		nodesToBeUpdated.clear();
		pending.clear();
	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {
		// nothing to do
	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {
		// nothing to do
	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {
		// nothing to do
	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {
		// nothing to do
	}

	@Override
	public void modelElementAddition(VcsCommitItem s, IHawkObject element,
			IGraphNode elementNode, boolean isTransient) {
		if (!isTransient) {
			markDependentToBeUpdated(elementNode.getId().toString());
		}
	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode,
			boolean isTransient) {

		if (!isTransient) {
			markDependentToBeUpdated(elementNode.getId().toString());
		} else {
			markForRemoval(elementNode);
		}
	}

	private void markForRemoval(IGraphNode elementNode) {
		markedForRemoval.add(elementNode);

	}

	private Entry<String, String> createEntry(String k, String v) {
		Entry<String, String> ret = new PendingEntry(k, v);
		return ret;
	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s,
			IHawkObject eObject, String attrName, Object oldValue,
			Object newValue, IGraphNode elementNode, boolean isTransient) {
		if (!isTransient) {
			pending.add(createEntry(elementNode.getId().toString(), attrName));
		}
	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s,
			IHawkObject eObject, String attrName, IGraphNode node,
			boolean isTransient) {
		if (!isTransient) {
			pending.add(createEntry(node.getId().toString(), attrName));
		}
	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
		if (!isTransient) {
			pending.add(createEntry(source.getId().toString(), edgelabel));
		}
	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source,
			IGraphNode destination, String edgelabel, boolean isTransient) {
		if (!isTransient) {
			pending.add(createEntry(source.getId().toString(), edgelabel));
		}
	}

	private void markDependentToBeUpdated(final String key) {
		pending.add(createEntry(key, "*"));
	}

	private void markDependentToBeUpdated(final String key, final String value) {

		final IGraphNodeIndex idx = db
				.getOrCreateNodeIndex("derivedaccessdictionary");

		Iterable<IGraphNode> nodes = idx.query(key, value);

		for (IGraphNode node : nodes) {
			markDependentToBeUpdated(node);
		}

	}

	private void markDependentToBeUpdated(IGraphNode node) {
		final Iterable<IGraphEdge> incoming = node.getIncoming();
		final IGraphEdge firstIncoming = incoming.iterator().next();
		final String derivedPropertyName = firstIncoming.getType();

		if (node.getPropertyKeys().contains(derivedPropertyName)) {
			node.setProperty(derivedPropertyName,
					"_NYD##" + node.getProperty("derivationlogic"));
			nodesToBeUpdated.add(node);
		} else {
			System.err
					.println("updateDerivedAttributes() -- derived attribute node did not contain property: "
							+ derivedPropertyName);
		}
	}

	@Override
	public void setModelIndexer(IModelIndexer m) {
		// not used
	}

}
