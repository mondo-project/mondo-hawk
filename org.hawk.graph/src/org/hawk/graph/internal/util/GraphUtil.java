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

	// made static to avoid repeated error messages about unknown types
	private static HashSet<String> unknownTypes = new HashSet<>();

	public static String toJavaType(String type) {

		// TODO complete set of known types -- possibly delegate to factories to
		// expose them?

		if (type.contains("Int") || type.contains("int")
				|| type.contains("Long") || type.contains("long"))
			return "Integer";
		if (type.contains("Bool") || type.contains("bool"))
			return "Boolean";
		if (type.contains("Double") || type.contains("Real")
				|| type.contains("Float") || type.contains("double")
				|| type.contains("real") || type.contains("float"))
			return "Real";
		if (type.contains("String") || type.contains("string"))
			return "String";

		if (!unknownTypes.contains(type)) {
			System.err
					.println("warning, unknown type found, casting to String: "
							+ type);
			unknownTypes.add(type);
		}

		return type;

	}

}
