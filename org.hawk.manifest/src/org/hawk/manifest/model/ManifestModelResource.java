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
package org.hawk.manifest.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.pde.internal.core.bundle.Bundle;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleVersionHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.manifest.ManifestBundleInstanceObject;
import org.hawk.manifest.ManifestBundleObject;
import org.hawk.manifest.ManifestImportObject;
import org.hawk.manifest.ManifestPackageInstanceObject;
import org.hawk.manifest.ManifestPackageObject;
import org.hawk.manifest.ManifestRequiresObject;
import org.hawk.manifest.metamodel.ManifestMetaModelResourceFactory;
import org.osgi.framework.VersionRange;

@SuppressWarnings("restriction")
public class ManifestModelResource implements IHawkModelResource {

	private IModelResourceFactory parser;
	private Set<IHawkObject> allContents = new HashSet<IHawkObject>();
	private String uri;
	private Map<String, IHawkClassifier> types = new HashMap<>();

	public IHawkClassifier getType(String s) {
		return types.get(s);
	}

	public ManifestModelResource(String uri, IModelResourceFactory parser,
			Map<String, String> map) {
		this.uri = uri;
		this.parser = parser;

		//
		Bundle b = new Bundle();
		// ...
		b.setModel(new WorkspaceBundleModel(null));
		b.load(map);

		List<ExportPackageObject> exports = new LinkedList<>();
		List<ImportPackageObject> imports = new LinkedList<>();
		List<RequireBundleObject> requires = new LinkedList<>();
		String symbolicName = null;
		String version = null;
		List<String> otherProperties = new LinkedList<String>();

		for (Entry<String, String> entry : map.entrySet()) {

			String key = entry.getKey();

			IManifestHeader h = b.getManifestHeader(key);

			if (h instanceof ExportPackageHeader) {
				for (ExportPackageObject o : ((ExportPackageHeader) h)
						.getPackages())
					exports.add(o);
			} else if (h instanceof ImportPackageHeader) {
				for (ImportPackageObject o : ((ImportPackageHeader) h)
						.getPackages())
					imports.add(o);
			} else if (h instanceof RequireBundleHeader) {
				for (RequireBundleObject o : ((RequireBundleHeader) h)
						.getRequiredBundles())
					requires.add(o);
			} else if (h instanceof BundleSymbolicNameHeader) {
				symbolicName = ((BundleSymbolicNameHeader) h).getId();
			} else if (h instanceof BundleVersionHeader) {
				VersionRange r = ((BundleVersionHeader) h).getVersionRange();
				version = r.getLeft().toString();
			} else {
				otherProperties.add(key + ": " + entry.getValue());
			}
		}

		// create elements
		ManifestBundleObject bundle = new ManifestBundleObject(symbolicName,
				this);
		allContents.add(bundle);

		ManifestBundleInstanceObject bundleInstance = new ManifestBundleInstanceObject(
				version, this, bundle, otherProperties);
		allContents.add(bundleInstance);

		// add reference targets
		for (RequireBundleObject o : requires) {
			ManifestBundleObject rBundle = new ManifestBundleObject(o.getId(),
					this);
			allContents.add(rBundle);
			ManifestRequiresObject req = new ManifestRequiresObject(
					o.getAttribute("version"), this, rBundle);
			allContents.add(req);
			bundleInstance.addRequires(req);
		}

		for (ImportPackageObject o : imports) {
			ManifestPackageObject iPackage = new ManifestPackageObject(
					o.getName(), this);
			allContents.add(iPackage);
			ManifestImportObject imp = new ManifestImportObject(
					o.getAttribute("version"), this, iPackage);
			allContents.add(imp);
			bundleInstance.addImport(imp);
		}

		for (ExportPackageObject o : exports) {
			ManifestPackageObject ePackage = new ManifestPackageObject(
					o.getName(), this);
			allContents.add(ePackage);
			ManifestPackageInstanceObject pe = new ManifestPackageInstanceObject(
					o.getAttribute("version"), this, ePackage);
			allContents.add(pe);
			bundleInstance.addExport(pe);
		}

		// add types to local registry
		ManifestMetaModelResourceFactory mmresf = new ManifestMetaModelResourceFactory();
		types = mmresf.getTypes();

	}

	@Override
	public void unload() {
		parser = null;
		allContents = null;
	}

	@Override
	public String getType() {
		return parser.getType();
	}

	@Override
	public Iterator<IHawkObject> getAllContents() {
		return allContents.iterator();
	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {
		return allContents;
	}

	public String getUri() {
		return uri;
	}

}
