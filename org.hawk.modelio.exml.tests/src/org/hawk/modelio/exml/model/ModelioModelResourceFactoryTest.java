package org.hawk.modelio.exml.model;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.junit.Test;

public class ModelioModelResourceFactoryTest {

	private static final String RAMC_PATH = "resources/jenkins/jenkins_1.540.0.ramc";

	@Test
	public void ramc() throws Exception {
		ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(new File(RAMC_PATH));

		final Set<String> ids = new HashSet<>();
		for (IHawkObject o : resource.getAllContentsSet()) {
			if (!ids.add(o.getUriFragment())) {
				fail("Fragment " + o.getUriFragment() + " is repeated in the resource!");
			}
		}
	}
}
