/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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

	/** adds a new metamodel resource to hawk, from a file */
	IHawkMetaModelResource parse(File f) throws Exception;

	/** provides any static metamodel resources to hawk */
	Set<IHawkMetaModelResource> getStaticMetamodels();

	void shutdown();

	boolean canParse(File f);

	Collection<String> getMetaModelExtensions();

	/** adds a new metamodel resource to hawk, given a string representation */
	IHawkMetaModelResource parseFromString(String name, String contents)
			throws Exception;

	/** saves a package into string format, so it can be loaded by parseFromString */
	String dumpPackageToString(IHawkPackage ePackage) throws Exception;

}
