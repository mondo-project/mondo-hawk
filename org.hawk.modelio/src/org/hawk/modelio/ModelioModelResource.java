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
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.Model;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.modelio.api.modelio.Modelio;
import org.modelio.app.core.IModelioEventService;
import org.modelio.app.core.events.ModelioEventService;
import org.modelio.app.project.core.services.IProjectService;
import org.modelio.gproject.data.project.DefinitionScope;
import org.modelio.gproject.data.project.ProjectDescriptor;
import org.modelio.gproject.data.project.ProjectDescriptorReader;
import org.modelio.gproject.fragment.IProjectFragment;
import org.modelio.gproject.gproject.GProject;
import org.modelio.gproject.model.MModelServices;
import org.modelio.metamodel.data.MetamodelLoader;
import org.modelio.metamodel.mda.Project;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vbasic.auth.NoneAuthData;
import org.modelio.vcore.smkernel.mapi.MObject;
import org.modelio.xmi.generation.ExportServices;
import org.modelio.xmi.gui.report.ReportModel;
import org.modelio.xmi.util.GenerationProperties;
import org.modelio.xmi.util.XMILogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioModelResource implements IHawkModelResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioModelResource.class);

	private final File modelioZipFile;
	private final String factoryClassName;
	private List<Model> models;

	@Override
	public void unload() {
		// TODO check for memory leak
		models = null;
	}

	public ModelioModelResource(File f, String factoryClassName) {
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
			Set<IHawkObject> allElements = new HashSet<IHawkObject>();
			for (Model m : getModels()) {
				allElements.add(new ModelioObject(m));

				EList<org.eclipse.uml2.uml.Element> all = m.allOwnedElements();
				for (org.eclipse.uml2.uml.Element e : all) {
					allElements.add(new ModelioObject(e));
				}
			}
			return allElements;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptySet();
		}
	}

	private List<Model> getModels() throws Exception {
		if (models != null) {
			return models;
		}
		models = new ArrayList<>();

		try (final TemporaryUnzippedFile unzipped = new TemporaryUnzippedFile(modelioZipFile, "mondo-modelio")) {
			final Path tmpDirPath = unzipped.getTemporaryDirectory();
			final Path descriptorPath = find(tmpDirPath, "project.conf");

			try (final ModelioProject p = new ModelioProject(descriptorPath)) {
				for (Package pkg : p.getPackages()) {
					try {
						models.add(p.exportIntoXMI(pkg));
					} catch (Exception ex) {
						LOGGER.error("Could not export package " + pkg.getName(), ex);
					}
				}
				return models;
			}
		}
	}

	protected Path find(final Path basePath, final String fileName) throws IOException {
		final FileLocator locator = new FileLocator(fileName);
		Files.walkFileTree(basePath, locator);
		if (locator.wantedPath == null) {
			throw new IllegalArgumentException("No '" + fileName + "' file was found in " + basePath);
		}
		return locator.wantedPath;
	}

	private static final class FileLocator extends SimpleFileVisitor<Path> {
		private final String wantedName;
		Path wantedPath = null;

		public FileLocator(final String name) {
			this.wantedName = name;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (wantedName.equals(file.getFileName().toString())) {
				wantedPath = file;
				return FileVisitResult.TERMINATE;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (wantedName.equals(dir.getFileName().toString())) {
				wantedPath = dir;
				return FileVisitResult.TERMINATE;
			}
			return super.preVisitDirectory(dir, attrs);
		}
	}

	private static class ModelioProject implements Closeable {

		private final ProjectDescriptor descriptor;
		private GProject project;
		private IProjectService projectService;

		public ModelioProject(Path resolve) throws Exception {
			MetamodelLoader.Load();
			this.descriptor = new ProjectDescriptorReader().read(resolve, DefinitionScope.LOCAL);

			// Normally LifeCycleManager does this, but it depends on too many UI components
			Modelio modelio = Modelio.getInstance();
			Field fldContext = modelio.getClass().getDeclaredField("eclipseContext");
			fldContext.setAccessible(true);
			IEclipseContext modelioEclipseContext = (IEclipseContext)fldContext.get(modelio);
			modelioEclipseContext.set(IModelioEventService.class, new ModelioEventService(modelioEclipseContext));

			projectService = modelioEclipseContext.get(IProjectService.class);
			projectService.openProject(descriptor, new NoneAuthData(), new NullProgressMonitor());
			project = projectService.getOpenedProject();
		}

		public Model exportIntoXMI(Package mainPackage) throws Exception {
			GenerationProperties genProp = GenerationProperties.getInstance();
			genProp.initialize(new MModelServices(project));
			genProp.setTimeDisplayerActivated(false);
			genProp.setSelectedPackage(mainPackage);

			final File xmiExportLog = File.createTempFile("xmiexport", ".log");
			xmiExportLog.deleteOnExit();
			XMILogs.getInstance().setLogFile(xmiExportLog.getAbsolutePath());

			genProp.setReportModel(new ReportModel());
			ExportServices exportService = new ExportServices(null);
			return exportService.createEcoreModel(mainPackage, null);
		}

		public List<Package> getPackages() throws Exception {
			final List<Package> pkgs = new ArrayList<>();
			for (IProjectFragment fragment : project.getOwnFragments()) {
				for (MObject mObj : fragment.getRoots()) {
					if (mObj instanceof Package) {
						pkgs.add((Package) mObj);
					} else if (mObj instanceof Project) {
						pkgs.add(((Project) mObj).getModel());
					}
				}
			}
			return pkgs;
		}

		@Override
		public void close() throws IOException {
			projectService.closeProject(project);
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
	public byte[] getSignature(IHawkObject o) {
		return o.signature();
	}
}
