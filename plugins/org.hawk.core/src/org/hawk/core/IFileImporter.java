/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - Initial Implementation of Hawk Server Configuration
 ******************************************************************************/
package org.hawk.core;

import java.io.File;

/**
 * Interface for a component which imports files on demand from a predefined
 * {@link IVcsManager} into a predefined temporary location. Meant to be a
 * heavily restricted version of the {@link IVcsManager} interface for use by
 * various classes that need to fetch extra files on demand.
 */
public interface IFileImporter {

	/**
	 * Imports a file from the VCS to a temporary local directory, and returns
	 * that temporary copy. This temporary directory will be the same one where
	 * the changed files for that VCS are imported.
	 *
	 * @return Files where the contents of that path can be read from, or
	 *         <code>null</code. if that path does not exist or could not be
	 *         read.
	 */
	File importFile(String path);
}
