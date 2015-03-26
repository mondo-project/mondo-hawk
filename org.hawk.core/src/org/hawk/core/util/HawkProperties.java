package org.hawk.core.util;

import java.util.Collection;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawkproperties")
public class HawkProperties {

	@XStreamAlias("type")
	public String dbType;

	@XStreamAlias("monitored")
	public Collection<String> monitoredVCS;

	public HawkProperties(String dbType, Collection<String> monitoredVCS) {
		this.dbType = dbType;
		this.monitoredVCS = monitoredVCS;
	}

}
