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

import java.util.Collection;

import org.hawk.core.runtime.ModelIndexerImpl;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawkProperties")
public class HawkProperties {

	@XStreamAlias("dbType")
	protected String dbType;

	@XStreamAlias("monitoredVCS")
	protected Collection<String[]> monitoredVCS;

	@XStreamAlias("minDelay")
	protected int minDelay = ModelIndexerImpl.DEFAULT_MINDELAY;

	@XStreamAlias("maxDelay")
	protected int maxDelay = ModelIndexerImpl.DEFAULT_MAXDELAY;

	public String getDbType() {
		return dbType;
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public Collection<String[]> getMonitoredVCS() {
		return monitoredVCS;
	}

	public void setMonitoredVCS(Collection<String[]> monitoredVCS) {
		this.monitoredVCS = monitoredVCS;
	}

	public int getMinDelay() {
		return minDelay;
	}

	public void setMinDelay(int minDelay) {
		this.minDelay = minDelay;
	}

	public int getMaxDelay() {
		return maxDelay;
	}

	public void setMaxDelay(int maxDelay) {
		this.maxDelay = maxDelay;
	}

	public HawkProperties() {
	}

	public HawkProperties(String dbType, Collection<String[]> monitoredVCS, int minDelay, int maxDelay) {
		this.dbType = dbType;
		this.monitoredVCS = monitoredVCS;
		this.minDelay = minDelay;
		this.maxDelay = maxDelay;
	}

}
