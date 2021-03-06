// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exec.proto

package com.rnctech.nrdataservice.proto;

/**
 * Protobuf enum {@code rn.OutputType}
 */
public enum OutputType
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>TEXT = 0;</code>
   */
  TEXT(0),
  /**
   * <code>IMAGE = 1;</code>
   */
  IMAGE(1),
  UNRECOGNIZED(-1),
  ;

  /**
   * <code>TEXT = 0;</code>
   */
  public static final int TEXT_VALUE = 0;
  /**
   * <code>IMAGE = 1;</code>
   */
  public static final int IMAGE_VALUE = 1;


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
  public static OutputType valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static OutputType forNumber(int value) {
    switch (value) {
      case 0: return TEXT;
      case 1: return IMAGE;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<OutputType>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      OutputType> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<OutputType>() {
          public OutputType findValueByNumber(int number) {
            return OutputType.forNumber(number);
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
    return com.rnctech.nrdataservice.proto.ExecProto.getDescriptor().getEnumTypes().get(2);
  }

  private static final OutputType[] VALUES = values();

  public static OutputType valueOf(
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

  private OutputType(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:rn.OutputType)
}

