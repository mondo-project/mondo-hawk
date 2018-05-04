/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.integration.tests.emf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.backend.tests.BackendTestSuite;
import org.hawk.backend.tests.factories.IGraphDatabaseFactory;
import org.hawk.core.query.IQueryEngine;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.syncValidationListener.SyncValidationListener;
import org.hawk.integration.tests.ModelIndexingTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.Parameterized.Parameters;

/**
 * Integration test case that indexes a fragmented version of the GraBaTs'09
 * set0 model, and runs queries on containment subtrees with the
 * {@link IQueryEngine#PROPERTY_SUBTREECONTEXT} through
 * {@link EOLQueryEngine#getAllOf(String, String, String)} and
 * {@link EOLQueryEngine#getAllOfKind(String)}. Has a small program to do the
 * fragmentation at multiple levels.
 */
public class SubtreeContextTest extends ModelIndexingTest {

	private static class Fragmenter {
		private static final String pathToMetamodel = "resources/metamodels/JDTAST.ecore";
		private static final String pathToOriginal = "resources/models/set0/set0.xmi";
		private static final String pathToFragmented = "resources/models/set0-fragmented/set0.xmi";

		private ResourceSetImpl rs;

		public void run() throws IOException {
			rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

			Resource rEcoreMM = rs.createResource(URI.createFileURI(new File(pathToMetamodel).getAbsolutePath()));
			rEcoreMM.load(null);
			for (EObject rawEPackage : rEcoreMM.getContents()) {
				EPackage epackage = (EPackage) rawEPackage;
				rs.getPackageRegistry().put(epackage.getNsURI(), epackage);
			}
			rs.getResources().remove(rEcoreMM);

			final File fOriginal = new File(pathToOriginal);
			Resource rRoot = rs.createResource(URI.createFileURI(fOriginal.getAbsolutePath()));
			rRoot.load(null);

			final File fFragmented = new File(pathToFragmented);
			if (fFragmented.getParentFile().exists()) {
				FileUtils.deleteDirectory(fFragmented.getParentFile());
			}
			rRoot.setURI(URI.createFileURI(fFragmented.getAbsolutePath()));
			fragment(rRoot);

			for (Resource r : rs.getResources()) {
				r.save(null);
			}
			for (Resource r : rs.getResources()) {
				r.unload();
			}
			rs = null;
		}

		private void fragment(Resource rRoot) {
			File fRoot = new File(rRoot.getURI().toFileString());
			File fRootFolder = fRoot.getParentFile();

			for (TreeIterator<EObject> itContents = rRoot.getAllContents(); itContents.hasNext();) {
				EObject eob = itContents.next();
				if (rRoot.getContents().contains(eob)) {
					// No roots (otherwise, we'd have endless recursion)
					continue;
				}

				final String eClassName = eob.eClass().getName();
				switch (eClassName) {
				case "IJavaProject":
				case "BinaryPackageFragmentRoot":
				case "IPackageFragment":
					EStructuralFeature sf = eob.eClass().getEStructuralFeature("elementName");
					String name = eob.eGet(sf).toString();

					final File fFolder = new File(fRootFolder, String.format("%s_%s", eClassName, name));
					fFolder.mkdirs();
					final File fChild = new File(fFolder, String.format("%s_%s.xmi", eClassName, name));
					final URI uriChild = URI.createFileURI(fChild.getAbsolutePath());
					Resource r = rs.createResource(uriChild);
					itContents.prune();

					r.getContents().add(eob);
					fragment(r);
					break;
				}
			}

		}

	}

	@Rule
	public GraphChangeListenerRule<SyncValidationListener> syncValidation = new GraphChangeListenerRule<>(
			new SyncValidationListener());

	@Rule
	public TemporaryFolder modelFolder = new TemporaryFolder();

	private File folderOriginal, folderFragmented;
	private String originalRepoURI, fragmentedRepoURI;

	@Parameters(name = "{0}")
	public static Iterable<Object[]> params() {
		return BackendTestSuite.caseParams();
	}

	public SubtreeContextTest(IGraphDatabaseFactory dbf) {
		super(dbf, new EMFModelSupportFactory());
	}

	@Before
	public void setUp() throws Throwable {
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/JDTAST.ecore"));

		// Add set0 as usual - should work normally
		folderOriginal = new File("resources/models/set0").getAbsoluteFile();
		requestFolderIndex(folderOriginal);
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());

				return null;
			}
		});

		folderFragmented = new File("resources/models/set0-fragmented").getAbsoluteFile();
		requestFolderIndex(folderFragmented);
		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				assertEquals(0, syncValidation.getListener().getTotalErrors());
				return null;
			}
		});

		originalRepoURI = folderOriginal.toPath().toUri().toString();
		fragmentedRepoURI = folderFragmented.toPath().toUri().toString();
	}

	@Test
	public void allContents() throws Throwable {
		// Sanity check for .allContents (repo-based)
		final int originalSize = (int) eol("return Model.allContents.size;",
				ctx(IQueryEngine.PROPERTY_REPOSITORYCONTEXT, originalRepoURI));
		final int fragmentedSize = (int) eol("return Model.allContents.size;",
				ctx(IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI));
		assertEquals(originalSize, fragmentedSize);
		assertEquals(originalSize * 2, eol("return Model.allContents.size;"));

		// Now doing it with the subtree context (will be replaced by a breadth-first traversal)
		final int subtreeOriginalSize = (int) eol("return Model.allContents.size;", ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, originalRepoURI,
			IQueryEngine.PROPERTY_SUBTREECONTEXT, "/set0.xmi"
		));
		assertEquals(originalSize, subtreeOriginalSize);

		final int subtreeFragmentedSize = (int) eol("return Model.allContents.size;", ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_SUBTREECONTEXT, "/set0.xmi"
		));
		assertEquals(subtreeFragmentedSize, subtreeOriginalSize);

		// Now limit to the contents of the first IJavaProject
		final int originalJavaContents = (int) eol("return 1 + IJavaProject.all.first.closure(e|e.eContents).size;",
				ctx(IQueryEngine.PROPERTY_REPOSITORYCONTEXT, originalRepoURI));
		final int fileBasedJavaContents = (int) eol("return Model.allContents.size;", ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_FILECONTEXT, "/IJavaProject_org.eclipse.jdt.apt.pluggable.core/*"));
		final int subtreeJavaContents = (int) eol("return Model.allContents.size;", ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_SUBTREECONTEXT, "/IJavaProject_org.eclipse.jdt.apt.pluggable.core/IJavaProject_org.eclipse.jdt.apt.pluggable.core.xmi"
		));
		assertEquals(originalJavaContents, fileBasedJavaContents);
		assertEquals(originalJavaContents, subtreeJavaContents);
	}

	@Test
	public void getAllOf() throws Throwable {
		final String eolQuery = "return IType.all.size;";
		final int fileClasses = (int) eol(eolQuery, ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_FILECONTEXT, "/IJavaProject_org.eclipse.jdt.apt.pluggable.core/*"));
		final int subtreeClasses = (int) eol(eolQuery, ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_SUBTREECONTEXT, "/IJavaProject_org.eclipse.jdt.apt.pluggable.core/IJavaProject_org.eclipse.jdt.apt.pluggable.core.xmi"
		));
		assertEquals(fileClasses, subtreeClasses);
	}

	@Test
	public void subtreeTraversalScoping() throws Throwable {
		// None of the external package fragment roots should be visible with traversal scoping on
		final String eolQuery = "return IJavaProject.all.first.externalPackageFragmentRoots.size;";
		final int fileClasses = (int) eol(eolQuery, ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_FILECONTEXT, "/IJavaProject_org.eclipse.jdt.apt.pluggable.core/*",
			IQueryEngine.PROPERTY_ENABLE_TRAVERSAL_SCOPING, "true"));
		assertEquals(0, fileClasses);

		// Same should happen with the subtree context
		final int subtreeClasses = (int) eol(eolQuery, ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_SUBTREECONTEXT, "/IJavaProject_org.eclipse.jdt.apt.pluggable.core/IJavaProject_org.eclipse.jdt.apt.pluggable.core.xmi",
			IQueryEngine.PROPERTY_ENABLE_TRAVERSAL_SCOPING, "true"));
		assertEquals(0, subtreeClasses);
	}

	@Test
	public void getFiles() throws Throwable {
		final int fragmentedFilesCount = (int) eol("return Model.files.size;", ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_FILECONTEXT, "/IJavaProject_org.eclipse.jdt.apt.pluggable.core/*"));
		final int subtreeFilesCount = (int) eol("return Model.files.size;", ctx(
			IQueryEngine.PROPERTY_REPOSITORYCONTEXT, fragmentedRepoURI,
			IQueryEngine.PROPERTY_SUBTREECONTEXT, "/IJavaProject_org.eclipse.jdt.apt.pluggable.core/IJavaProject_org.eclipse.jdt.apt.pluggable.core.xmi"
		));
		assertEquals(fragmentedFilesCount, subtreeFilesCount);
	}

	private static Map<String, Object> ctx(String... opts) {
		Map<String, Object> ctx = new HashMap<>();
		for (int i = 0; i + 1 < opts.length; i += 2) {
			final String key = opts[i], value = opts[i + 1];
			ctx.put(key, value);
		}
		return ctx;
	}

	/**
	 * Creates the fragmented version of set0, to help with testing.
	 */
	public static void main(String[] args) throws IOException {
		new Fragmenter().run();
	}

}
