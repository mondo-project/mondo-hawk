package org.hawk.modelio.exml.model;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.junit.Test;

public class ModelioModelResourceFactoryTest {

	private static final String RAMC_PATH = "resources/jenkins/jenkins_1.540.0.ramc";

	@Test
	public void ramc() throws Exception {
		ModelioModelResourceFactory factory = new ModelioModelResourceFactory();
		IHawkModelResource resource = factory.parse(new File(RAMC_PATH));

		final Map<String, IHawkObject> elements = new HashMap<>();
		//final Set<String> rootOrContained = new HashSet<>();
		for (IHawkObject o : resource.getAllContentsSet()) {
			if (elements.put(o.getUriFragment(), o) != null) {
				fail("Fragment " + o.getUriFragment() + " is repeated in the resource!");
			}

			// TODO: need to ask Antonin about dangling Association and AssociationEnds
			/*
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
			*/
		}

		/*
		HashSet<String> unreachable = new HashSet<>(elements.keySet());
		unreachable.removeAll(rootOrContained);
		if (!unreachable.isEmpty()) {
			System.err.println(String.format("Found %d unreachable objects among %d", unreachable.size(), resource.getAllContentsSet().size()));
			for (String o : unreachable) {
				System.err.println(elements.get(o));
			}
			fail("There should be no unreachable objects from the roots");
		}
		*/
	}
}
