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

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.hawk.manifest.model.ManifestModelResource;
import org.hawk.manifest.utils.Utils;

public class ManifestRequiresObject extends ManifestObject {

	private String minVersion = null;
	private String maxVersion = null;
	private Boolean isMinVersionInclusive = null;
	private Boolean isMaxVersionInclusive = null;
	private Boolean optionalResolution = null;

	private ManifestBundleObject bundle;

	public ManifestRequiresObject(String[] versionRange, String optional, ManifestModelResource manifestModelResource,
			ManifestBundleObject bundle) {

		optionalResolution = optional != null ? optional.equals("optional") : false;

		this.res = manifestModelResource;

		parseVersionRange(versionRange);

		this.bundle = bundle;
	}

	@Override
	public String getUri() {
		return bundle.getUri() + ": " + new Utils().generateVersionRangeIdentifier(minVersion, maxVersion,
				isMinVersionInclusive, isMaxVersionInclusive);
	}

	@Override
	public String getUriFragment() {
		return bundle.getUriFragment() + ": " + new Utils().generateVersionRangeIdentifier(minVersion, maxVersion,
				isMinVersionInclusive, isMaxVersionInclusive);
	}

	@Override
	public boolean isFragmentUnique() {
		return false;
	}

	@Override
	public IHawkClassifier getType() {
		return res.getType(ManifestRequires.CLASSNAME);
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		String name = hsf.getName();
		switch (name) {
		case "bundle":
			return bundle != null;
		case "minVersion":
			return minVersion != null;
		case "maxVersion":
			return maxVersion != null;
		case "isMinVersionInclusive":
			return isMinVersionInclusive != null;
		case "isMaxVersionInclusive":
			return isMaxVersionInclusive != null;
		case "optionalResolution":
			return optionalResolution != null;
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
		case "optionalResolution":
			return optionalResolution;
		default:
			return null;
		}
	}

	@Override
	public Object get(IHawkReference ref, boolean b) {
		String name = ref.getName();
		switch (name) {
		case "bundle":
			return bundle;
		default:
			return null;
		}
	}

	private void parseVersionRange(String[] versionRange) {

		if (versionRange == null)
			return;

		if (versionRange.length == 1 || versionRange.length == 2) {

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

		// default to no version if range is malformed
		// else
		// throw new Exception("invalid version range:" +
		// Arrays.toString(versionRange));
	}

}
