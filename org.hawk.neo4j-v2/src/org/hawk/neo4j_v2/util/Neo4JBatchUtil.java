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

public class Neo4JBatchUtil {

	public static Label createLabel(final String s) {

		Label a = DynamicLabel.label(s);

		return a;
	}

	public static BatchInserter getGraph(String st) {

		File f = new File(st);
		
		Map<String, String> config = new HashMap<String, String>();
		long x = Runtime.getRuntime().maxMemory() / 1000000 / 60;
		config.put("neostore.nodestore.db.mapped_memory", 3 * x + "M");
		config.put("neostore.relationshipstore.db.mapped_memory", 14 * x + "M");
		config.put("neostore.propertystore.db.mapped_memory", x + "M");
		config.put("neostore.propertystore.db.strings.mapped_memory", 2 * x
				+ "M");
		config.put("neostore.propertystore.db.arrays.mapped_memory", x + "M");
		config.put("neostore.propertystore.db.index.keys.mapped_memory", x
				+ "M");
		config.put("neostore.propertystore.db.index.mapped_memory", x + "M");
		config.put("keep_logical_logs", "false");

		System.out.println("Opening: " + f.getPath() + "\nWITH: "
				+ Runtime.getRuntime().maxMemory() / 1000000000 + "."
				+ Runtime.getRuntime().maxMemory() % 1000000000
				+ " GB of HEAP and: " + getTotalVM(config) / 1000 + "."
				+ getTotalVM(config) % 1000
				+ " GB of Database VM (embedded in heap)");

		BatchInserter i = BatchInserters.inserter(f.getPath(), config);

		registerShutdownHook(i);

		return i;

	}

	public static BatchInserter getGraph(String st, Map<String, String> config) {

		File f = new File(st);
		
		System.out.println("Opening: " + f.getPath() + "\nWITH: "
				+ Runtime.getRuntime().maxMemory() / 1000000000 + "."
				+ Runtime.getRuntime().maxMemory() % 1000000000
				+ " GB of HEAP and: " + getTotalVM(config) / 1000 + "."
				+ getTotalVM(config) % 1000
				+ " GB of Database VM (embedded in heap)");

		BatchInserter i = BatchInserters.inserter(f.getPath(), config);

		registerShutdownHook(i);

		return i;

	}

	// extra check to make sure database is shut down if the jvm is interrupted
	/**
	 * 
	 * @param graph2
	 */
	private static void registerShutdownHook(final BatchInserter graph2) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					long l = System.nanoTime();
					graph2.shutdown();
					System.out.println("SHUTDOWN HOOK INVOKED: (took ~"
							+ (System.nanoTime() - l) / 1000000000
							+ "sec to commit changes)");
				} catch (Exception e) {
				}
			}
		});
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
				// .setConfig( config )
				.newGraphDatabase();

		registerShutdownHook(s);

		return s;

	}

	public static void registerShutdownHook(final GraphDatabaseService database) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					long l = System.nanoTime();
					database.shutdown();
					System.out.println("SHUTDOWN HOOK INVOKED: (took ~"
							+ (System.nanoTime() - l) / 1000000000
							+ "sec to commit changes)");
				} catch (Exception e) {
				}
			}
		});
	}

}
