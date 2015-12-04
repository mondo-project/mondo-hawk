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
package org.hawk.core.query;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;

public interface IQueryEngine {

	public static final String PROPERTY_FILECONTEXT = "FILE";
	public static final String PROPERTY_REPOSITORYCONTEXT = "REPOSITORY";
	public static final String PROPERTY_ENABLE_CACHING = "ENABLE_CACHING";

	Object contextlessQuery(IGraphDatabase g, String query) throws InvalidQueryException, QueryExecutionException;

	Object contextlessQuery(IGraphDatabase g, File query) throws InvalidQueryException, QueryExecutionException;

	Object contextfullQuery(IGraphDatabase g, String query,
			Map<String, String> context) throws InvalidQueryException, QueryExecutionException;

	Object contextfullQuery(IGraphDatabase g, File query,
			Map<String, String> context) throws InvalidQueryException, QueryExecutionException;

	IAccessListener calculateDerivedAttributes(IGraphDatabase g,
			Iterable<IGraphNode> nodes) throws InvalidQueryException, QueryExecutionException;

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

}
