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
package org.hawk.core;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.hawk.core.model.IHawkMetaModelResource;
import org.hawk.core.model.IHawkPackage;

public interface IMetaModelResourceFactory {

	String getType();

	String getHumanReadableName();

	// adds a new metamodel resource to hawk, from a file
	IHawkMetaModelResource parse(File f) throws Exception;

	// provides any static metamodel resources to hawk
	Set<IHawkMetaModelResource> getStaticMetamodels();

	void shutdown();

	boolean canParse(File f);

	Collection<String> getMetaModelExtensions();

	// creates a metamodel resource with name s, from package p
	IHawkMetaModelResource createMetamodelWithSinglePackage(String s,
			IHawkPackage p);

	// adds a new metamodel resource to hawk, given a string representation
	IHawkMetaModelResource parseFromString(String name, String contents)
			throws Exception;

	// removes a metamodel from this factory's static metamodels
	void removeMetamodel(String property);

}
