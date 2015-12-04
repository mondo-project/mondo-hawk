/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.queryaware;

import java.io.File;
import java.util.Map;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.EOLQueryEngine;

public class QueryAwareEOLQueryEngine extends EOLQueryEngine
// implements IOperationContributorProvider
{

	@Override
	public Object query(IGraphDatabase g, String query,
			Map<String, String> context) throws InvalidQueryException,
			QueryExecutionException {
		System.err
				.println("warning this plugin is no longer supported, it will run a usual eolqueryengine");
		return super.query(g, query, context);
	}

	@Override
	public Object query(IGraphDatabase g, File query,
			Map<String, String> context) throws InvalidQueryException,
			QueryExecutionException {
		System.err
				.println("warning this plugin is no longer supported, it will run a usual eolqueryengine");
		return super.query(g, query, context);
	}

}
