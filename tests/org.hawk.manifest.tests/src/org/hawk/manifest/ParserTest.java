package org.hawk.manifest;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hawk.core.model.IHawkObject;
import org.hawk.manifest.model.ManifestModelResource;
import org.hawk.manifest.model.ManifestModelResourceFactory;
import org.junit.Test;

public class ParserTest {

	@Test
	public void testDuplicateDependencies() {
		ManifestModelResourceFactory rFactory = new ManifestModelResourceFactory();
		ManifestModelResource resource = rFactory.parse(null, new File("resources/dupdep/META-INF/MANIFEST.MF"));

		final Map<String, Integer> instancesByType = new HashMap<>();
		for (IHawkObject obj : resource.getAllContents()) {
			final String typeName = obj.getType().getName();

			Integer oldCount = instancesByType.get(typeName);
			instancesByType.put(typeName, oldCount == null ? 1 : oldCount + 1);
		}

		// 1 from the manifest itself + 1 per require-bundle (even if it's duplicate)
		assertEquals(3, (int) instancesByType.get("ManifestBundle"));
		assertEquals(2, (int) instancesByType.get("ManifestRequires"));
		assertEquals(1, (int) instancesByType.get("ManifestBundleInstance"));
	}
}
