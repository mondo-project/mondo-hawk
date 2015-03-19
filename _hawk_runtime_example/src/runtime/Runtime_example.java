/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.util.DefaultConsole;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.updater.GraphMetaModelUpdater;
import org.hawk.graph.updater.GraphModelUpdater;
import org.hawk.localfolder.LocalFolder;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.hawk.svn.SvnManager;

public class Runtime_example {

	private static IQueryEngine q;
	private static IModelIndexer i;

	private static File parent = new File("runtime_data");

	private static final String queryLangID = "org.hawk.epsilon.emc.GraphEpsilonModel";

	private static File testquery = new File(
			"../org.hawk.epsilon/src/org/hawk/epsilon/query/Test_Query.eol");
	private static File query = new File(
			"../org.hawk.epsilon/src/org/hawk/epsilon/query/Grabats_Query_Simplified.eol");
	private static File query2 = new File(
			"../org.hawk.epsilon/src/org/hawk/epsilon/query/Grabats_Query_Derived.eol");
	private static File query3 = new File(
			"../org.hawk.epsilon/src/org/hawk/epsilon/query/Grabats_Query_Derived_INDEXED.eol");

	static String pw = null;

	public static String password() {
		final JFrame parent = new JFrame();
		return JOptionPane.showInputDialog(parent,
				"enter your VCS password please", "thisisapassword");
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// create an empty hawk model indexer
		i = new ModelIndexerImpl("hawk1", parent, new DefaultConsole());

		// add a metamodel factory
		i.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());

		// add a model factory
		i.addModelResourceFactory(new EMFModelResourceFactory());

		IGraphDatabase db = (new Neo4JDatabase());
		// create the model index with relevant database
		db.run("epsilon_test_db", i.getParentFolder(), i.getConsole());
		i.setDB(db);

		// set path of vcs to monitor

		// String vcsloc =
		// "C:/Users/kb/Desktop/workspace_runtime/EOL_tests/hawk_model_tests/0";

		// String vcsloc =
		// "../org.hawk.emf/src/org/hawk/emf/model/examples/fragmented";

		//

		// String vcsloc =
		// "../org.hawk.emf/src/org/hawk/emf/model/examples/single/0";

		String vcsloc = "../_hawk_runtime_example/runtime_data/model/0";

		//

		// NB: to use svn with our local CS repository, you need to put
		// -Dsvnkit.http.methods="NTLM,Basic,Digest,Negotiate"
		// as runtime parameters so the authentication does not fail (and also
		// your own user name and password!)
		//
		// String vcsloc =
		// "https://cssvn.york.ac.uk/repos/sosym/kostas/Hawk/org.hawk.emf/src/org/hawk/emf/model/examples/single/0";

		// add a vcs monitor
		// IVcsManager vcs = new SvnManager();
		IVcsManager vcs = new LocalFolder();

		String pw;

		// do not ask for a password if the vcs monitor is a LocalFolder
		// instance otherwise ask for a password
		if (vcs instanceof LocalFolder)
			pw = "noneed";
		else
			pw = password();

		if (pw != null) {
			vcs.run(vcsloc, "kb634", pw, i.getConsole());
		} else
			System.exit(1);
		pw = null;
		i.addVCSManager(vcs);

		// add a metamodel updater
		i.setMetaModelUpdater(new GraphMetaModelUpdater());

		// add one or more metamodel files
		File metamodel = new File(
				"../org.hawk.emf/src/org/hawk/emf/metamodel/examples/single/JDTAST.ecore");
		// register them
		i.registerMetamodel(metamodel);
		// i.removeMetamodel(metamodel);

		//
		addDerivedandIndexedAttributes();
		//

		// add a (default) model updater
		i.addModelUpdater(new GraphModelUpdater());

		// add a query language
		q = new EOLQueryEngine();
		i.addQueryEngine(q);

		// initialise the server for real-time updates to changes -- this has to
		// be done after initialising all the relevant plugins you want online
		i.init();

		// add console interaction if needed
		Thread t = consoleInteraction(i);
		t.start();

		// terminate hawk
		// h.shutdown();

	}

	private static void addDerivedandIndexedAttributes() throws Exception {

		i.addDerivedAttribute("org.amma.dsl.jdt.dom", "MethodDeclaration",
				"isPublic", "Boolean", false, true, true,
				"org.hawk.epsilon.emc.GraphEpsilonModel",
				"self.modifiers.exists(mod:Modifier|mod.public==true)");

		i.addDerivedAttribute("org.amma.dsl.jdt.dom", "MethodDeclaration",
				"isStatic", "Boolean", false, true, true,
				"org.hawk.epsilon.emc.GraphEpsilonModel",
				"self.modifiers.exists(mod:Modifier|mod.static==true)");

		i.addDerivedAttribute(
				"org.amma.dsl.jdt.dom",
				"MethodDeclaration",
				"isSameReturnType",
				"Boolean",
				false,
				true,
				true,
				"org.hawk.epsilon.emc.GraphEpsilonModel",
				"self.returnType.isTypeOf(SimpleType) and self.revRefNav_bodyDeclarations.isTypeOf(TypeDeclaration) and self.returnType.name.fullyQualifiedName == self.revRefNav_bodyDeclarations.name.fullyQualifiedName");

		// i.addIndexedAttribute("org.amma.dsl.jdt.dom", "Modifier", "static");

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
							i2.shutdown(null, true);
							System.exit(0);
						} else if (s.equalsIgnoreCase("adi")) {
							addDerivedandIndexedAttributes();
						} else if (s.equalsIgnoreCase("qq")) {
							i2.query(testquery, queryLangID);
						} else if (s.equalsIgnoreCase("query")
								|| s.equalsIgnoreCase("q")) {
							i2.query(query, queryLangID);
							i2.query(query2, queryLangID);
							i2.query(query3, queryLangID);
							// q.contextlessQuery(i.getGraph(), query);
							// q.contextlessQuery(i.getGraph(), query2);
							// q.contextlessQuery(i.getGraph(), query3);
						} else if (s.equalsIgnoreCase("q1")) {
							i2.query(query, queryLangID);
						} else if (s.equalsIgnoreCase("q2")) {
							i2.query(query2, queryLangID);
						} else if (s.equalsIgnoreCase("q3")) {
							// q.contextlessQuery(i.getGraph(), query3);
							i2.query(query3, queryLangID);
						} else if (s.equalsIgnoreCase("cq")) {
							Map<String, String> map = new HashMap<String, String>();
							map.put(EOLQueryEngine.PROPERTY_FILECONTEXT, "*");
							q.contextfullQuery(i.getGraph(),
									"TypeDeclaration.all.size().println();",
									map);
						} else if (s.equalsIgnoreCase("cqs")) {
							Map<String, String> map = new HashMap<String, String>();
							map.put(EOLQueryEngine.PROPERTY_FILECONTEXT, "*");
							q.contextfullQuery(i.getGraph(), query, map);
							q.contextfullQuery(i.getGraph(), query2, map);
							q.contextfullQuery(i.getGraph(), query3, map);

						} else if (s.equalsIgnoreCase("tf")) {

							i.addDerivedAttribute("org.amma.dsl.jdt.dom",
									"MethodDeclaration", "isSameReturnType",
									"Boolean", false, true, true,
									"org.hawk.epsilon.emc.GraphEpsilonModel",
									// always false
									"self.returnType.isTypeOf(MethodDeclaration)");

						} else if (s.equalsIgnoreCase("tt")) {

							i.addDerivedAttribute(
									"org.amma.dsl.jdt.dom",
									"MethodDeclaration",
									"isSameReturnType",
									"Boolean",
									false,
									true,
									true,
									"org.hawk.epsilon.emc.GraphEpsilonModel",
									"self.returnType.isTypeOf(SimpleType) and self.revRefNav_bodyDeclarations.isTypeOf(TypeDeclaration) and self.returnType.name.fullyQualifiedName == self.revRefNav_bodyDeclarations.name.fullyQualifiedName");

						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
		};
	}
}
