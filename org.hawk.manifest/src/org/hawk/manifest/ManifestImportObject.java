/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.manifest;

import java.util.Arrays;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.manifest.model.ManifestModelResource;
import org.hawk.manifest.utils.Utils;

public class ManifestImportObject extends ManifestObject {

	String minVersion = null;
	String maxVersion = null;
	Boolean isMinVersionInclusive = null;
	Boolean isMaxVersionInclusive = null;

	ManifestPackageObject p;

	public ManifestImportObject(String[] versionRange,
			ManifestModelResource manifestModelResource, ManifestPackageObject p)
			throws Exception {

		this.res = manifestModelResource;

		parseVersionRange(versionRange);

		this.p = p;
	}

	@Override
	public String getUri() {
		return p.getUri()
				+ ": "
				+ new Utils().generateVersionRangeIdentifier(minVersion,
						maxVersion, isMinVersionInclusive,
						isMaxVersionInclusive);
	}

	@Override
	public boolean URIIsRelative() {
		return false;
	}

	@Override
	public String getUriFragment() {
		return p.getUriFragment()
				+ ": "
				+ new Utils().generateVersionRangeIdentifier(minVersion,
						maxVersion, isMinVersionInclusive,
						isMaxVersionInclusive);
	}

	@Override
	public boolean isFragmentUnique() {
		return false;
	}

	@Override
	public IHawkClassifier getType() {
		return res.getType(ManifestImport.CLASSNAME);
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		String name = hsf.getName();
		switch (name) {
		case "package":
			return p != null;
		case "minVersion":
			return minVersion != null;
		case "maxVersion":
			return maxVersion != null;
		case "isMinVersionInclusive":
			return isMinVersionInclusive != null;
		case "isMaxVersionInclusive":
			return isMaxVersionInclusive != null;
		default:
			return false;
		}
	}

	@Override
	public Object get(IHawkAttribute attr) {
		String name = attr.getName();
		switch (name) {
		case "minVersion":
			return minVersion;
		case "maxVersion":
			return maxVersion;
		case "isMinVersionInclusive":
			return isMinVersionInclusive;
		case "isMaxVersionInclusive":
			return isMaxVersionInclusive;
		default:
			return null;
		}
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		String name = ref.getName();
		switch (name) {
		case "package":
			return p;
		default:
			return null;
		}
	}

	private void parseVersionRange(String[] versionRange) throws Exception {

		if (versionRange == null)
			return;

		else if (versionRange.length == 1 || versionRange.length == 2) {

			String min = versionRange[0];

			if (min.startsWith("[")) {
				minVersion = min.substring(1);
				isMinVersionInclusive = true;
			}

			else if (min.startsWith("(")) {
				minVersion = min.substring(1);
				isMinVersionInclusive = false;
			}

			else {
				minVersion = min;
				isMinVersionInclusive = true;
			}

			if (versionRange.length == 2) {

				String max = versionRange[1];

				if (max.endsWith("]")) {
					maxVersion = max.substring(0, max.length() - 1);
					isMaxVersionInclusive = true;
				}

				else if (max.endsWith(")")) {
					maxVersion = max.substring(0, max.length() - 1);
					isMaxVersionInclusive = false;
				}

				else {
					maxVersion = max;
					isMaxVersionInclusive = true;
				}

			}

		}

		else
			throw new Exception("invalid version range:"
					+ Arrays.toString(versionRange));
	}

}
