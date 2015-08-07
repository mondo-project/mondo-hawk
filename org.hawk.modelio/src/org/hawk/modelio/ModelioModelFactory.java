package org.hawk.modelio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.modelio.gproject.data.project.DefinitionScope;
import org.modelio.gproject.data.project.ProjectDescriptor;
import org.modelio.gproject.data.project.ProjectDescriptorReader;
import org.modelio.gproject.gproject.GProject;
import org.modelio.gproject.gproject.GProjectFactory;
import org.modelio.gproject.module.catalog.FileModuleStore;
import org.modelio.metamodel.data.MetamodelLoader;
import org.modelio.vbasic.auth.NoneAuthData;
import org.modelio.vbasic.progress.NullProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioModelFactory implements IModelResourceFactory {

	static final String MODULES_PATH_PROPERTY = "org.hawk.modelio.modules.path";
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioModelFactory.class);

	@Override
	public String getType() {
		return this.getClass().getCanonicalName();
	}

	@Override
	public IHawkModelResource parse(File f) {
		return new ModelioModelResource(f, this);
	}

	@Override
	public void shutdown() {
		// nothing to do!
	}

	@Override
	public Set<String> getModelExtensions() {
		return new HashSet<String>(Arrays.asList("conf"));
	}

	@Override
	public boolean canParse(File f) {
		if (!f.canRead() || !f.isFile() || !f.getName().endsWith("conf")) {
			return false;
		}

		// Start by loading the Modelio Metamodel
		MetamodelLoader.Load();

		try {
			final String modulesPath = System.getProperty(MODULES_PATH_PROPERTY);
			if (modulesPath == null) {
				LOGGER.error(MODULES_PATH_PROPERTY + " has not been set to the path to the Modelio modules directory: cannot parse {}", f);
				return false;
			}
			final Path fms = Paths.get(modulesPath);

			// Load the description of a given project
			final Path p = Paths.get(f.getCanonicalPath());
			final ProjectDescriptorReader reader = new ProjectDescriptorReader();
			final ProjectDescriptor pd = reader.read(p, DefinitionScope.LOCAL);

			// Load the project
			final GProject gp = GProjectFactory.openProject(pd, new NoneAuthData(),
					new FileModuleStore(fms), null, new NullProgress());
			gp.close();

			return true;
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public String getHumanReadableName() {
		return "Modelio parser for Hawk";
	}

}
