// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exec.proto

package com.rnctech.nrdataservice.proto;

/**
 * Protobuf enum {@code rn.IPythonStatus}
 */
public enum IPythonStatus
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>STARTING = 0;</code>
   */
  STARTING(0),
  /**
   * <code>RUNNING = 1;</code>
   */
  RUNNING(1),
  UNRECOGNIZED(-1),
  ;

  /**
   * <code>STARTING = 0;</code>
   */
  public static final int STARTING_VALUE = 0;
  /**
   * <code>RUNNING = 1;</code>
   */
  public static final int RUNNING_VALUE = 1;


  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static IPythonStatus valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static IPythonStatus forNumber(int value) {
    switch (value) {
      case 0: return STARTING;
      case 1: return RUNNING;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<IPythonStatus>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      IPythonStatus> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<IPythonStatus>() {
          public IPythonStatus findValueByNumber(int number) {
            return IPythonStatus.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return com.rnctech.nrdataservice.proto.ExecProto.getDescriptor().getEnumTypes().get(1);
  }

  private static final IPythonStatus[] VALUES = values();

  public static IPythonStatus valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private IPythonStatus(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:rn.IPythonStatus)
}
