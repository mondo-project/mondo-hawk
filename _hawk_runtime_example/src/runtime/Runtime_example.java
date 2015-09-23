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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.hawk.bpmn.metamodel.BPMNMetaModelResourceFactory;
import org.hawk.bpmn.model.BPMNModelResourceFactory;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.util.DefaultConsole;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.hawk.localfolder.LocalFolder;
import org.hawk.neo4j_v2.Neo4JDatabase;

public class Runtime_example {

	private static IQueryEngine q;
	private static IModelIndexer hawk;

	private static File parent = new File("runtime_data");

	private static final String queryLangID = "org.hawk.epsilon.emc.EOLQueryEngine";

	private static File testquery = new File(
			"../org.hawk.epsilon/src/org/hawk/epsilon/query/Test_Query.eol");
	private static File query = new File(
			"../org.hawk.epsilon/src/org/hawk/epsilon/query/Grabats_Query_Simplified.eol");
	private static File query2 = new File(
			"../org.hawk.epsilon/src/org/hawk/epsilon/query/Grabats_Query_Derived.eol");
	private static File query3 = new File(
			"../org.hawk.epsilon/src/org/hawk/epsilon/query/Grabats_Query_Derived_INDEXED.eol");

	static String pw = null;
	private static IGraphChangeListener listener;

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
		hawk = new ModelIndexerImpl("hawk1", parent, new DefaultConsole());

		// add a metamodel factory
		// hawk.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());
		hawk.addMetaModelResourceFactory(new BPMNMetaModelResourceFactory());

		// add a model factory
		// hawk.addModelResourceFactory(new EMFModelResourceFactory());
		hawk.addModelResourceFactory(new BPMNModelResourceFactory());

		IGraphDatabase db = (new Neo4JDatabase());
		// create the model index with relevant database
		db.run(hawk.getParentFolder(), hawk.getConsole());
		hawk.setDB(db, true);

		// set path of vcs to monitor

		// String vcsloc =
		// "C:/Users/kb/Desktop/workspace_runtime/EOL_tests/hawk_model_tests/0";

		// String vcsloc =
		// "../org.hawk.emf/src/org/hawk/emf/model/examples/fragmented";

		//

		// String vcsloc =
		// "../org.hawk.emf/src/org/hawk/emf/model/examples/single/0";

		String vcsloc = "..\\_hawk_evaluation_simulation\\model\\bpmn-miwg_bpmn-miwg-test-suite";

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
			vcs.run(vcsloc, "kb634", pw, hawk.getConsole());
		} else
			System.exit(1);
		pw = null;

		hawk.addVCSManager(vcs, true);

		// add a metamodel updater
		hawk.setMetaModelUpdater(new GraphMetaModelUpdater());

		// add a (default) model updater
		hawk.addModelUpdater(new GraphModelUpdater());

		// add a query language
		q = new EOLQueryEngine();
		hawk.addQueryEngine(q);

		hawk.addGraphChangeListener(listener);

		// initialise the server for real-time updates to changes -- this has to
		// be done after initialising all the relevant plugins you want online

		char[] init = new char[5];
		init[0] = 'a';
		init[1] = 'd';
		init[2] = 'm';
		init[3] = 'i';
		init[4] = 'n';

		hawk.setAdminPassword(init);
		hawk.init();

		// add console interaction if needed
		Thread t = consoleInteraction(hawk);
		t.start();

		// i.removeMetamodel(metamodel);

		// hawk.addVCSManager(vcs, true);
		//
		// addDerivedandIndexedAttributes();
		//

		// terminate hawk
		// h.shutdown();

	}

	private static void addDerivedandIndexedAttributes() throws Exception {

		hawk.addDerivedAttribute("org.amma.dsl.jdt.dom", "MethodDeclaration",
				"isPublic", "Boolean", false, true, true, queryLangID,
				"self.modifiers.exists(mod:Modifier|mod.public==true)");

		hawk.addDerivedAttribute("org.amma.dsl.jdt.dom", "MethodDeclaration",
				"isStatic", "Boolean", false, true, true, queryLangID,
				"self.modifiers.exists(mod:Modifier|mod.static==true)");

		hawk.addDerivedAttribute(
				"org.amma.dsl.jdt.dom",
				"MethodDeclaration",
				"isSameReturnType",
				"Boolean",
				false,
				true,
				true,
				queryLangID,
				"self.returnType.isTypeOf(SimpleType) and self.revRefNav_bodyDeclarations.isTypeOf(TypeDeclaration) and self.returnType.name.fullyQualifiedName == self.revRefNav_bodyDeclarations.name.fullyQualifiedName");

		// i.addIndexedAttribute("org.amma.dsl.jdt.dom", "Modifier", "static");

	}

	private static Thread consoleInteraction(final IModelIndexer i2) {
		return new Thread() {
			@Override
			public void run() {
				final IGraphDatabase graph = i2.getGraph();

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
						} else if (s.equalsIgnoreCase("adi")) {
							addDerivedandIndexedAttributes();
						} else if (s.equalsIgnoreCase("qq")) {
							q.contextlessQuery(graph, testquery);
						} else if (s.equalsIgnoreCase("query")
								|| s.equalsIgnoreCase("q")) {
							q.contextlessQuery(graph, query);
							q.contextlessQuery(graph, query2);
							q.contextlessQuery(graph, query3);
						} else if (s.equalsIgnoreCase("q1")) {
							q.contextlessQuery(graph, query);
						} else if (s.equalsIgnoreCase("q2")) {
							q.contextlessQuery(graph, query2);
						} else if (s.equalsIgnoreCase("q3")) {
							q.contextlessQuery(graph, query3);
						} else if (s.equalsIgnoreCase("cq")) {
							Map<String, String> map = new HashMap<String, String>();
							map.put(EOLQueryEngine.PROPERTY_FILECONTEXT, "*");
							q.contextfullQuery(graph,
									"TypeDeclaration.all.size().println();",
									map);
						} else if (s.equalsIgnoreCase("cqs")) {
							Map<String, String> map = new HashMap<String, String>();
							map.put(EOLQueryEngine.PROPERTY_FILECONTEXT, "*");
							q.contextfullQuery(graph, query, map);
							q.contextfullQuery(graph, query2, map);
							q.contextfullQuery(graph, query3, map);

						} else if (s.equalsIgnoreCase("tf")) {

							hawk.addDerivedAttribute("org.amma.dsl.jdt.dom",
									"MethodDeclaration", "isSameReturnType",
									"Boolean", false, true, true, queryLangID,
									// always false
									"self.returnType.isTypeOf(MethodDeclaration)");

						} else if (s.equalsIgnoreCase("tt")) {

							hawk.addDerivedAttribute(
									"org.amma.dsl.jdt.dom",
									"MethodDeclaration",
									"isSameReturnType",
									"Boolean",
									false,
									true,
									true,
									queryLangID,
									"self.returnType.isTypeOf(SimpleType) and self.revRefNav_bodyDeclarations.isTypeOf(TypeDeclaration) and self.returnType.name.fullyQualifiedName == self.revRefNav_bodyDeclarations.name.fullyQualifiedName");

						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
		};
	}

	public static IModelIndexer run(IGraphChangeListener l) throws Exception {
		listener = l;
		main(null);
		return hawk;
	}
}
