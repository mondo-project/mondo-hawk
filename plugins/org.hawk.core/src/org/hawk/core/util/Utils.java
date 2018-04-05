/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Utils {

	public String toString(Object source) {

		// non arrays
		if (source.getClass().getComponentType() == null)
			return source.toString();

		//
		if (source instanceof int[])
			return Arrays.toString((int[]) source);
		if (source instanceof long[])
			return Arrays.toString((long[]) source);
		if (source instanceof boolean[])
			return Arrays.toString((boolean[]) source);
		if (source instanceof String[])
			return Arrays.toString((String[]) source);
		if (source instanceof Object[])
			return Arrays.toString((Object[]) source);

		System.err.println("Utils.toString used with a:" + source.getClass());
		return source.toString();

	}

	public List<?> asList(Object source) {

		// already a collection
		if (source instanceof Collection<?>)
			return Arrays.asList(((Collection<?>) source).toArray());

		// object arrays
		if (source instanceof Object[])
			return Arrays.asList((Object[]) source);

		List<Object> ret = new LinkedList<>();
		// primitive arrays
		if (source instanceof int[]) {
			for (int i : (int[]) source)
				ret.add(new Integer(i));
		} else if (source instanceof long[]) {
			for (long i : (long[]) source)
				ret.add(new Long(i));
		} else if (source instanceof boolean[]) {
			for (boolean i : (boolean[]) source)
				ret.add(new Boolean(i));
		} else { // default -- wrap contents by single valued list
			ret.add(source);
		}

		return ret;

	}

}
