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
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.IAccessListener;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.orientdb.OrientDatabase;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLSelect;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;

/**
 * Gives raw access to OrientDB SELECT SQL queries. 
 */
public class OrientSQLQueryEngine implements IQueryEngine {

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
	public Object query(IModelIndexer m, String query, Map<String, String> context)
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
	public Object query(IModelIndexer m, File query, Map<String, String> context) throws InvalidQueryException, QueryExecutionException {
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
				List<IGraphNode> results = new ArrayList<>();
				for (Object elem : (Iterable<?>)result) {
					if (elem instanceof ODocument) {
						ORID identity = ((ODocument) elem).getIdentity();
						if (identity.isValid()) {
							results.add(db.getNodeById(identity));
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
