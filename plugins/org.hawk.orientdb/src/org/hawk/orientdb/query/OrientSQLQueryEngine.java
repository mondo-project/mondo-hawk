/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York, Aston University.
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
package org.hawk.orientdb.query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.IAccessListener;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.orientdb.OrientDatabase;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentHelper;
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLSelect;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;

/**
 * Gives raw access to OrientDB SELECT SQL queries. 
 */
public class OrientSQLQueryEngine implements IQueryEngine {

	private class OrientNodeWrapper implements IGraphNodeReference {
		private final ORID identity;
		private final OrientDatabase db;

		private OrientNodeWrapper(ORID identity, OrientDatabase db) {
			this.identity = identity;
			this.db = db;
		}

		@Override
		public String getTypeName() {
			final IGraphNode typeNode = getNode().getEdgesWithType("ofType").iterator().next().getEndNode();
			return typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
		}

		@Override
		public IGraphNode getNode() {
			return db.getNodeById(identity);
		}

		@Override
		public String getId() {
			return identity.toString();
		}

		@Override
		public IQueryEngine getContainerModel() {
			return OrientSQLQueryEngine.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((identity == null) ? 0 : identity.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OrientNodeWrapper other = (OrientNodeWrapper) obj;
			if (identity == null) {
				if (other.identity != null)
					return false;
			} else if (!identity.equals(other.identity))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ONW|id:" + identity + "|" + getTypeName();
		}
	}

	@Override
	public IAccessListener calculateDerivedAttributes(IModelIndexer m, Iterable<IGraphNode> nodes)
			throws InvalidQueryException, QueryExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public List<String> validate(String derivationlogic) {
		OCommandSQL cmd = new OCommandSQL(derivationlogic);
		try {
			new OCommandExecutorSQLSelect().parse(cmd);
		} catch (OCommandSQLParsingException ex) {
			return Collections.singletonList(ex.toString());
		}
		return Collections.emptyList();
	}

	@Override
	public void setDefaultNamespaces(String defaultNamespaces) {
		// nothing to do
	}

	@Override
	public Object query(IModelIndexer m, String query, Map<String, Object> context)
			throws InvalidQueryException, QueryExecutionException {
		if (m.getGraph() instanceof OrientDatabase) {
			final OrientDatabase graph = (OrientDatabase) m.getGraph();
			try (final IGraphTransaction tx = graph.beginTransaction()) {
				Object val = query(graph, query);
				tx.success();
				return val;
			}
		} else {
			throw new InvalidQueryException("Backend is not OrientDB - cannot use this native query driver");
		}
	}

	@Override
	public Object query(IModelIndexer m, File query, Map<String, Object> context) throws InvalidQueryException, QueryExecutionException {
		final StringBuffer sbuf = new StringBuffer();
		try (final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(query), "UTF-8"))) {
			String line;
			while ((line = r.readLine()) != null) {
				sbuf.append(line);
			}
		} catch (Exception e) {
			System.err.println("error reading eol code file:");
			e.printStackTrace();
		}
		return query(m, sbuf.toString(), context);
	}

	private Object query(final OrientDatabase db, final String query) throws InvalidQueryException {
		OCommandSQL cmd = new OCommandSQL(query);
		try {
			new OCommandExecutorSQLSelect().parse(cmd);
			Object result = db.getGraph().command(cmd).execute();

			// Wrap ODocuments into graph nodes whenever possible
			if (result instanceof ODocument) {
				final ORID identity = ((ODocument) result).getIdentity();
				return identity.isValid() ? db.getNodeById(identity) : null;
			} else if (result instanceof Iterable) {
				List<Object> results = new ArrayList<>();
				for (Object elem : (Iterable<?>)result) {
					if (elem instanceof ODocument) {
						final ODocument odoc = (ODocument) elem;
						final ORID identity = odoc.getIdentity();
						if (identity.isPersistent()) {
							// This is a proper model element node - return it as a graph node reference
							results.add(new OrientNodeWrapper(identity, db));
						} else {
							// This is a temporary result document - return it as a standard map,
							// removing the Orient @rid (which isn't meaningful here anyway)
							Map<String, Object> mapResult = odoc.toMap();
							mapResult.remove(ODocumentHelper.ATTRIBUTE_RID);
							results.add(mapResult);
						}
					}
				}
				return results;
			} else {
				return result;
			}

		} catch (OCommandSQLParsingException ex) {
			throw new InvalidQueryException(ex);
		}
	}
}
