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
package org.hawk.manifest.metamodel;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;

public class ManifestMetaModelResourceFactory implements
		IMetaModelResourceFactory {

	private static final String TYPE = "org.hawk.manifest.metamodel.ManifestMetaModelParser";
	private static final String HUMAN_READABLE_NAME = "Manifest Metamodel Resource Factory";
	private final Set<String> metamodelExtensions = new HashSet<>();

	public static void main(String[] a) {

		Object o = new ManifestMetaModelResourceFactory().getStaticMetamodels();

	}

	public ManifestMetaModelResourceFactory() {
	}

	public Map<String, IHawkClassifier> getTypes() {
		Map<String, IHawkClassifier> types = new HashMap<>();

		IHawkMetaModelResource res = new ManifestMetaModelResource(this);

		for (IHawkObject o : res.getAllContents()) {
			IHawkPackage p = ((IHawkPackage) o);
			for (IHawkClassifier c : p.getClasses())
				types.put(c.getName(), c);
		}

		return types;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getHumanReadableName() {
		return HUMAN_READABLE_NAME;
	}

	@Override
	public IHawkMetaModelResource parse(File f) throws Exception {
		throw new Exception(
				"ManifestMetaModelResourceFactory cannot parse files, it provides its own static metamodel");
	}

	@Override
	public Set<String> getMetaModelExtensions() {
		return metamodelExtensions;
	}

	@Override
	public boolean canParse(File f) {
		return false;
	}

	@Override
	public HashSet<IHawkMetaModelResource> getStaticMetamodels() {

		HashSet<IHawkMetaModelResource> set = new HashSet<>();
		IHawkMetaModelResource res = new ManifestMetaModelResource(this);
		set.add(res);

		return set;
	}

	@Override
	public void shutdown() {

	}

	@Override
	public IHawkMetaModelResource parseFromString(String name, String contents)
			throws Exception {
		return null;
	}

	@Override
	public String dumpPackageToString(IHawkPackage ePackage) throws Exception {
		return "dummy_string";
	}

}
