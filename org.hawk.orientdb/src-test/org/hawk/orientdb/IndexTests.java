/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.orientdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.DefaultConsole;
import org.hawk.orientdb.util.FluidMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for populating and querying indices.
 */
public class IndexTests {

	private OrientDatabase db;

	@Before
	public void setup() {
		db = new OrientDatabase();
		db.run(new File("testdb"), new DefaultConsole());
	}

	@After
	public void teardown() throws Exception {
		db.delete();
	}

	@Test
	public void query() {
		final String mmBarURI = "http://foo/bar";
		final String mmFileURI = "file://a/b/c.d";
		final FluidMap mmBarNodeProps = FluidMap.create().add(IModelIndexer.IDENTIFIER_PROPERTY, mmBarURI);
		final FluidMap mmFileNodeProps = FluidMap.create().add(IModelIndexer.IDENTIFIER_PROPERTY, mmFileURI);

		try (IGraphTransaction tx = db.beginTransaction()) {
			IGraphNode mmNode = db.createNode(mmBarNodeProps, "metamodel");
			db.getMetamodelIndex().add(mmNode, FluidMap.create().add("id", mmBarURI));

			IGraphNode mmFileNode = db.createNode(mmFileNodeProps, "metamodel");
			db.getMetamodelIndex().add(mmFileNode, FluidMap.create().add("id", mmFileURI));

			tx.success();
		}

		// Query with partial wildcard
		final IGraphIterable<IGraphNode> iter = db.getMetamodelIndex().query("id", "http://*");
		assertSame(db, iter.getSingle().getGraph());
		assertEquals(1, iter.size());

		// Query with full value
		final IGraphIterable<IGraphNode> iter3 = db.getMetamodelIndex().query("id", mmBarURI);
		assertSame(db, iter3.getSingle().getGraph());
		assertEquals(1, iter3.size());
		assertEquals(mmBarURI, iter3.getSingle().getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		// Query with wildcard on one field
		final IGraphIterable<IGraphNode> iter4 = db.getMetamodelIndex().query("id", "*");
		assertSame(db, iter4.getSingle().getGraph());
		assertEquals(2, iter4.size());

		// Retrieve with full value
		final IGraphIterable<IGraphNode> iter2 = db.getMetamodelIndex().get("id", mmFileURI);
		assertEquals(1, iter2.size());
		assertEquals(mmFileURI, iter2.getSingle().getProperty(IModelIndexer.IDENTIFIER_PROPERTY));

		// Use */* (all fields, all values): not available with OrientDB?
		assertEquals(new HashSet<>(Arrays.asList(mmBarURI, mmFileURI)), db.getKnownMMUris());
	}
}
