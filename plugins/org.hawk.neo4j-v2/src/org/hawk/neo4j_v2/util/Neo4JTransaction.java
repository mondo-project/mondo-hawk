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
package org.hawk.neo4j_v2.util;

import org.hawk.core.graph.*;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.neo4j.graphdb.GraphDatabaseService;

public class Neo4JTransaction implements IGraphTransaction {

	org.neo4j.graphdb.Transaction t;
	GraphDatabaseService graph;

	public Neo4JTransaction(IGraphDatabase graph) {
		this.graph = ((Neo4JDatabase) graph).getGraph();
		t = this.graph.beginTx();
	}

	public void success() {
		t.success();
	}

	public void failure() {
		t.failure();
	}

	public void close() {
		t.close();
	}

}
