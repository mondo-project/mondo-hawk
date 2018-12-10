/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.timeaware.factory;

import java.io.File;
import java.util.List;

import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawk;
import org.hawk.core.IHawkFactory;
import org.hawk.core.IHawkPlugin;

public class TimeAwareHawkFactory implements IHawkFactory {

	@Override
	public IHawk create(String name, File storageFolder, String location, ICredentialsStore credStore, IConsole console,
			List<String> enabledPlugins) throws Exception {
		return new TimeAwareHawk(name, storageFolder, credStore, console);
	}

	@Override
	public InstanceInfo[] listInstances(String location) throws Exception {
		return new InstanceInfo[0];
	}

	@Override
	public List<String> listBackends(String location) throws Exception {
		// ask elsewhere
		return null;
	}

	@Override
	public List<IHawkPlugin> listPlugins(String location) throws Exception {
		// ask elsewhere
		return null;
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
		return true;
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public String getHumanReadableName() {
		return "Time-aware local Hawk";
	}

}
