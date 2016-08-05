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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hawk.bpmn.metamodel.BPMNMetaModelResourceFactory;
import org.hawk.bpmn.model.BPMNModelResourceFactory;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.security.FileBasedCredentialsStore;
import org.hawk.core.util.DefaultConsole;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.hawk.graph.sampleListener.ExampleListener;
import org.hawk.http.HTTPManager;
import org.hawk.localfolder.LocalFolder;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResourceFactory;
import org.hawk.modelio.exml.model.ModelioModelResourceFactory;
import org.hawk.neo4j_v2.Neo4JDatabase;

public class Runtime_example {

	private static Git git;
	private static LinkedHashMap<String, RevCommit> linkedHashMap = new LinkedHashMap<>();

	private static IQueryEngine q;
	private static IModelIndexer hawk;

	private static final String queryLangID = "org.hawk.epsilon.emc.EOLQueryEngine";

	// these queries assume this project is run in the same folder as the rest
	// of hawk (for example cloning the entire hawk git repo)
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

		File parent = new File("runtime_data"
		// "runtime_data_OrientDB"
		);

		IGraphDatabase db = (new Neo4JDatabase()
		// new OrientDatabase()
		);

		// create an empty hawk model indexer
		char[] init = new char[5];
		init[0] = 'a';
		init[1] = 'd';
		init[2] = 'm';
		init[3] = 'i';
		init[4] = 'n';
		final FileBasedCredentialsStore credStore = new FileBasedCredentialsStore(
				new File("credentials.xml"), init);
		hawk = new ModelIndexerImpl("hawk1", parent, credStore,
				new DefaultConsole());

		// add a metamodel factory
		hawk.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());
		// hawk.addMetaModelResourceFactory(new BPMNMetaModelResourceFactory());
		// hawk.addMetaModelResourceFactory(new
		// ModelioMetaModelResourceFactory());

		// add a model factory
		hawk.addModelResourceFactory(new EMFModelResourceFactory());
		// hawk.addModelResourceFactory(new BPMNModelResourceFactory());
		// hawk.addModelResourceFactory(new ModelioModelResourceFactory());

		// create the model index with relevant database
		db.run(hawk.getParentFolder(), hawk.getConsole());
		hawk.setDB(db, true);

		// add a vcs monitor (we can add as many as we want)
		// IVcsManager vcs = new SvnManager();
		// IVcsManager vcs = new HTTPManager();
		IVcsManager vcs = new LocalFolder();

		String pw;

		// do not ask for a password if the vcs monitor is a LocalFolder
		// instance otherwise ask for a password
		if (vcs instanceof LocalFolder)
			pw = "noneed";
		else
			pw = password();

		// set path of vcs to monitor in args[0] for this example
		if (pw != null) {
			vcs.setCredentials("kb634", pw, credStore);
			vcs.init(args[0], hawk);
			vcs.run();
		} else
			System.exit(1);
		pw = null;

		hawk.addVCSManager(vcs, true);

		// add the metamodel updater
		hawk.setMetaModelUpdater(new GraphMetaModelUpdater());

		// add a (default) model updater
		hawk.addModelUpdater(new GraphModelUpdater());

		// add a query language
		q = new EOLQueryEngine();
		hawk.addQueryEngine(q);

		// only needed for debugging - both can be removed for production
		// scenarios
		hawk.addGraphChangeListener(new SyncChangeListener(git, linkedHashMap,
				hawk));
		hawk.addGraphChangeListener(new ExampleListener());

		// register all metamodels found in the hawk.emf metamodel examples
		// folder (assumed to be found alongside this project)
		// otherwise register any metamodel by providing it here
		for (File f : new File("../org.hawk.emf/src/org/hawk/emf/metamodel/examples/single")
				.listFiles())
			if (f.getPath().endsWith(".ecore"))
				hawk.registerMetamodels(f);

		// Initialise the server for real-time updates to changes -- this has to
		// be done after initialising all the relevant plugins you want online.
		// These parameters can be set to 0/0, and then only manual syncs sent
		// through #requestImmediateSync will be honored.
		// hawk.init(1000, 512 * 1000);
		hawk.init(0, 0);

		// add console interaction if needed
		Thread t = consoleInteraction(hawk);
		t.start();

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

				while (true) {
					BufferedReader r = new BufferedReader(
							new InputStreamReader(System.in));

					try {
						String s = r.readLine();
						if (s.equalsIgnoreCase("quit")
								|| s.equalsIgnoreCase("exit")
								|| s.equalsIgnoreCase("e")) {
							// i2.shutdown(ShutdownRequestType.ONLY_LOCAL);
							i2.delete();
							System.exit(0);
						} else if (s.equalsIgnoreCase("qq")
								|| s.equalsIgnoreCase("exitpersist")
								|| s.equalsIgnoreCase("ee")) {
							// i2.shutdown(ShutdownRequestType.ONLY_LOCAL);
							i2.shutdown(ShutdownRequestType.ONLY_LOCAL);
							System.exit(0);
						} else if (s.equalsIgnoreCase("adi")) {
							addDerivedandIndexedAttributes();
						} else if (s.equalsIgnoreCase("tq")) {
							q.query(i2, testquery, null);
						} else if (s.equalsIgnoreCase("query")
								|| s.equalsIgnoreCase("q")) {
							q.query(i2, query, null);
							q.query(i2, query2, null);
							q.query(i2, query3, null);
						} else if (s.equalsIgnoreCase("q1")) {
							q.query(i2, query, null);
						} else if (s.equalsIgnoreCase("q2")) {
							q.query(i2, query2, null);
						} else if (s.equalsIgnoreCase("q3")) {
							q.query(i2, query3, null);
						} else if (s.equalsIgnoreCase("cq")) {
							Map<String, Object> map = new HashMap<String, Object>();
							map.put(EOLQueryEngine.PROPERTY_FILECONTEXT, "*");
							q.query(i2,
									"TypeDeclaration.all.size().println();",
									map);
						} else if (s.equalsIgnoreCase("cqs")) {
							Map<String, Object> map = new HashMap<String, Object>();
							map.put(EOLQueryEngine.PROPERTY_FILECONTEXT, "*");
							q.query(i2, query, map);
							q.query(i2, query2, map);
							q.query(i2, query3, map);

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

						} else if (s.equalsIgnoreCase("r")) {
							hawk.requestImmediateSync();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
		};
	}

	public static IModelIndexer run(Git gitt,
			LinkedHashMap<String, RevCommit> linkedHashMapp, String[] vcsloc)
			throws Exception {
		git = gitt;
		linkedHashMap = linkedHashMapp;
		main(vcsloc);
		return hawk;
	}
}
