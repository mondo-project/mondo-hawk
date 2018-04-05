/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.manifest.metamodel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.manifest.ManifestBundle;
import org.hawk.manifest.ManifestBundleInstance;
import org.hawk.manifest.ManifestImport;
import org.hawk.manifest.ManifestMetamodel;
import org.hawk.manifest.ManifestPackage;
import org.hawk.manifest.ManifestPackageInstance;
import org.hawk.manifest.ManifestRequires;

public class ManifestMetaModelResource implements IHawkMetaModelResource {

	Set<IHawkObject> contents = new HashSet<>();
	ManifestMetaModelResourceFactory factory;

	public ManifestMetaModelResource(
			ManifestMetaModelResourceFactory manifestMetaModelResourceFactory) {
		factory = manifestMetaModelResourceFactory;

		ManifestMetamodel p = new ManifestMetamodel(this);
		contents.add(p);

		ManifestBundle b = new ManifestBundle(p);
		p.add(b);
		ManifestBundleInstance bi = new ManifestBundleInstance(p);
		p.add(bi);
		ManifestRequires mr = new ManifestRequires(p);
		p.add(mr);
		ManifestPackage mp = new ManifestPackage(p);
		p.add(mp);
		ManifestPackageInstance mpi = new ManifestPackageInstance(p);
		p.add(mpi);
		ManifestImport mi = new ManifestImport(p);
		p.add(mi);
	}

	@Override
	public void unload() {
		contents = null;
	}

	@Override
	public Set<IHawkObject> getAllContents() {
		return contents;
	}

	@Override
	public IMetaModelResourceFactory getMetaModelResourceFactory() {
		return factory;
	}

	@Override
	public void save(OutputStream output, Map<Object, Object> options)
			throws IOException {
	}

}
