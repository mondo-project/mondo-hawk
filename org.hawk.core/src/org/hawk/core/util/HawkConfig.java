package org.hawk.core.util;

import java.util.Collection;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawkconfig")
public class HawkConfig {

	@XStreamAlias("locations")
	public Collection<String> locs;

	public HawkConfig(Collection<String> locations) {
		this.locs = locations;
	}

}
