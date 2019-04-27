/*******************************************************************************
 * Copyright (c) 2019 Aston University.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.timeaware.tests.annotators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;

public final class TestUtils {

	private TestUtils() {}

	public static void assertHasNodes(Iterable<ITimeAwareGraphNode> iterable, ITimeAwareGraphNode... expectedNodes) {
		final Iterator<ITimeAwareGraphNode> itVersions = iterable.iterator();

		for (int i = 0; i < expectedNodes.length; i++) {
			assertTrue("Element " + i + " exists", itVersions.hasNext());

			final ITimeAwareGraphNode expectedNode = expectedNodes[i];
			final ITimeAwareGraphNode actualNode = itVersions.next();
			assertEquals("Element " + i + " has the same ID", expectedNode.getId(), actualNode.getId());
			assertEquals("Element " + i + " has the same time", expectedNode.getTime(), actualNode.getTime());
		}
		
		assertFalse("There are exactly " + expectedNodes.length + " elements", itVersions.hasNext());
	}

	public static void assertEmpty(Iterable<ITimeAwareGraphNode> iterable) {
		assertFalse(iterable.iterator().hasNext());
	}

}
