/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core;


/**
 * Interface for a factory of {@link IHawk} objects. Useful for having plugins
 * contribute different implementations of {@link IHawk} (which may introduce
 * their own dependencies).
 */
public interface IHawkFactory {

	/**
	 * Creates a new instance of the desired implementation.
	 */
	IHawk create(String name, String location, IAbstractConsole console) throws Exception;


	/**
	 * Indicates whether the created instance should be customized with the
	 * contents of the extension points in the local installation (
	 * <code>true</code>) or not ( <code>false</code>).
	 */
	boolean instancesAreExtensible();

	/**
	 * Indicates whether instances require creating a local graph (
	 * <code>true</code>), or not (<code>false</code>).
	 */
	boolean instancesUseLocalGraph();
}
