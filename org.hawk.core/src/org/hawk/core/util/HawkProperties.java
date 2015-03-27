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
