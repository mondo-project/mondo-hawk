/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.query;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphNode;

public interface IQueryEngine {

	public static final String PROPERTY_FILECONTEXT = "FILE";
	public static final String PROPERTY_REPOSITORYCONTEXT = "REPOSITORY";
	public static final String PROPERTY_DEFAULTNAMESPACES = "DEFAULTNAMESPACES";
	public static final String PROPERTY_ENABLE_CACHING = "ENABLE_CACHING";
	public static final String PROPERTY_ENABLE_TRAVERSAL_SCOPING = "ENABLE_TRAVERSAL_SCOPING";

	/**
	 * If used, this key should be associated to a Map<String, Object> with
	 * additional local variables for the underlying query.
	 */
	public static final String PROPERTY_ARGUMENTS = "ARGUMENTS";

	IAccessListener calculateDerivedAttributes(IModelIndexer m,
			Iterable<IGraphNode> nodes) throws InvalidQueryException,
			QueryExecutionException;

	String getType();

	List<String> validate(String derivationlogic);

	/**
	 * Changes the default namespaces used to resolve ambiguous type references.
	 *
	 * @param defaultNamespaces
	 *            Comma-separated list of namespace URIs to be used as the
	 *            default namespaces in later queries.
	 */
	void setDefaultNamespaces(String defaultNamespaces);

	/**
	 * Callers of this method should honour the IModelIndexer state when
	 * attempting to query Hawk, only accepting HawkState.RUNNING from
	 * CompositeStateListener().getCurrentState().
	 * 
	 * @throws QueryExecutionException
	 *             The model indexer is currently not available.
	 * @throws InvalidQueryException
	 *             The query expression is not parsable by the engine.
	 */
	Object query(IModelIndexer m, String query, Map<String, Object> context)
			throws InvalidQueryException, QueryExecutionException;

	/**
	 * Callers of this method should honour the IModelIndexer state when
	 * attempting to query Hawk, only accepting HawkState.RUNNING from
	 * CompositeStateListener().getCurrentState()
	 * 
	 * @throws QueryExecutionException
	 *             The model indexer is currently not available. * @throws
	 *             InvalidQueryException The query expression is not parsable by
	 *             the engine.
	 */
	Object query(IModelIndexer m, File query, Map<String, Object> context)
			throws InvalidQueryException, QueryExecutionException;

}
