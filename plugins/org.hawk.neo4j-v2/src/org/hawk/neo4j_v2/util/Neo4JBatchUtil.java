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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4JBatchUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(Neo4JBatchUtil.class);

	public static Label createLabel(final String s) {
		Label a = DynamicLabel.label(s);
		return a;
	}

	public static BatchInserter getGraph(String st) {
		File f = new File(st);

		Map<String, String> config = new HashMap<String, String>();
		long x = Runtime.getRuntime().maxMemory() / 1_000_000 / 60;
		config.put("neostore.nodestore.db.mapped_memory", 3 * x + "M");
		config.put("neostore.relationshipstore.db.mapped_memory", 14 * x + "M");
		config.put("neostore.propertystore.db.mapped_memory", x + "M");
		config.put("neostore.propertystore.db.strings.mapped_memory", 2 * x + "M");
		config.put("neostore.propertystore.db.arrays.mapped_memory", x + "M");
		config.put("neostore.propertystore.db.index.keys.mapped_memory", x + "M");
		config.put("neostore.propertystore.db.index.mapped_memory", x + "M");
		config.put("keep_logical_logs", "false");

		LOGGER.info("Opening: {}\nWITH: {}.{} GB of HEAP and: {}.{}GB of Database VM (embedded in heap)",
			f.getPath(), Runtime.getRuntime().maxMemory() / 1_000_000_000,
			Runtime.getRuntime().maxMemory() % 1_000_000_000,
			getTotalVM(config) / 1000,
			getTotalVM(config) % 1000);

		BatchInserter i = BatchInserters.inserter(f.getPath(), config);
		return i;
	}

	private static int getTotalVM(Map<String, String> config) {

		int totalVM = 0;

		totalVM += Integer
				.parseInt(config.get("neostore.nodestore.db.mapped_memory")
						.substring(
								0,
								config.get(
										"neostore.nodestore.db.mapped_memory")
										.length() - 1));
		totalVM += Integer.parseInt(config.get(
				"neostore.relationshipstore.db.mapped_memory").substring(
				0,
				config.get("neostore.relationshipstore.db.mapped_memory")
						.length() - 1));
		totalVM += Integer.parseInt(config.get(
				"neostore.propertystore.db.mapped_memory")
				.substring(
						0,
						config.get("neostore.propertystore.db.mapped_memory")
								.length() - 1));
		totalVM += Integer.parseInt(config.get(
				"neostore.propertystore.db.strings.mapped_memory").substring(
				0,
				config.get("neostore.propertystore.db.strings.mapped_memory")
						.length() - 1));
		totalVM += Integer.parseInt(config.get(
				"neostore.propertystore.db.arrays.mapped_memory").substring(
				0,
				config.get("neostore.propertystore.db.arrays.mapped_memory")
						.length() - 1));

		totalVM += Integer
				.parseInt(config
						.get("neostore.propertystore.db.index.keys.mapped_memory")
						.substring(
								0,
								config.get(
										"neostore.propertystore.db.index.keys.mapped_memory")
										.length() - 1));

		totalVM += Integer.parseInt(config.get(
				"neostore.propertystore.db.index.mapped_memory").substring(
				0,
				config.get("neostore.propertystore.db.index.mapped_memory")
						.length() - 1));

		return totalVM;
	}

	public static GraphDatabaseService createGraphService(String databaseloc) {

		long x = Runtime.getRuntime().maxMemory() / 1000000 / 60 * 2;

		GraphDatabaseService s = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(databaseloc)
				.setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size,
						5 * x + "M")
				.setConfig(
						GraphDatabaseSettings.relationshipstore_mapped_memory_size,
						15 * x + "M")
				.setConfig(
						GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size,
						2 * x + "M")
				.setConfig(GraphDatabaseSettings.strings_mapped_memory_size,
						2 * x + "M")
				.setConfig(GraphDatabaseSettings.arrays_mapped_memory_size,
						x + "M")
				.setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
				.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true")
				// .setConfig( config )
				.newGraphDatabase();

		registerShutdownHook(s);

		return s;

	}

	private static GraphDatabaseService lastGraph;

	public static void registerShutdownHook(final GraphDatabaseService database) {
		if (lastGraph == null) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						final long l = System.nanoTime();
						lastGraph.shutdown();
						LOGGER.info("SHUTDOWN HOOK INVOKED: (took ~{}s to commit changes)", (System.nanoTime() - l) / 1_000_000_000);
					} catch (Exception e) {
						LOGGER.error("Error during shutdown hook", e);
					}
				}
			});
		}
		lastGraph = database;
	}

}
