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

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawkConfig")
public class HawkConfig {

	@XStreamAlias("hawkName")
	protected String name;

	@XStreamAlias("hawkLoc")
	protected String loc;

	@XStreamAlias("hawkFactory")
	protected String hawkFactory = LocalHawkFactory.ID;

	public HawkConfig() {
	}

	public HawkConfig(String n, String l) {
		name = n;
		loc = l;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLoc() {
		return loc;
	}

	public void setLoc(String loc) {
		this.loc = loc;
	}

	public String getHawkFactory() {
		return hawkFactory;
	}

	public void setHawkFactory(String id) {
		this.hawkFactory = id;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof HawkConfig))
			return false;
		else {
			boolean ret = ((HawkConfig) o).getLoc().equals(this.getLoc());
			// System.err.println("equals: " + ((HawkConfig) o).getLoc() + " : "
			// + this.getLoc() + " : " + ret);
			return ret;
		}
	}

	@Override
	public int hashCode() {
		// System.err.println("hashcode");
		return getLoc().hashCode();
	}

}
