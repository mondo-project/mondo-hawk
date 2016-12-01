package org.hawk.neo4j_v2;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.hawk.epsilon.emc.EOLQueryEngine;
import org.junit.Test;

public class DerivedFeatureTest extends AbstractGraphTest {

	@Test
	public void derivedEdge() throws Throwable {
		setup("tree-dedge");
		indexer.registerMetamodels(new File("resources/metamodels/Ecore.ecore"),
				new File("resources/metamodels/Tree.ecore"));
		requestFolderIndex(new File("resources/models/tree-dedges"));

		waitForSync(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				indexer.addDerivedAttribute("Tree", "Tree", "descendants", "dummy", true, true, false,
						EOLQueryEngine.TYPE, "return self.closure(c|c.children).flatten;");

				// Forward derived edges
				{
					final Map<String, Integer> expected = new HashMap<>();
					expected.put("t3", 3);
					expected.put("t4", 1);
					expected.put("t5", 0);
					expected.put("t6", 0);

					for (Entry<String, Integer> entry : expected.entrySet()) {
						final Map<String, Object> context = Collections.singletonMap(EOLQueryEngine.PROPERTY_ARGUMENTS,
								(Object) Collections.singletonMap("nodeLabel", entry.getKey()));

						Object result = queryEngine.query(indexer,
							"return Tree.all.selectOne(t|t.label=nodeLabel).descendants.size;", context);
						assertEquals(
							String.format("%s should have %d descendants", entry.getKey(), entry.getValue()),
							entry.getValue(), result);
					}
				}

				// Reverse derived edges
				{
					final Map<String, Integer> reverseExpected = new HashMap<>();
					reverseExpected.put("t3", 0);
					reverseExpected.put("t4", 1);
					reverseExpected.put("t5", 1);
					reverseExpected.put("t6", 2);

					for (Entry<String, Integer> entry : reverseExpected.entrySet()) {
						final Map<String, Object> context = Collections.singletonMap(EOLQueryEngine.PROPERTY_ARGUMENTS,
								(Object) Collections.singletonMap("nodeLabel", entry.getKey()));

						Object result = queryEngine.query(indexer,
								"return Tree.all.selectOne(t|t.label=nodeLabel).revRefNav_descendants.size;", context);
						assertEquals(
							String.format("%s should have %d ancestors", entry.getKey(), entry.getValue()),
							entry.getValue(), result);
					}
				}

				// Reverse reference navigation for the edges returns the element nodes and not just the derived value nodes
				List<String> expected = Arrays.asList("t3", "t4");
				Object result = queryEngine.query(indexer,
					"return Tree.all.selectOne(t|t.label='t6').revRefNav_descendants.label;",
					Collections.<String,Object>emptyMap());
				assertEquals(expected, result);

				return null;
			}
		});
	}

	// TODO: indexed lookup tests
}
