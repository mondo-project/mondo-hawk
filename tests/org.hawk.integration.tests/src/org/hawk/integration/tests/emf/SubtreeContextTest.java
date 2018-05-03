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

import java.io.File;
import java.io.IOException;

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
import org.junit.Rule;
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
			rs.getResourceFactoryRegistry()
				.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

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

			for (TreeIterator<EObject> itContents = rRoot.getAllContents(); itContents.hasNext(); ) {
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
	public GraphChangeListenerRule<SyncValidationListener> syncValidation
		= new GraphChangeListenerRule<>(new SyncValidationListener());

	@Rule
	public TemporaryFolder modelFolder = new TemporaryFolder();

	@Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
    	return BackendTestSuite.caseParams();
    }

	public SubtreeContextTest(IGraphDatabaseFactory dbf) {
		super(dbf, new EMFModelSupportFactory());
	}

	public static void main(String[] args) throws IOException {
		new Fragmenter().run();
	}

}
