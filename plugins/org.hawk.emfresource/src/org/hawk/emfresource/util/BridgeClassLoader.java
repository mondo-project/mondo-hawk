/*******************************************************************************
 * Copyright (c) 2015 University of York.
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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.emfresource.util;

/**
 * Classloader that combines two existing classloaders into one. Based on the
 * example from http://www.infoq.com/articles/code-generation-with-osgi.
 *
 * This classloader is only used for the lazy loading modes.
 */
class BridgeClassLoader extends ClassLoader {
	private final ClassLoader secondary;

	public BridgeClassLoader(ClassLoader primary, ClassLoader secondary) {
		super(primary);
		this.secondary = secondary;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return secondary.loadClass(name);
	}
}