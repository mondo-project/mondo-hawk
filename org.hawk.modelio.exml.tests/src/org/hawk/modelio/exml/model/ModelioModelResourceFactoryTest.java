package org.hawk.modelio.exml.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioReference;
import org.junit.Test;

public class ModelioModelResourceFactoryTest {

	private static final String RAMC_PATH = "resources/jenkins/jenkins_1.540.0.ramc";
	private static final String ICONTAINMENT_PATH = "resources/implicitContainment/example.exml";

	@SuppressWarnings("unchecked")
	@Test
	public void ramc() throws Exception {
		ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(new File(RAMC_PATH));

		final Map<String, IHawkObject> elements = new HashMap<>();
		final Set<String> rootOrContained = new HashSet<>();
		for (IHawkObject o : resource.getAllContentsSet()) {
			if (elements.put(o.getUriFragment(), o) != null) {
				fail("Fragment " + o.getUriFragment() + " is repeated in the resource!");
			}

			if (o.isRoot()) {
				rootOrContained.add(o.getUriFragment());
			}
			if (o.getType() instanceof IHawkClass) {
				for (IHawkReference ref : ((IHawkClass) o.getType()).getAllReferences()) {
					if (ref.isContainment()) {
						Object value = o.get(ref, true);
						if (value instanceof Iterable) {
							for (IHawkObject target : (Iterable<IHawkObject>) value) {
								rootOrContained.add(target.getUriFragment());
							}
						} else if (value instanceof IHawkObject) {
							rootOrContained.add(((IHawkObject) value).getUriFragment());
						}
					} else if (ref.isContainer()) {
						rootOrContained.add(o.getUriFragment());
					}
				}
			}
		}

		HashSet<String> unreachable = new HashSet<>(elements.keySet());
		unreachable.removeAll(rootOrContained);
		if (!unreachable.isEmpty()) {
			System.err.println(String.format("Found %d unreachable objects among %d", unreachable.size(), resource.getAllContentsSet().size()));
			for (String o : unreachable) {
				System.err.println(elements.get(o));
			}
			fail("There should be no unreachable objects from the roots");
		}
	}

	@Test
	public void implicitContainment() throws Exception {
		final String grandparentUID = "25801104-0000-1069-0000-000000000000";
		final String parentUID = "00d01054-0001-1627-0000-000000000000";
		final String childUID = "00d011d0-0000-041f-0000-000000000000";

		ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(new File(ICONTAINMENT_PATH));

		Set<IHawkObject> allContents = resource.getAllContentsSet();
		for (IHawkObject ob : allContents) {
			final ModelioObject mob = (ModelioObject)ob;
			final ModelioReference refParent = (ModelioReference)mob.getType().getStructuralFeature(ModelioClass.REF_PARENT);
			final ModelioProxy refValue = (ModelioProxy) mob.get(refParent, true);

			switch (mob.getUriFragment()) {
			case parentUID:
				assertNotNull("Parent of the parent should be set", refValue);
				assertEquals("Parent of the parent should be " + grandparentUID, grandparentUID, refValue.getUriFragment());
				break;
			case childUID:
				assertNotNull("Parent of the child should be set", refValue);
				assertEquals("Parent of the child should be the parent", parentUID, refValue.getUriFragment());
				break;
			default:
				fail("Unexpected object " + mob.getUriFragment());
			}
		}
	}
}
