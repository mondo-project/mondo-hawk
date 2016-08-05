/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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