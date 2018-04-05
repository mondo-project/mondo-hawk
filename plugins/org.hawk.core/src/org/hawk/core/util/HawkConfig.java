/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.util;

import java.util.List;

import org.hawk.core.runtime.LocalHawkFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawkConfig")
public class HawkConfig {

	@XStreamAlias("hawkName")
	protected String name;

	@XStreamAlias("hawkLoc")
	protected String storageFolder;

	@XStreamAlias("hawkRemoteLocation")
	protected String location;

	@XStreamAlias("hawkFactory")
	protected String hawkFactory = LocalHawkFactory.ID;

	@XStreamAlias("hawkEnabledPlugins")
	protected List<String> hawkEnabledPlugins;

	public HawkConfig() {
	}

	public HawkConfig(String name, String storage, String location, String factory, List<String> plugins) {
		this.name = name;
		this.storageFolder = storage;
		this.location = location;
		this.hawkFactory = factory;
		this.hawkEnabledPlugins = plugins;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStorageFolder() {
		return storageFolder;
	}

	public void setStorageFolder(String storageFolder) {
		this.storageFolder = storageFolder;
	}

	public String getHawkFactory() {
		return hawkFactory;
	}

	public void setHawkFactory(String id) {
		this.hawkFactory = id;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public List<String> getEnabledPlugins() {
		return hawkEnabledPlugins;
	}

	public void setEnabledPlugins(List<String> hawkEnabledPlugins) {
		this.hawkEnabledPlugins = hawkEnabledPlugins;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof HawkConfig))
			return false;
		else {
			boolean ret = ((HawkConfig) o).getStorageFolder().equals(this.getStorageFolder());
			// System.err.println("equals: " + ((HawkConfig) o).getLoc() + " : "
			// + this.getLoc() + " : " + ret);
			return ret;
		}
	}

	@Override
	public int hashCode() {
		// System.err.println("hashcode");
		return getStorageFolder().hashCode();
	}

}
