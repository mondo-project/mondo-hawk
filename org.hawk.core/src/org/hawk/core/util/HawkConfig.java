package org.hawk.core.util;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("hawkConfig")
public class HawkConfig {

	@XStreamAlias("hawkName")
	protected String name;

	@XStreamAlias("hawkLoc")
	protected String loc;

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
