// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exec.proto

package com.rnctech.nrdataservice.proto;

public interface ExecuteResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rn.ExecuteResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.rn.ExecuteStatus status = 1;</code>
   * @return The enum numeric value on the wire for status.
   */
  int getStatusValue();
  /**
   * <code>.rn.ExecuteStatus status = 1;</code>
   * @return The status.
   */
  com.rnctech.nrdataservice.proto.ExecuteStatus getStatus();

  /**
   * <code>.rn.OutputType type = 2;</code>
   * @return The enum numeric value on the wire for type.
   */
  int getTypeValue();
  /**
   * <code>.rn.OutputType type = 2;</code>
   * @return The type.
   */
  com.rnctech.nrdataservice.proto.OutputType getType();

  /**
   * <code>string output = 3;</code>
   * @return The output.
   */
  java.lang.String getOutput();
  /**
   * <code>string output = 3;</code>
   * @return The bytes for output.
   */
  com.google.protobuf.ByteString
      getOutputBytes();
}