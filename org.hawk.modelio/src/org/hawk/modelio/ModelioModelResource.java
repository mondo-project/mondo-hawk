/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	   Seyyed Shah - initial API and implementation
 * 	   Antonio Garcia Dominguez - rework for using .zip archives of projects 
 *     Konstantinos Barmpis - provision of original EMF factory this one (indirectly) extends
 *     
 ******************************************************************************/
package org.hawk.modelio;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.modelio.gproject.data.project.DefinitionScope;
import org.modelio.gproject.data.project.FragmentType;
import org.modelio.gproject.data.project.ProjectDescriptor;
import org.modelio.gproject.data.project.ProjectDescriptorReader;
import org.modelio.gproject.fragment.IProjectFragment;
import org.modelio.gproject.fragment.exml.ExmlFragment;
import org.modelio.gproject.gproject.GProject;
import org.modelio.gproject.gproject.GProjectFactory;
import org.modelio.gproject.model.MModelServices;
import org.modelio.gproject.module.catalog.FileModuleStore;
import org.modelio.metamodel.data.MetamodelLoader;
import org.modelio.metamodel.mda.Project;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vbasic.auth.NoneAuthData;
import org.modelio.vbasic.progress.NullProgress;
import org.modelio.vcore.smkernel.mapi.MObject;
import org.modelio.xmi.generation.ExportServices;
import org.modelio.xmi.generation.GenerationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioModelResource implements IHawkModelResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioModelResource.class);

	private final File modelioZipFile;
	private final Path modulesPath;
	private final String factoryClassName;
	private Model model;

	@Override
	public void unload() {
		//TODO check for memory leak
		model = null;
	}

	public ModelioModelResource(File f, Path modulesPath, String factoryClassName) {
		if (modulesPath == null) {
			throw new NullPointerException("The path to the modules directory has not been set");
		}

		this.modulesPath = modulesPath;
		this.modelioZipFile = f;
		this.factoryClassName = factoryClassName;
	}

	@Override
	public Iterator<IHawkObject> getAllContents() {
		return getAllContentsSet().iterator();
	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {
		try {
			final Model model = getModel();
			Set<IHawkObject> allElements = new HashSet<IHawkObject>();
			allElements.add(new ModelioObject(model));

			EList<Element> all = model.allOwnedElements();
			for (Element e : all) {
				allElements.add(new ModelioObject(e));
			}
			return allElements;
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptySet();
		}
	}

	private Model getModel() throws IOException {
		if (model != null) {
			return model;
		}

		try (final TemporaryUnzippedFile unzipped = new TemporaryUnzippedFile(modelioZipFile, "mondo-modelio")) {
			final Path tmpDirPath = unzipped.getTemporaryDirectory();
			final Path descriptorPath = tmpDirPath.resolve("project.conf");
			try (final ModelioProject p = new ModelioProject(descriptorPath, modulesPath)) {
				final Package mainPackage = p.getMainPackage();
				model = p.exportIntoXMI(mainPackage);
				return model;
			}
		}
	}

	private static class ModelioProject implements Closeable {

		private final ProjectDescriptor descriptor;
		private final GProject project;

		public ModelioProject(Path resolve, Path modulesPath) throws IOException {
			MetamodelLoader.Load();
			this.descriptor = new ProjectDescriptorReader().read(resolve, DefinitionScope.LOCAL);
			this.project = (GProject) GProjectFactory.openProject(this.descriptor, new NoneAuthData(),
					new FileModuleStore(modulesPath), null, new NullProgress());
		}

		public Model exportIntoXMI(Package mainPackage) {
			GenerationProperties genProp = GenerationProperties.getInstance();
			genProp.initialize(new MModelServices(project));
			genProp.setTimeDisplayerActivated(false);
			genProp.setSelectedPackage(mainPackage);

			ExportServices exportService = new ExportServices();
			return exportService.createEcoreModel(mainPackage, null);
		}

		public Package getMainPackage() {
			Iterator<MObject> iterator = getExmlFragment().doGetRoots().iterator();
			Project o = (Project) iterator.next();
			return o.getModel();
		}

		public ExmlFragment getExmlFragment() {
			return (ExmlFragment)getFragment(FragmentType.EXML);
		}

		public IProjectFragment getFragment(FragmentType type) {
			for (IProjectFragment fragments : project.getOwnFragments()) {
				if (fragments.getType().equals(type)) {
					return fragments;
				}
			}
			return null;
		}

		@Override
		public void close() throws IOException {
			project.close();
		}
		
	}

	private static class TemporaryUnzippedFile implements Closeable {
		private Path tmpDir;

		public TemporaryUnzippedFile(File f, String tempPrefix) throws IOException {
			this.tmpDir = Files.createTempDirectory(tempPrefix);
			unpack(f);
		}

		public Path getTemporaryDirectory() {
			return tmpDir;
		}

		@Override
		public void close() throws IOException {
			if (tmpDir != null) {
				recursiveDelete();
			}
		}

		private void unpack(File zipFile) throws IOException, ZipException {
			try (ZipFile openedZip = new ZipFile(zipFile)) {
				final Enumeration<? extends ZipEntry> entries = openedZip.entries();
				while (entries.hasMoreElements()) {
					final ZipEntry entry = entries.nextElement();
					try (final InputStream entryIS = openedZip.getInputStream(entry)) {
						final Path target = tmpDir.resolve(entry.getName());
						if (entry.isDirectory()) {
							Files.createDirectories(target);
						} else {
							Files.createDirectories(target.getParent());
							Files.copy(entryIS, target);
						}
					}
				}
			}
		}

		private void recursiveDelete() {
			// based on http://stackoverflow.com/questions/779519/delete-files-recursively-in-java/8685959#8685959
			try {
				Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						if (exc == null) {
							Files.delete(dir);
							return FileVisitResult.CONTINUE;
						} else {
							throw exc;
						}
					}
				});
			} catch (IOException e) {
				LOGGER.error("Could not delete directory " + tmpDir, e);
			}
		}
	}

	@Override
	public String getType() {
		return factoryClassName;
	}

	@Override
	public int getSignature(IHawkObject o) {
		return o.hashCode();
	}
}
