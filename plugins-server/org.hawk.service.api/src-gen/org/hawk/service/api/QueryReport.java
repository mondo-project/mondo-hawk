/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.hawk.service.api;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)", date = "2018-10-07")
public class QueryReport implements org.apache.thrift.TBase<QueryReport, QueryReport._Fields>, java.io.Serializable, Cloneable, Comparable<QueryReport> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("QueryReport");

  private static final org.apache.thrift.protocol.TField RESULT_FIELD_DESC = new org.apache.thrift.protocol.TField("result", org.apache.thrift.protocol.TType.STRUCT, (short)1);
  private static final org.apache.thrift.protocol.TField WALL_MILLIS_FIELD_DESC = new org.apache.thrift.protocol.TField("wallMillis", org.apache.thrift.protocol.TType.I64, (short)2);
  private static final org.apache.thrift.protocol.TField IS_CANCELLED_FIELD_DESC = new org.apache.thrift.protocol.TField("isCancelled", org.apache.thrift.protocol.TType.BOOL, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new QueryReportStandardSchemeFactory());
    schemes.put(TupleScheme.class, new QueryReportTupleSchemeFactory());
  }

  public QueryResult result; // required
  public long wallMillis; // required
  public boolean isCancelled; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    RESULT((short)1, "result"),
    WALL_MILLIS((short)2, "wallMillis"),
    IS_CANCELLED((short)3, "isCancelled");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // RESULT
          return RESULT;
        case 2: // WALL_MILLIS
          return WALL_MILLIS;
        case 3: // IS_CANCELLED
          return IS_CANCELLED;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __WALLMILLIS_ISSET_ID = 0;
  private static final int __ISCANCELLED_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.RESULT, new org.apache.thrift.meta_data.FieldMetaData("result", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, QueryResult.class)));
    tmpMap.put(_Fields.WALL_MILLIS, new org.apache.thrift.meta_data.FieldMetaData("wallMillis", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.IS_CANCELLED, new org.apache.thrift.meta_data.FieldMetaData("isCancelled", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(QueryReport.class, metaDataMap);
  }

  public QueryReport() {
  }

  public QueryReport(
    QueryResult result,
    long wallMillis,
    boolean isCancelled)
  {
    this();
    this.result = result;
    this.wallMillis = wallMillis;
    setWallMillisIsSet(true);
    this.isCancelled = isCancelled;
    setIsCancelledIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public QueryReport(QueryReport other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetResult()) {
      this.result = new QueryResult(other.result);
    }
    this.wallMillis = other.wallMillis;
    this.isCancelled = other.isCancelled;
  }

  public QueryReport deepCopy() {
    return new QueryReport(this);
  }

  @Override
  public void clear() {
    this.result = null;
    setWallMillisIsSet(false);
    this.wallMillis = 0;
    setIsCancelledIsSet(false);
    this.isCancelled = false;
  }

  public QueryResult getResult() {
    return this.result;
  }

  public QueryReport setResult(QueryResult result) {
    this.result = result;
    return this;
  }

  public void unsetResult() {
    this.result = null;
  }

  /** Returns true if field result is set (has been assigned a value) and false otherwise */
  public boolean isSetResult() {
    return this.result != null;
  }

  public void setResultIsSet(boolean value) {
    if (!value) {
      this.result = null;
    }
  }

  public long getWallMillis() {
    return this.wallMillis;
  }

  public QueryReport setWallMillis(long wallMillis) {
    this.wallMillis = wallMillis;
    setWallMillisIsSet(true);
    return this;
  }

  public void unsetWallMillis() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __WALLMILLIS_ISSET_ID);
  }

  /** Returns true if field wallMillis is set (has been assigned a value) and false otherwise */
  public boolean isSetWallMillis() {
    return EncodingUtils.testBit(__isset_bitfield, __WALLMILLIS_ISSET_ID);
  }

  public void setWallMillisIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __WALLMILLIS_ISSET_ID, value);
  }

  public boolean isIsCancelled() {
    return this.isCancelled;
  }

  public QueryReport setIsCancelled(boolean isCancelled) {
    this.isCancelled = isCancelled;
    setIsCancelledIsSet(true);
    return this;
  }

  public void unsetIsCancelled() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __ISCANCELLED_ISSET_ID);
  }

  /** Returns true if field isCancelled is set (has been assigned a value) and false otherwise */
  public boolean isSetIsCancelled() {
    return EncodingUtils.testBit(__isset_bitfield, __ISCANCELLED_ISSET_ID);
  }

  public void setIsCancelledIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __ISCANCELLED_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case RESULT:
      if (value == null) {
        unsetResult();
      } else {
        setResult((QueryResult)value);
      }
      break;

    case WALL_MILLIS:
      if (value == null) {
        unsetWallMillis();
      } else {
        setWallMillis((Long)value);
      }
      break;

    case IS_CANCELLED:
      if (value == null) {
        unsetIsCancelled();
      } else {
        setIsCancelled((Boolean)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case RESULT:
      return getResult();

    case WALL_MILLIS:
      return getWallMillis();

    case IS_CANCELLED:
      return isIsCancelled();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case RESULT:
      return isSetResult();
    case WALL_MILLIS:
      return isSetWallMillis();
    case IS_CANCELLED:
      return isSetIsCancelled();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof QueryReport)
      return this.equals((QueryReport)that);
    return false;
  }

  public boolean equals(QueryReport that) {
    if (that == null)
      return false;

    boolean this_present_result = true && this.isSetResult();
    boolean that_present_result = true && that.isSetResult();
    if (this_present_result || that_present_result) {
      if (!(this_present_result && that_present_result))
        return false;
      if (!this.result.equals(that.result))
        return false;
    }

    boolean this_present_wallMillis = true;
    boolean that_present_wallMillis = true;
    if (this_present_wallMillis || that_present_wallMillis) {
      if (!(this_present_wallMillis && that_present_wallMillis))
        return false;
      if (this.wallMillis != that.wallMillis)
        return false;
    }

    boolean this_present_isCancelled = true;
    boolean that_present_isCancelled = true;
    if (this_present_isCancelled || that_present_isCancelled) {
      if (!(this_present_isCancelled && that_present_isCancelled))
        return false;
      if (this.isCancelled != that.isCancelled)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_result = true && (isSetResult());
    list.add(present_result);
    if (present_result)
      list.add(result);

    boolean present_wallMillis = true;
    list.add(present_wallMillis);
    if (present_wallMillis)
      list.add(wallMillis);

    boolean present_isCancelled = true;
    list.add(present_isCancelled);
    if (present_isCancelled)
      list.add(isCancelled);

    return list.hashCode();
  }

  @Override
  public int compareTo(QueryReport other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetResult()).compareTo(other.isSetResult());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetResult()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.result, other.result);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetWallMillis()).compareTo(other.isSetWallMillis());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetWallMillis()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.wallMillis, other.wallMillis);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIsCancelled()).compareTo(other.isSetIsCancelled());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIsCancelled()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.isCancelled, other.isCancelled);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("QueryReport(");
    boolean first = true;

    sb.append("result:");
    if (this.result == null) {
      sb.append("null");
    } else {
      sb.append(this.result);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("wallMillis:");
    sb.append(this.wallMillis);
    first = false;
    if (!first) sb.append(", ");
    sb.append("isCancelled:");
    sb.append(this.isCancelled);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (result == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'result' was not present! Struct: " + toString());
    }
    // alas, we cannot check 'wallMillis' because it's a primitive and you chose the non-beans generator.
    // alas, we cannot check 'isCancelled' because it's a primitive and you chose the non-beans generator.
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class QueryReportStandardSchemeFactory implements SchemeFactory {
    public QueryReportStandardScheme getScheme() {
      return new QueryReportStandardScheme();
    }
  }

  private static class QueryReportStandardScheme extends StandardScheme<QueryReport> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, QueryReport struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // RESULT
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.result = new QueryResult();
              struct.result.read(iprot);
              struct.setResultIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // WALL_MILLIS
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.wallMillis = iprot.readI64();
              struct.setWallMillisIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // IS_CANCELLED
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.isCancelled = iprot.readBool();
              struct.setIsCancelledIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      if (!struct.isSetWallMillis()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'wallMillis' was not found in serialized data! Struct: " + toString());
      }
      if (!struct.isSetIsCancelled()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'isCancelled' was not found in serialized data! Struct: " + toString());
      }
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, QueryReport struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.result != null) {
        oprot.writeFieldBegin(RESULT_FIELD_DESC);
        struct.result.write(oprot);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(WALL_MILLIS_FIELD_DESC);
      oprot.writeI64(struct.wallMillis);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(IS_CANCELLED_FIELD_DESC);
      oprot.writeBool(struct.isCancelled);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class QueryReportTupleSchemeFactory implements SchemeFactory {
    public QueryReportTupleScheme getScheme() {
      return new QueryReportTupleScheme();
    }
  }

  private static class QueryReportTupleScheme extends TupleScheme<QueryReport> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, QueryReport struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      struct.result.write(oprot);
      oprot.writeI64(struct.wallMillis);
      oprot.writeBool(struct.isCancelled);
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, QueryReport struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.result = new QueryResult();
      struct.result.read(iprot);
      struct.setResultIsSet(true);
      struct.wallMillis = iprot.readI64();
      struct.setWallMillisIsSet(true);
      struct.isCancelled = iprot.readBool();
      struct.setIsCancelledIsSet(true);
    }
  }

}

