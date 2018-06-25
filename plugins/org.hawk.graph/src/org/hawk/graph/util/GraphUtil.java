/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
package org.hawk.graph.util;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphUtil.class);

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
				LOGGER.warn("Unknown type found, converting to String: {}", type);
			}
		}

		return ret;
	}

}
