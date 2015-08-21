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
package org.hawk.core.runtime;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.IHawk;
import org.hawk.core.IHawkFactory;

public class LocalHawkFactory implements IHawkFactory {

	/**
	 * Unique identifier for this factory, as defined in the <code>plugin.xml</code> file.
	 */
	public final static String ID = "org.hawk.core.hawkFactory.local";

	@Override
	public IHawk create(String name, File localStorageFolder, String location, IAbstractConsole console) throws Exception {
		return new LocalHawk(name, localStorageFolder, console);
	}

	@Override
	public boolean instancesAreExtensible() {
		return true;
	}

	@Override
	public boolean instancesCreateGraph() {
		return true;
	}

	@Override
	public boolean instancesUseLocation() {
		return false;
	}

	@Override
	public Map<String, Boolean> listInstances(String location) {
		// TODO Auto-generated method stub
		return Collections.emptyMap();
	}

}
