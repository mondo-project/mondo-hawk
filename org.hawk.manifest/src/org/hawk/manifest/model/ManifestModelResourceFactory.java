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
package org.hawk.manifest.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.osgi.util.ManifestElement;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.osgi.framework.Constants;

public class ManifestModelResourceFactory implements IModelResourceFactory {

	String type = "org.hawk.manifest.metamodel.ManifestModelParser";
	String hrn = "Manifest Model Resource Factory";

	Set<String> modelExtensions;

	public ManifestModelResourceFactory() {
		modelExtensions = new HashSet<String>();
		modelExtensions.add("MANIFEST.MF".toLowerCase());
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getHumanReadableName() {
		return hrn;
	}

	@Override
	public IHawkModelResource parse(File f) {

		Map<String, String> map = getManifestContents(f);
		IHawkModelResource ret = null;
		try {
			ret = new ManifestModelResource(f.toURI().toString(), this, map);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;

	}

	@Override
	public void shutdown() {
	}

	@Override
	public Set<String> getModelExtensions() {
		return modelExtensions;
	}

	@Override
	public boolean canParse(File f) {

		Map<String, String> map = getManifestContents(f);

		return map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME);

	}

	private Map<String, String> getManifestContents(File f) {

		try {
			InputStream i = new FileInputStream(f);
			return ManifestElement.parseBundleManifest(i, null);
		} catch (Exception e) {
			System.err.println("error in parsing manifest file: " + f.getPath()
					+ ", returning null for getManifestContents");
			return null;
		}
	}

}
