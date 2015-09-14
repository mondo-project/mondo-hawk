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
package runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.util.DefaultConsole;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.hawk.ifc.IFCModelFactory;
import org.hawk.ifc.mm.IFCMetaModelResourceFactory;
import org.hawk.localfolder.LocalFolder;
import org.hawk.neo4j_v2.Neo4JDatabase;

public class IFC_Runtime_example {

	private final static char[] adminpw = "admin".toCharArray();

	private static IQueryEngine q;
	private static IModelIndexer i;

	private static File parent = new File("runtime_data");

	private static File query = new File(
			"C:/Users/kb/Desktop/workspace-luna/org.hawk.epsilon/src/org/hawk/epsilon/query/Grabats_Query_Simplified.eol");

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		i = new ModelIndexerImpl("hawk1", parent, new DefaultConsole());

		// metamodel
		i.addMetaModelResourceFactory(new IFCMetaModelResourceFactory());
		// model
		i.addModelResourceFactory(new IFCModelFactory());

		IGraphDatabase db = (new Neo4JDatabase());
		// create the indexer with relevant database
		db.run(i.getParentFolder(), i.getConsole());
		i.setDB(db,true);

		// set path of vcs
		String vcsloc = "../org.hawk.ifc/samples";

		// add vcs monitors
		IVcsManager vcs = new LocalFolder();
		vcs.run(vcsloc, "un", "pw", i.getConsole());
		i.addVCSManager(vcs,true);

		// metamodel updater
		i.setMetaModelUpdater(new GraphMetaModelUpdater());

		// model updater
		i.addModelUpdater(new GraphModelUpdater());

		// query language
		q = new EOLQueryEngine();
		i.addQueryEngine(q);

		// initialise the server for real-time updates to changes
		i.setAdminPassword(adminpw);
		i.init();

		// add console interaction if needed
		Thread t = consoleInteraction(i);
		t.start();

		// terminate hawk
		// h.shutdown();
	}

	private static Thread consoleInteraction(final IModelIndexer i2) {
		return new Thread() {
			@Override
			public void run() {
				while (true) {
					BufferedReader r = new BufferedReader(
							new InputStreamReader(System.in));
					try {
						String s = r.readLine();
						if (s.equalsIgnoreCase("quit")
								|| s.equalsIgnoreCase("exit")
								|| s.equalsIgnoreCase("e")) {
							i2.shutdown(ShutdownRequestType.ONLY_LOCAL);
							System.exit(0);
						}
						if (s.equalsIgnoreCase("query")
								|| s.equalsIgnoreCase("q")) {
							q.contextlessQuery(i.getGraph(), query);
						}
						if (s.equalsIgnoreCase("nuke")) {
							i2.delete();
							System.exit(0);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
		};
	}
}
