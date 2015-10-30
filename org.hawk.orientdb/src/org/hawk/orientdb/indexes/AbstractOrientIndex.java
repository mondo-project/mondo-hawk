package org.hawk.orientdb.indexes;

import org.hawk.orientdb.OrientDatabase;
import org.hawk.orientdb.OrientNameCleaner;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.index.OIndexManagerProxy;
import com.orientechnologies.orient.core.index.OSimpleKeyIndexDefinition;
import com.orientechnologies.orient.core.metadata.schema.OType;

public class AbstractOrientIndex {

	public enum IndexType { NODE, EDGE };
	private static final String SEPARATOR_SBTREE = "_@sbtree@_";

	protected final String name;
	protected final OrientDatabase graph;
	protected final IndexType type;

	/**
	 * Normalizes a value expression so it'll always be either an Integer, a
	 * Double or a String.
	 */
	protected static Object normalizeValue(Object valueExpr) {
		if (valueExpr instanceof Byte || valueExpr instanceof Short || valueExpr instanceof Long) {
			valueExpr = ((Number)valueExpr).intValue();
		} else if (valueExpr instanceof Float) {
			valueExpr = ((Float)valueExpr).doubleValue();
		} else if (valueExpr instanceof String || valueExpr instanceof Integer || valueExpr instanceof Double) {
			return valueExpr;
		}
		return valueExpr.toString();
	}

	public String getName() {
		return name;
	}

	public AbstractOrientIndex(String name, OrientDatabase graph, IndexType type) {
		this.name = name;
		this.graph = graph;
		this.type = type;
	}

	protected OIndex<?> getOrCreateFieldIndex(final String field, final Class<?> valueClass) {
		final String idxName = getSBTreeIndexName(field);
		final OIndexManager indexManager = getIndexManager();
		OIndex<?> idx = indexManager.getIndex(idxName);
	
		if (idx == null) {
			createIndex(field, valueClass);
	
			// We need to fetch again the index: using the odeone that was just
			// created will result in multithreading exceptions from OrientDB
			idx = indexManager.getIndex(idxName);
		}
		return idx;
	}

	/**
	 * Creates the SBTree index paired to this field within this logical index.
	 */
	protected void createIndex(final String field, final Class<?> keyClass) {
		final OIndexManager indexManager = getIndexManager();
	
		// Indexes have to be created outside transactions
		final boolean wasTransactional = graph.currentMode() == OrientDatabase.TX_MODE;
		if (wasTransactional) {
			graph.enterBatchMode();
		}
	
		// Index key type
		OType keyType = OType.STRING;
		if (keyClass == Byte.class || keyClass == Short.class || keyClass == Integer.class || keyClass == Long.class) {
			keyType = OType.INTEGER;
		} else if (keyClass == Float.class || keyClass == Double.class) {
			keyType = OType.DOUBLE;
		}
	
		// Create SBTree NOTUNIQUE index
		final String idxName = getSBTreeIndexName(field);
		final OSimpleKeyIndexDefinition indexDef = new OSimpleKeyIndexDefinition(keyType);
		indexManager.createIndex(idxName, "NOTUNIQUE", indexDef, null, null, null, "SBTREE");

		if (type == IndexType.NODE) {
			graph.getIndexStore().addNodeFieldIndex(name, field);
		} else {
			graph.getIndexStore().addEdgeFieldIndex(name, field);
		}

		if (wasTransactional) {
			graph.exitBatchMode();
		}
	}

	protected String getSBTreeIndexName(final String field) {
		return OrientNameCleaner.escapeToField(name + SEPARATOR_SBTREE + field);
	}

	protected OIndexManager getIndexManager() {
		final ODatabaseDocumentTx rawGraph = graph.getGraph();
		final OIndexManagerProxy indexManager = rawGraph.getMetadata().getIndexManager();
		return indexManager;
	}

}