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
public class MetamodelParserDetails implements org.apache.thrift.TBase<MetamodelParserDetails, MetamodelParserDetails._Fields>, java.io.Serializable, Cloneable, Comparable<MetamodelParserDetails> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("MetamodelParserDetails");

  private static final org.apache.thrift.protocol.TField FILE_EXTENSIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("fileExtensions", org.apache.thrift.protocol.TType.SET, (short)1);
  private static final org.apache.thrift.protocol.TField IDENTIFIER_FIELD_DESC = new org.apache.thrift.protocol.TField("identifier", org.apache.thrift.protocol.TType.STRING, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new MetamodelParserDetailsStandardSchemeFactory());
    schemes.put(TupleScheme.class, new MetamodelParserDetailsTupleSchemeFactory());
  }

  public Set<String> fileExtensions; // required
  public String identifier; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    FILE_EXTENSIONS((short)1, "fileExtensions"),
    IDENTIFIER((short)2, "identifier");

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
        case 1: // FILE_EXTENSIONS
          return FILE_EXTENSIONS;
        case 2: // IDENTIFIER
          return IDENTIFIER;
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
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.FILE_EXTENSIONS, new org.apache.thrift.meta_data.FieldMetaData("fileExtensions", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.SetMetaData(org.apache.thrift.protocol.TType.SET, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.IDENTIFIER, new org.apache.thrift.meta_data.FieldMetaData("identifier", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(MetamodelParserDetails.class, metaDataMap);
  }

  public MetamodelParserDetails() {
  }

  public MetamodelParserDetails(
    Set<String> fileExtensions,
    String identifier)
  {
    this();
    this.fileExtensions = fileExtensions;
    this.identifier = identifier;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public MetamodelParserDetails(MetamodelParserDetails other) {
    if (other.isSetFileExtensions()) {
      Set<String> __this__fileExtensions = new HashSet<String>(other.fileExtensions);
      this.fileExtensions = __this__fileExtensions;
    }
    if (other.isSetIdentifier()) {
      this.identifier = other.identifier;
    }
  }

  public MetamodelParserDetails deepCopy() {
    return new MetamodelParserDetails(this);
  }

  @Override
  public void clear() {
    this.fileExtensions = null;
    this.identifier = null;
  }

  public int getFileExtensionsSize() {
    return (this.fileExtensions == null) ? 0 : this.fileExtensions.size();
  }

  public java.util.Iterator<String> getFileExtensionsIterator() {
    return (this.fileExtensions == null) ? null : this.fileExtensions.iterator();
  }

  public void addToFileExtensions(String elem) {
    if (this.fileExtensions == null) {
      this.fileExtensions = new HashSet<String>();
    }
    this.fileExtensions.add(elem);
  }

  public Set<String> getFileExtensions() {
    return this.fileExtensions;
  }

  public MetamodelParserDetails setFileExtensions(Set<String> fileExtensions) {
    this.fileExtensions = fileExtensions;
    return this;
  }

  public void unsetFileExtensions() {
    this.fileExtensions = null;
  }

  /** Returns true if field fileExtensions is set (has been assigned a value) and false otherwise */
  public boolean isSetFileExtensions() {
    return this.fileExtensions != null;
  }

  public void setFileExtensionsIsSet(boolean value) {
    if (!value) {
      this.fileExtensions = null;
    }
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public MetamodelParserDetails setIdentifier(String identifier) {
    this.identifier = identifier;
    return this;
  }

  public void unsetIdentifier() {
    this.identifier = null;
  }

  /** Returns true if field identifier is set (has been assigned a value) and false otherwise */
  public boolean isSetIdentifier() {
    return this.identifier != null;
  }

  public void setIdentifierIsSet(boolean value) {
    if (!value) {
      this.identifier = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case FILE_EXTENSIONS:
      if (value == null) {
        unsetFileExtensions();
      } else {
        setFileExtensions((Set<String>)value);
      }
      break;

    case IDENTIFIER:
      if (value == null) {
        unsetIdentifier();
      } else {
        setIdentifier((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case FILE_EXTENSIONS:
      return getFileExtensions();

    case IDENTIFIER:
      return getIdentifier();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case FILE_EXTENSIONS:
      return isSetFileExtensions();
    case IDENTIFIER:
      return isSetIdentifier();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof MetamodelParserDetails)
      return this.equals((MetamodelParserDetails)that);
    return false;
  }

  public boolean equals(MetamodelParserDetails that) {
    if (that == null)
      return false;

    boolean this_present_fileExtensions = true && this.isSetFileExtensions();
    boolean that_present_fileExtensions = true && that.isSetFileExtensions();
    if (this_present_fileExtensions || that_present_fileExtensions) {
      if (!(this_present_fileExtensions && that_present_fileExtensions))
        return false;
      if (!this.fileExtensions.equals(that.fileExtensions))
        return false;
    }

    boolean this_present_identifier = true && this.isSetIdentifier();
    boolean that_present_identifier = true && that.isSetIdentifier();
    if (this_present_identifier || that_present_identifier) {
      if (!(this_present_identifier && that_present_identifier))
        return false;
      if (!this.identifier.equals(that.identifier))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_fileExtensions = true && (isSetFileExtensions());
    list.add(present_fileExtensions);
    if (present_fileExtensions)
      list.add(fileExtensions);

    boolean present_identifier = true && (isSetIdentifier());
    list.add(present_identifier);
    if (present_identifier)
      list.add(identifier);

    return list.hashCode();
  }

  @Override
  public int compareTo(MetamodelParserDetails other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetFileExtensions()).compareTo(other.isSetFileExtensions());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFileExtensions()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.fileExtensions, other.fileExtensions);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetIdentifier()).compareTo(other.isSetIdentifier());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIdentifier()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.identifier, other.identifier);
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
    StringBuilder sb = new StringBuilder("MetamodelParserDetails(");
    boolean first = true;

    sb.append("fileExtensions:");
    if (this.fileExtensions == null) {
      sb.append("null");
    } else {
      sb.append(this.fileExtensions);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("identifier:");
    if (this.identifier == null) {
      sb.append("null");
    } else {
      sb.append(this.identifier);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (fileExtensions == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'fileExtensions' was not present! Struct: " + toString());
    }
    if (identifier == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'identifier' was not present! Struct: " + toString());
    }
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
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class MetamodelParserDetailsStandardSchemeFactory implements SchemeFactory {
    public MetamodelParserDetailsStandardScheme getScheme() {
      return new MetamodelParserDetailsStandardScheme();
    }
  }

  private static class MetamodelParserDetailsStandardScheme extends StandardScheme<MetamodelParserDetails> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, MetamodelParserDetails struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // FILE_EXTENSIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set0 = iprot.readSetBegin();
                struct.fileExtensions = new HashSet<String>(2*_set0.size);
                String _elem1;
                for (int _i2 = 0; _i2 < _set0.size; ++_i2)
                {
                  _elem1 = iprot.readString();
                  struct.fileExtensions.add(_elem1);
                }
                iprot.readSetEnd();
              }
              struct.setFileExtensionsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // IDENTIFIER
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.identifier = iprot.readString();
              struct.setIdentifierIsSet(true);
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
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, MetamodelParserDetails struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.fileExtensions != null) {
        oprot.writeFieldBegin(FILE_EXTENSIONS_FIELD_DESC);
        {
          oprot.writeSetBegin(new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, struct.fileExtensions.size()));
          for (String _iter3 : struct.fileExtensions)
          {
            oprot.writeString(_iter3);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.identifier != null) {
        oprot.writeFieldBegin(IDENTIFIER_FIELD_DESC);
        oprot.writeString(struct.identifier);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class MetamodelParserDetailsTupleSchemeFactory implements SchemeFactory {
    public MetamodelParserDetailsTupleScheme getScheme() {
      return new MetamodelParserDetailsTupleScheme();
    }
  }

  private static class MetamodelParserDetailsTupleScheme extends TupleScheme<MetamodelParserDetails> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, MetamodelParserDetails struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      {
        oprot.writeI32(struct.fileExtensions.size());
        for (String _iter4 : struct.fileExtensions)
        {
          oprot.writeString(_iter4);
        }
      }
      oprot.writeString(struct.identifier);
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, MetamodelParserDetails struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      {
        org.apache.thrift.protocol.TSet _set5 = new org.apache.thrift.protocol.TSet(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
        struct.fileExtensions = new HashSet<String>(2*_set5.size);
        String _elem6;
        for (int _i7 = 0; _i7 < _set5.size; ++_i7)
        {
          _elem6 = iprot.readString();
          struct.fileExtensions.add(_elem6);
        }
      }
      struct.setFileExtensionsIsSet(true);
      struct.identifier = iprot.readString();
      struct.setIdentifierIsSet(true);
    }
  }

}

