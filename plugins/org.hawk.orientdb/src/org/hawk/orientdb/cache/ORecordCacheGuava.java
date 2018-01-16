package org.hawk.orientdb.cache;

import java.util.Collection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.orientechnologies.orient.core.cache.ORecordCache;
import com.orientechnologies.orient.core.cache.ORecordCacheSoftRefs;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ORecord;

/**
 * Variant of {@link ORecordCacheSoftRefs} which uses a Guava cache.
 */
public class ORecordCacheGuava implements ORecordCache {
	private boolean enabled = true;
	private Cache<ORID, ORecord> cache = CacheBuilder.newBuilder().softValues().build();

	@Override
	public ORecord get(final ORID rid) {
		if (!isEnabled())
			return null;
		return cache.getIfPresent(rid);
	}

	@Override
	public ORecord put(final ORecord record) {
		if (!isEnabled())
			return null;

		ORecord oldValue = cache.getIfPresent(record.getIdentity());
		cache.put(record.getIdentity(), record);
		return oldValue; 
	}

	@Override
	public ORecord remove(final ORID rid) {
		if (!isEnabled())
			return null;

		ORecord oldValue = cache.getIfPresent(rid);
		cache.invalidate(rid);
		return oldValue;
	}

	@Override
	public void shutdown() {
		clear();
	}

	@Override
	public void clear() {
		cache.invalidateAll();
	}

	@Override
	public void startup() {
		// nothing to do
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean enable() {
		final boolean oldValue = enabled;
		enabled = true;
		return oldValue != true;
	}

	@Override
	public boolean disable() {
		final boolean oldValue = enabled;
		enabled = false;
		return oldValue != false;
	}

	@Override
	public int size() {
		return (int) cache.size();
	}

	@Override
	public Collection<ORID> keys() {
		return cache.asMap().keySet();
	}
}
