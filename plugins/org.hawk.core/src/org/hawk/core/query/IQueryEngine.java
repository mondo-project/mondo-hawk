/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - expand javadocs, add
 *       PROPERTY_FILEFIRST, PROPERTY_SUBTREECONTEXT,
 *       PROPERTY_ISCANCELLED_CALLABLE
 ******************************************************************************/
package org.hawk.core.query;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphNode;

public interface IQueryEngine {

	/**
	 * If true, contextful getAllOf will start from the files in
	 * {@link #PROPERTY_FILECONTEXT} then check types. Can be useful when querying a
	 * small fragment out of a large graph.
	 * 
	 * If false or unset, the search will start from the types and then filter by
	 * file: this is good when looking for a rare type across a large graph.
	 */
	public static final String PROPERTY_FILEFIRST = "FILEFIRST";

	/**
	 * If set to the full repository path (starting with '/') of a file, contextful
	 * getAllOf(...) will only return the elements of that type inside the
	 * containment subtree rooted in that file. Model.allContents will also be
	 * limited to the elements in that subtree.
	 * 
	 * The first type this request is received, the query engine will create derived
	 * edges from the elements of that type to its ancestors, which will be followed
	 * in reverse from the root of the subtree to provide results quickly.
	 */
	public static final String PROPERTY_SUBTREECONTEXT = "SUBTREE";

	/**
	 * If {@link #PROPERTY_SUBTREECONTEXT} is used and this is set to "true",
	 * Type.all queries will implicitly register derived edges from Type to all its
	 * direct and indirect containers for its type, which will speed up subsequent
	 * accesses by traversing the derived edges in reverse from the local root(s) of
	 * the files in scope. A local root is understood as an element that is not contained
	 * by any other element in the same file.
	 * 
	 * If false or unset, the behaviour will depend on whether
	 * {@link #PROPERTY_FILEFIRST} has been set or not. This option takes priority
	 * over {@link #PROPERTY_FILEFIRST}.
	 *
	 * @see #PROPERTY_SUBTREECONTEXT
	 * @see #PROPERTY_FILEFIRST
	 */
	public static final String PROPERTY_SUBTREE_DERIVEDALLOF = "SUBTREE_DERIVEDALLOF";

	/**
	 * If set to a comma-separated list of repository path patterns (where '*' is
	 * 'any 0+ characters, glob style), limits Model.allContents and contextful
	 * getAllOf(...) to the contents of these files.
	 *
	 * @see #PROPERTY_FILEFIRST
	 */
	public static final String PROPERTY_FILECONTEXT = "FILE";

	/**
	 * If set to the full URI of a repository, results will be limited to the files within
	 * this repository.
	 *
	 * @see #PROPERTY_FILECONTEXT
	 * @see #PROPERTY_SUBTREECONTEXT
	 */
	public static final String PROPERTY_REPOSITORYCONTEXT = "REPOSITORY";

	/**
	 * If set to a list of metamodel URIs, it will resolve ambiguous type references by
	 * using the first metamodel in the list that contains a type with that name.
	 */
	public static final String PROPERTY_DEFAULTNAMESPACES = "DEFAULTNAMESPACES";

	/**
	 * If set to true, limits incoming and outgoing edges from any model element to the
	 * same context defined by {@link #PROPERTY_FILECONTEXT}, {@link #PROPERTY_REPOSITORYCONTEXT}
	 * and/or {@link #PROPERTY_SUBTREECONTEXT}.
	 */
	public static final String PROPERTY_ENABLE_TRAVERSAL_SCOPING = "ENABLE_TRAVERSAL_SCOPING";

	/**
	 * If used, this key should be associated to a Map<String, Object> with
	 * additional local variables for the underlying query.
	 */
	public static final String PROPERTY_ARGUMENTS = "ARGUMENTS";

	/**
	 * If set, this key should be associated to a {@link java.util.concurrent.Callable<Boolean>} that will return
	 * <code>true</code> if the query should be cancelled, or <code>false</code>
	 * otherwise. The query engine will control the timing used to invoke the function.
	 */
	public static final String PROPERTY_ISCANCELLED_CALLABLE = "IS_CANCELLED_F";

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
