/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.internal.util;

import java.util.HashSet;

public class GraphUtil {

	private GraphUtil() {}

	// made static to avoid repeated error messages about unknown types
	private static HashSet<String> unknownTypes = new HashSet<>();

	public static boolean isPrimitiveOrWrapperType(final Class<?> valueClass) {
		boolean ret = String.class.isAssignableFrom(valueClass)
				|| Boolean.class.isAssignableFrom(valueClass)
				|| Character.class.isAssignableFrom(valueClass)
				|| Byte.class.isAssignableFrom(valueClass)
				|| Short.class.isAssignableFrom(valueClass)
				|| Integer.class.isAssignableFrom(valueClass)
				|| Long.class.isAssignableFrom(valueClass)
				|| Float.class.isAssignableFrom(valueClass)
				|| Double.class.isAssignableFrom(valueClass)
				|| boolean.class.isAssignableFrom(valueClass)
				|| char.class.isAssignableFrom(valueClass)
				|| byte.class.isAssignableFrom(valueClass)
				|| short.class.isAssignableFrom(valueClass)
				|| int.class.isAssignableFrom(valueClass)
				|| long.class.isAssignableFrom(valueClass)
				|| float.class.isAssignableFrom(valueClass)
				|| double.class.isAssignableFrom(valueClass);

		if (!ret) {
			final String type = valueClass.getName();
			if (unknownTypes.add(type)) {
				System.err.println("warning, unknown type found, converting to String: " + type);
			}
		}

		return ret;
	}

}
