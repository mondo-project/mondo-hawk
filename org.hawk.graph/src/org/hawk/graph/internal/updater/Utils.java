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
package org.hawk.graph.internal.updater;

import java.util.Set;

public class Utils {

	protected String makeRelative(Set<String> bases, String extension) {
		for (final String base : bases) {
			if (extension.startsWith(base)) {
				return extension.substring(base.length());
			}
		}
		System.err
				.println(String.format(
						"WARNING: could not make '%s' into a relative path",
						extension));
		return extension;
	}

	protected String[] addToElementProxies(String[] proxies,
			String fullPathURI, String edgelabel, boolean isContainment,
			boolean isContainer) {

		// System.err.println("addtoelementproxies: " +
		// Arrays.toString(proxies));
		// System.err.println("fullpathuri " + fullPathURI);
		// System.err.println("edgelabel " + edgelabel);

		if (proxies != null) {

			String[] ret = new String[proxies.length + 4];

			for (int i = 0; i < proxies.length; i = i + 4) {

				ret[i] = proxies[i];
				ret[i + 1] = proxies[i + 1];
				ret[i + 2] = proxies[i + 2];
				ret[i + 3] = proxies[i + 3];

			}

			ret[proxies.length] = fullPathURI;
			ret[proxies.length + 1] = edgelabel;
			ret[proxies.length + 2] = isContainment + "";
			ret[proxies.length + 3] = isContainer + "";

			proxies = null;

			// System.err.println("ret " + Arrays.toString(ret));

			return ret;

		} else {
			String[] ret = new String[] { fullPathURI, edgelabel,
					isContainment + "", isContainer + "" };
			return ret;
		}
	}

}
