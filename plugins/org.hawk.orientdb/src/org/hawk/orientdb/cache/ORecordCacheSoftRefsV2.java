package org.hawk.orientdb.cache;

import com.orientechnologies.orient.core.cache.OAbstractMapCache;
import com.orientechnologies.orient.core.cache.ORecordCache;
import com.orientechnologies.orient.core.cache.ORecordCacheSoftRefs;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ORecord;

/**
 * Variant of {@link ORecordCacheSoftRefs} which uses a slightly patched up {@link OSoftRefsHashMapV2}.
 */
public class ORecordCacheSoftRefsV2 extends OAbstractMapCache<OSoftRefsHashMapV2<ORID, ORecord>> implements ORecordCache {

  public ORecordCacheSoftRefsV2() {
    super(new OSoftRefsHashMapV2<ORID, ORecord>());
  }

  @Override
  public ORecord get(final ORID rid) {
    if (!isEnabled())
      return null;

    return cache.get(rid);
  }

  @Override
  public ORecord put(final ORecord record) {
    if (!isEnabled())
      return null;
    return cache.put(record.getIdentity(), record);
  }

  @Override
  public ORecord remove(final ORID rid) {
    if (!isEnabled())
      return null;
    return cache.remove(rid);
  }

  @Override
  public void shutdown() {
    clear();
  }

  @Override
  public void clear() {
    cache.clear();
    cache = new OSoftRefsHashMapV2<ORID, ORecord>();
  }
}
