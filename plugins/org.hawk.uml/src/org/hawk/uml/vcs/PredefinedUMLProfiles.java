/*******************************************************************************
 * Copyright (c) 2017 Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.uml.vcs;

import java.util.Arrays;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.hawk.core.IModelIndexer;

/**
 * Exposes the predefined UML profiles as models. These need to be registered as
 * metamodels *and* indexed as models, since some of the elements in the
 * {@link PredefinedUMLLibraries} link to them as both regular model elements
 * and types.
 *
 * Not recommended for use right now.
 */
public class PredefinedUMLProfiles extends PathmapResourceCollection {

	public PredefinedUMLProfiles() {
		super(UMLResource.PROFILES_PATHMAP);
	}

	@Override
	public String getCurrentRevision() throws Exception {
		return getRootNsURI(UMLResource.ECORE_PROFILE_URI);
	}

	@Override
	public void init(String vcsloc, IModelIndexer hawk) throws Exception {
		for (String uri : Arrays.asList(
				UMLResource.ECORE_PROFILE_URI,
				UMLResource.STANDARD_PROFILE_URI,
				UMLResource.UML2_PROFILE_URI)) {
			rs.createResource(URI.createURI(uri)).load(null);
		}
	}

	@Override
	public String getHumanReadableName() {
		return "UML Predefined Profiles";
	}

}
