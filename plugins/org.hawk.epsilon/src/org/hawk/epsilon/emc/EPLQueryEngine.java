/*******************************************************************************
 * Copyright (c) 2016 Aston University.
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
package org.hawk.epsilon.emc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.epl.EplModule;
import org.eclipse.epsilon.epl.execute.PatternMatch;
import org.eclipse.epsilon.epl.execute.PatternMatchModel;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.epsilon.emc.tracking.AccessListener;

/**
 * Adds support for EPL queries to Hawk. Derived attributes are not supported.
 */
public class EPLQueryEngine extends EOLQueryEngine {

	public static final String TYPE = "org.hawk.epsilon.emc.EPLQueryEngine";
	public static final String RULENAME_KEY = "_rulename";

	@Override
	public AccessListener calculateDerivedAttributes(IModelIndexer m, Iterable<IGraphNode> nodes) {
		throw new UnsupportedOperationException();
	}

	
	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	protected IEolModule createModule() {
		return new EplModule();
	}

	@Override
	protected Object runQuery(IEolModule module) throws QueryExecutionException {
		final Object ret = super.runQuery(module);
		if (ret instanceof PatternMatchModel) {
			final PatternMatchModel matchModel = (PatternMatchModel) ret;

			final List<Map<String, Object>> results = new ArrayList<>(matchModel.getMatches().size());
			for (PatternMatch match : matchModel.getMatches()) {
				final Map<String, Object> result = new HashMap<>(match.getRoleBindings());
				result.put(RULENAME_KEY, match.getPattern().getName());
				results.add(result);
			}
			return results;
		} else {
			return ret;
		}
	}
	
	@Override
	public String getHumanReadableName() {
		return "EPL Query Engine";
	}
}
