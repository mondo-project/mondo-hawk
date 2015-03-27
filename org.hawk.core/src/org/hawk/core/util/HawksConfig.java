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
