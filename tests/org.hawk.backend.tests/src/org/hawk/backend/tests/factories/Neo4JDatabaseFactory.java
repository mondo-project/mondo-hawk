/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.backend.tests.factories;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.neo4j_v2.Neo4JDatabase;

public final class Neo4JDatabaseFactory implements IGraphDatabaseFactory {
	@Override
	public IGraphDatabase create() {
		return new Neo4JDatabase();
	}

	@Override
	public String toString() {
		return "Neo4j";
	}
}