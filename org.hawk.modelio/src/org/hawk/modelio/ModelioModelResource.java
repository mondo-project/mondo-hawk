package org.hawk.modelio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.hawk.core.IModelResourceFactory;
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

	private File modelioProj;
	private final IModelResourceFactory parser;

	@Override
	public void unload() {
		modelioProj = null;
	}

	public ModelioModelResource(File f, IModelResourceFactory p) {
		parser = p;
		modelioProj = f;
	}

	@Override
	public Iterator<IHawkObject> getAllContents() {
		return getAllContentsSet().iterator();
	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {
		try {
			final Model modelioElements = getModel(modelioProj.getCanonicalPath());
			Set<IHawkObject> allElements = new HashSet<IHawkObject>();

			EList<Element> all = modelioElements.allOwnedElements();
			for (Element e : all) {
				allElements.add(new ModelioObject(e));
			}
			return allElements;
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptySet();
		}
	}

	private Model getModel(String path) {
		// Start by loading the Modelio Metamodel
		MetamodelLoader.Load();

		// Load the description of a given project
		GProject gp = null;
		try {
			final String modulesPath = System.getProperty(ModelioModelFactory.MODULES_PATH_PROPERTY);
			if (modulesPath == null) {
				LOGGER.error(ModelioModelFactory.MODULES_PATH_PROPERTY
					+ " has not been set to the path to the Modelio modules directory: cannot parse {}",
					path);
				return null;
			}
			final Path fms = Paths.get(modulesPath);

			final Path p = modelioProj.toPath();
			LOGGER.info("Loading {}", p);
			ProjectDescriptor pd = new ProjectDescriptorReader().read(p, DefinitionScope.LOCAL);

			gp = (GProject) GProjectFactory.openProject(pd, new NoneAuthData(),
					new FileModuleStore(fms), null, new NullProgress());

			// Navigate through the modelio model
			for (IProjectFragment ipf : gp.getOwnFragments()) {
				if (ipf.getType().equals(FragmentType.EXML)) {

					ExmlFragment ef = (ExmlFragment) ipf;

					Iterator<MObject> iterator = ef.doGetRoots().iterator();
					Project o = (Project) iterator.next();

					// Get the Model package of the first fragment
					Package entryPoint = o.getModel();

					// XMI Export

					// Initiate the generation properties
					GenerationProperties genProp = GenerationProperties.getInstance();
					genProp.initialize(new MModelServices(gp));
					genProp.setTimeDisplayerActivated(false);
					genProp.setSelectedPackage(entryPoint);

					// XMI Export
					ExportServices exportService = new ExportServices();
					return exportService.createEcoreModel(entryPoint, null);
				}
			}
		} catch (IOException e1) {
			LOGGER.error(e1.getMessage(), e1);
		} finally {
			if (gp != null) {
				gp.close();
			}
		}

		return null;
	}

	@Override
	public String getType() {
		return parser.getType();
	}

	@Override
	public int getSignature(IHawkObject o) {
		return o.hashCode();
	}
}
