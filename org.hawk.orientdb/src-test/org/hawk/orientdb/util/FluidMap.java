package org.hawk.orientdb.util;

import java.util.HashMap;

public class FluidMap extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	public static FluidMap create() {
		return new FluidMap();
	}

	public FluidMap add(String key, Object value) {
		put(key, value);
		return this;
	}
}