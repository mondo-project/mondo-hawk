/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.util;

import org.hawk.core.runtime.LocalHawkFactory;
import org.hawk.core.runtime.ModelIndexerImpl;

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

	public HawkConfig() {
	}

	public HawkConfig(String name, String storage, String location, String factory) {
		this.name = name;
		this.storageFolder = storage;
		this.location = location;
		this.hawkFactory = factory;
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
