/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
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
package org.hawk.manifest.utils;

import java.util.Set;

import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public class Utils {

	public String generateVersionRangeIdentifier(String minVersion, String maxVersion, Boolean isMinVersionInclusive,
			Boolean isMaxVersionInclusive) {

		if (minVersion == null)
			return "null";

		String ret = "";

		if (isMinVersionInclusive)
			ret = ret + "[";
		else
			ret = ret + "(";

		ret = ret + minVersion;

		if (maxVersion != null) {

			ret = ret + "," + maxVersion;

			if (isMaxVersionInclusive)
				ret = ret + "]";
			else
				ret = ret + ")";

		}

		return ret;

	}

	public IHawkStructuralFeature getReference(String string, Set<IHawkReference> references) {

		for (IHawkReference r : references)
			if (r.getName().equals(string))
				return r;

		return null;
	}

}
