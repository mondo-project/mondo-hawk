/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
