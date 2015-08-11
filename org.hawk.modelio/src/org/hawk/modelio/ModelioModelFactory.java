package org.hawk.modelio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.modelio.metamodel.data.MetamodelLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioModelFactory implements IModelResourceFactory {

	private static final String MODULES_PATH_PROPERTY = "org.hawk.modelio.modules.path";
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioModelFactory.class);

	@Override
	public String getType() {
		return this.getClass().getCanonicalName();
	}

	@Override
	public IHawkModelResource parse(File zipFile) {
		return new ModelioModelResource(zipFile, getModulesPath(), getType());
	}

	@Override
	public void shutdown() {
		// nothing to do!
	}

	@Override
	public Set<String> getModelExtensions() {
		return new HashSet<String>(Arrays.asList("zip"));
	}

	@Override
	public boolean canParse(File f) {
		if (!f.canRead() || !f.isFile() || !f.getName().endsWith("zip")) {
			return false;
		}

		// Make sure we have a .zip with the Modelio project.conf descriptor in its root folder
		try (final ZipFile zipFile = new ZipFile(f)) {
			final ZipEntry confEntry = zipFile.getEntry("project.conf");
			if (confEntry == null) {
				LOGGER.warn("'{}' does not contain a Modelio project description (/project.conf).", f);
				return false;
			}
		} catch (IOException e1) {
			LOGGER.error(String.format("Could not open '%s' as a zip file", f.getName()), e1);
			return false;
		}

		// Try loading the Modelio Metamodel
		MetamodelLoader.Load();

		// Make sure we have the modules path set up as well
		final Path modulesPath = getModulesPath();
		if (modulesPath == null) {
			LOGGER.error("{} has not been set to the path to the Modelio modules directory: cannot parse {}", MODULES_PATH_PROPERTY, f);
			return false;
		}

		return true;
	}

	@Override
	public String getHumanReadableName() {
		return "Modelio parser for Hawk";
	}

	public Path getModulesPath() {
		return Paths.get(System.getProperty(MODULES_PATH_PROPERTY));
	}
}
