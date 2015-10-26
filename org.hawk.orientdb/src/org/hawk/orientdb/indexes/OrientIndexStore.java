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
package org.hawk.orientdb.indexes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hawk.orientdb.OrientDatabase;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;

/**
 * Stores metadata about the indexes that have been created for Hawk, as Hawk
 * expects it.
 */
public class OrientIndexStore {

	private static final String VCLASS = "hawkIndexStore";
	private static final String NODEIDX_PROP = "nodeIndexes";
	private static final String NODEFIELDIDX_PREFIX = "nidx_";

	private final Vertex vIndexStore;

	public OrientIndexStore(Vertex vIndexStore) {
		this.vIndexStore = vIndexStore;
	}

	public static OrientIndexStore getInstance(OrientDatabase db) {
		if (db.getGraph().getVertexType(VCLASS) == null) {
			db.enterBatchMode();
			db.getGraph().createVertexType(VCLASS);
			db.exitBatchMode();
		}

		final OrientBaseGraph graph = db.getGraph();
		Iterator<Vertex> itIndexStore = graph.getVerticesOfClass(VCLASS).iterator();
		Vertex vIndexStore = null;
		if (!itIndexStore.hasNext()) {
			final HashMap<String, Object> idxStoreProps = new HashMap<>();
			idxStoreProps.put(NODEIDX_PROP, new String[]{});
			vIndexStore = db.createNode(idxStoreProps, VCLASS).getVertex();
		} else {
			vIndexStore = itIndexStore.next();
		}

		return new OrientIndexStore(vIndexStore);
	}

	/**
	 * Adds a index name to the Hawk store.
	 */
	public void addNodeIndex(String indexName) {
		final Set<String> setNames = getNodeIndexNames();
		setNames.add(indexName);
		final String[] extendedNames = setNames.toArray(new String[setNames.size()]);
		vIndexStore.setProperty(NODEIDX_PROP, extendedNames);
	}

	/**
	 * Adds an index+field name pair to the Hawk store.
	 */
	public void addNodeFieldIndex(String indexName, String field) {
		addNodeIndex(indexName);

		final String propName = NODEFIELDIDX_PREFIX + indexName;
		final Set<String> setNames = getNodeFieldIndexNames(indexName);
		setNames.add(field);
		final String[] extNames = setNames.toArray(new String[setNames.size()]);
		vIndexStore.setProperty(propName, extNames);
	}

	/**
	 * Returns all the field names associated with the node index with the
	 * specified <code>indexName</code>.
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getNodeFieldIndexNames(final String indexName) {
		final String propName = NODEFIELDIDX_PREFIX + indexName;
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

	/**
	 * Returns all the node index names.
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getNodeIndexNames() {
		final Object oNames = vIndexStore.getProperty(NODEIDX_PROP);
		if (oNames == null) {
			return new HashSet<String>();
		} else if (oNames instanceof Collection) {
			return new HashSet<String>((Collection<String>)oNames);
		} else {
			return new HashSet<String>(Arrays.asList((String[])oNames));
		}
	}

	/** Removes a node index from the store. */
	public void removeNodeIndex(String indexName) {
		final String propName = NODEFIELDIDX_PREFIX + indexName;
		vIndexStore.removeProperty(propName);

		Set<String> nodeIndexNames = getNodeIndexNames();
		nodeIndexNames.remove(indexName);
		vIndexStore.setProperty(NODEIDX_PROP, nodeIndexNames.toArray(new String[nodeIndexNames.size()]));
	}

}
