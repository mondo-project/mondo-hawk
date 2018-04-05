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
package org.hawk.core.util;

import java.util.Collection;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawksConfig")
public class HawksConfig {

	@XStreamAlias("hawks")
	protected Collection<HawkConfig> hawks;

	public Collection<HawkConfig> getConfigs() {
		return hawks;
	}

	public void setLocs(Collection<HawkConfig> locs) {
		this.hawks = locs;
	}

	public HawksConfig(Collection<HawkConfig> locations) {
		this.hawks = locations;
	}

	public void addLocs(HawkConfig config) {
		this.hawks.add(config);
	}

	public boolean removeLoc(HawkConfig config) {
		return this.hawks.remove(config);
	}

}
