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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.util.DefaultConsole;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for the high-level capabilities for starting, stopping, deleting a
 * graph and switching between transactional and batch modes.
 */
public class DatabaseManagementTest {

	private File dbDirectory;

	@Before
	public void setup() throws IOException {
		dbDirectory = new File("testdb");
		deleteRecursively(dbDirectory);
		dbDirectory.mkdir();
	}

	@Test
	public void testStartShutdown() throws Exception {
		OrientDatabase db = new OrientDatabase();
		db.run("memory:testStartShutdown", null, new DefaultConsole());
		final Set<String> expectedIndexes = new HashSet<>(
			Arrays.asList(OrientDatabase.FILE_IDX_NAME, OrientDatabase.METAMODEL_IDX_NAME));
		assertEquals(expectedIndexes, db.getNodeIndexNames());
		assertEquals(Collections.EMPTY_SET, db.getEdgeIndexNames());
		db.shutdown();
	}

	@Test
	public void testStartDelete() throws Exception {
		OrientDatabase db = new OrientDatabase();
		db.run(dbDirectory, new DefaultConsole());
		assertTrue(dbDirectory.exists());
		db.delete();
		assertFalse(dbDirectory.exists());
	}

	@Test
	public void testStartTransaction() throws Exception {
		OrientDatabase db = new OrientDatabase();
		db.run("memory:testStartTransaction", null, new DefaultConsole());

		assertEquals(IGraphDatabase.Mode.TX_MODE, db.currentMode());
		try (IGraphTransaction tx = db.beginTransaction()) {
			assertEquals(IGraphDatabase.Mode.TX_MODE, db.currentMode());
		}
		db.enterBatchMode();
		assertEquals(IGraphDatabase.Mode.NO_TX_MODE, db.currentMode());
		db.exitBatchMode();
		assertEquals(IGraphDatabase.Mode.TX_MODE, db.currentMode());

		db.shutdown();
	}

	static void deleteRecursively(File directory) throws IOException {
		if (!directory.exists()) {
			return;
		}
		Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}
}
