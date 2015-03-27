package org.hawk.core.util;

import java.util.Collection;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawkConfig")
public class HawkConfig {

	@XStreamAlias("locations")
	protected Collection<String> locs;

	public Collection<String> getLocs() {
		return locs;
	}

	public void setLocs(Collection<String> locs) {
		this.locs = locs;
	}

	public HawkConfig(Collection<String> locations) {
		this.locs = locations;
	}

	public void addLocs(String folder) {
		this.locs.add(folder);
	}

	public void removeLoc(String folder) {
		this.locs.remove(folder);
	}

}
