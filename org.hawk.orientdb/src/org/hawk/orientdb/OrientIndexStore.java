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
package org.hawk.orientdb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores metadata about the indexes that have been created for Hawk, as Hawk
 * expects it.
 */
public class OrientIndexStore {

	private static final String NODEIDX_PROP = "nodeIndexes";
	private static final String NODEFIELDIDX_PREFIX = "nidx_";

	private static final String EDGEIDX_PROP = "edgeIndexes";
	private static final String EDGEFIELDIDX_PREFIX = "eidx_";

	private final OrientNode vIndexStore;

	public OrientIndexStore(OrientNode n) {
		this.vIndexStore = n;
		if (vIndexStore.getProperty(NODEIDX_PROP) == null) {
			vIndexStore.setProperty(NODEIDX_PROP, new String[] {});
		}
		if (vIndexStore.getProperty(EDGEIDX_PROP) == null) {
			vIndexStore.setProperty(EDGEIDX_PROP, new String[] {});
		}
	}

	/**
	 * Adds a node index name to the Hawk store.
	 */
	public void addNodeIndex(String indexName) {
		final String property = NODEIDX_PROP;
		addIndex(indexName, property);
	}

	/**
	 * Adds an edge index name to the Hawk store.
	 */
	public void addEdgeIndex(String indexName) {
		final String property = EDGEIDX_PROP;
		addIndex(indexName, property);
	}

	/**
	 * Adds a node index+field name pair to the Hawk store.
	 */
	public void addNodeFieldIndex(String indexName, String field) {
		addNodeIndex(indexName);
		addFieldIndex(NODEFIELDIDX_PREFIX, indexName, field);
	}

	/**
	 * Adds an edge index+field name pair to the Hawk store.
	 */
	public void addEdgeFieldIndex(String indexName, String field) {
		addEdgeIndex(indexName);
		addFieldIndex(EDGEFIELDIDX_PREFIX, indexName, field);
	}

	/**
	 * Returns all the field names associated with the node index with the
	 * specified <code>indexName</code>.
	 */
	public Set<String> getNodeFieldIndexNames(final String indexName) {
		return getFieldIndexNames(NODEFIELDIDX_PREFIX, indexName);
	}

	/**
	 * Returns all the field names associated with the edge index with the
	 * specified <code>indexName</code>.
	 */
	public Set<String> getEdgeFieldIndexNames(final String indexName) {
		return getFieldIndexNames(EDGEFIELDIDX_PREFIX, indexName);
	}

	/**
	 * Returns all the node index names.
	 */
	public Set<String> getNodeIndexNames() {
		return getIndexNames(NODEIDX_PROP);
	}

	/**
	 * Returns all the edge index names.
	 */
	public Set<String> getEdgeIndexNames() {
		return getIndexNames(EDGEIDX_PROP);
	}

	/** Removes a node index from the store. */
	public void removeNodeIndex(String indexName) {
		final String propName = NODEFIELDIDX_PREFIX + indexName;
		vIndexStore.removeProperty(propName);

		Set<String> nodeIndexNames = getNodeIndexNames();
		nodeIndexNames.remove(indexName);
		vIndexStore.setProperty(NODEIDX_PROP, nodeIndexNames.toArray(new String[nodeIndexNames.size()]));
	}

	private void addIndex(String indexName, final String property) {
		final Set<String> setNames = getNodeIndexNames();
		setNames.add(indexName);
		final String[] extendedNames = setNames.toArray(new String[setNames.size()]);
		vIndexStore.setProperty(property, extendedNames);
	}

	private void addFieldIndex(final String prefix, String indexName, String field) {
		final String propName = prefix + indexName;
		final Set<String> setNames = getNodeFieldIndexNames(indexName);
		setNames.add(field);
		final String[] extNames = setNames.toArray(new String[setNames.size()]);
		vIndexStore.setProperty(propName, extNames);
	}

	@SuppressWarnings("unchecked")
	private Set<String> getFieldIndexNames(final String prefix, final String indexName) {
		final String propName = prefix + indexName;
		final Set<String> setNames = new HashSet<>();
		Object oNames = vIndexStore.getProperty(propName);
		if (oNames instanceof Collection) {
			setNames.addAll((Collection<String>)oNames);
		}
		else if (oNames != null) {
			setNames.addAll(Arrays.asList((String[])oNames));
		}
		return setNames;
	}

	@SuppressWarnings("unchecked")
	private Set<String> getIndexNames(final String property) {
		final Object oNames = vIndexStore.getProperty(property);
		if (oNames instanceof Collection) {
			return new HashSet<String>((Collection<String>)oNames);
		} else if (oNames instanceof String[]) {
			return new HashSet<String>(Arrays.asList((String[])oNames));
		} else {
			return Collections.emptySet();
		}
	}

}
