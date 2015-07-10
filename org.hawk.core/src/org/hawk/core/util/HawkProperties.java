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

import java.util.Collection;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawkProperties")
public class HawkProperties {

	@XStreamAlias("dbType")
	protected String dbType;

	@XStreamAlias("monitoredVCS")
	protected Collection<String[]> monitoredVCS;

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

	public HawkProperties() {
	}

	public HawkProperties(String dbType, Collection<String[]> monitoredVCS) {
		this.dbType = dbType;
		this.monitoredVCS = monitoredVCS;
	}

}
