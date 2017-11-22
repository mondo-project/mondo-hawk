package org.hawk.uml;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.emf.EMFClass;
import org.hawk.emf.EMFPackage;
import org.hawk.uml.metamodel.UMLMetaModelResourceFactory;
import org.hawk.uml.metamodel.UMLWrapperFactory;
import org.hawk.uml.model.UMLModelResourceFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the handling of UML profiles within Hawk.
 *
 * EMF UML2 converts UML profiles to ECore - the code for this can be see in
 * {@link org.eclipse.uml2.uml.util.UMLUtil#Profile2EPackageConverter}.
 */
public class UMLProfileParsingTest {

	private static final String LATER_PROFILE_NSURI = "http://github.com/mondo-project/mondo-hawk/simpleProfile";
	private static final String FIRST_PROFILE_NSURI = "http:///schemas/RootElement/_I0k2UM7mEeeDhYwXdKEW_w/0";
	private static final File BASE_DIR = new File("../org.hawk.integration.tests/resources/models/uml/");

	private UMLMetaModelResourceFactory mmFactory;
	private UMLModelResourceFactory mFactory;

	@Before
	public void setup() {
		this.mmFactory = new UMLMetaModelResourceFactory();
		this.mFactory = new UMLModelResourceFactory();
	}

	@Test
	public void profileVersioning() throws Exception {
		IHawkMetaModelResource metamodel = mmFactory.parse(new File(BASE_DIR, "simpleProfile/model.profile.uml"));

		final UMLWrapperFactory wf = new UMLWrapperFactory();
		List<IHawkPackage> packages = new ArrayList<>();
		for (IHawkObject ob : metamodel.getAllContents()) {
			if (ob instanceof IHawkPackage) {
				final EMFPackage pkg = (EMFPackage) ob;
				packages.add(pkg);
				assertThat(wf.isProfile(pkg.getEObject()), equalTo(true));
			}
		}

		assertThat(packages, hasSize(5));

		final List<String> nsURIs = packages.stream().map(pkg -> pkg.getNsURI()).collect(Collectors.toList());

		assertThat(nsURIs, hasItem(FIRST_PROFILE_NSURI + "/0.0.1"));
		for (int i = 2; i <= 5; i++) {
			assertThat(nsURIs, hasItem(LATER_PROFILE_NSURI + "/0.0." + i));
		}
	}

	@Test
	public void modelsHonorProfileVersions() throws Exception {
		IHawkModelResource model = mFactory.parse(null, new File(BASE_DIR, "simpleProfileApplication/model.uml"));

		for (IHawkObject ob : model.getAllContents()) {
			final EMFClass type = (EMFClass) ob.getType();
			if ("special".equals(type.getName())) {
				assertThat(type.getPackageNSURI(), equalTo(LATER_PROFILE_NSURI + "/0.0.4"));
				return;
			}
		}

		fail("Should have found an application of a stereotype");
	}
}
