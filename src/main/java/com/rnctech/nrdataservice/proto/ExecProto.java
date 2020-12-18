// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exec.proto

package com.rnctech.nrdataservice.proto;

public final class ExecProto {
  private ExecProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_ExecuteRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_ExecuteRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_ExecuteResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_ExecuteResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_CancelRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_CancelRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_CancelResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_CancelResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_CompletionRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_CompletionRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_CompletionResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_CompletionResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_StatusRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_StatusRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_StatusResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_StatusResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_StopRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_StopRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_rn_StopResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rn_StopResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\nexec.proto\022\002rn\"\036\n\016ExecuteRequest\022\014\n\004co" +
      "de\030\001 \001(\t\"b\n\017ExecuteResponse\022!\n\006status\030\001 " +
      "\001(\0162\021.rn.ExecuteStatus\022\034\n\004type\030\002 \001(\0162\016.r" +
      "n.OutputType\022\016\n\006output\030\003 \001(\t\"\017\n\rCancelRe" +
      "quest\"\020\n\016CancelResponse\"1\n\021CompletionReq" +
      "uest\022\014\n\004code\030\001 \001(\t\022\016\n\006cursor\030\002 \001(\005\"%\n\022Co" +
      "mpletionResponse\022\017\n\007matches\030\001 \003(\t\"\017\n\rSta" +
      "tusRequest\"3\n\016StatusResponse\022!\n\006status\030\001" +
      " \001(\0162\021.rn.IPythonStatus\"\r\n\013StopRequest\"\016" +
      "\n\014StopResponse*\'\n\rExecuteStatus\022\013\n\007SUCCE" +
      "SS\020\000\022\t\n\005ERROR\020\001**\n\rIPythonStatus\022\014\n\010STAR" +
      "TING\020\000\022\013\n\007RUNNING\020\001*!\n\nOutputType\022\010\n\004TEX" +
      "T\020\000\022\t\n\005IMAGE\020\0012\216\002\n\004Exec\0226\n\007execute\022\022.rn." +
      "ExecuteRequest\032\023.rn.ExecuteResponse\"\0000\001\022" +
      ";\n\010complete\022\025.rn.CompletionRequest\032\026.rn." +
      "CompletionResponse\"\000\0221\n\006cancel\022\021.rn.Canc" +
      "elRequest\032\022.rn.CancelResponse\"\000\0221\n\006statu" +
      "s\022\021.rn.StatusRequest\032\022.rn.StatusResponse" +
      "\"\000\022+\n\004stop\022\017.rn.StopRequest\032\020.rn.StopRes" +
      "ponse\"\000B5\n\037com.rnctech.nrdataservice.pro" +
      "toB\tExecProtoP\001\242\002\004Execb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_rn_ExecuteRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_rn_ExecuteRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_ExecuteRequest_descriptor,
        new java.lang.String[] { "Code", });
    internal_static_rn_ExecuteResponse_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_rn_ExecuteResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_ExecuteResponse_descriptor,
        new java.lang.String[] { "Status", "Type", "Output", });
    internal_static_rn_CancelRequest_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_rn_CancelRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_CancelRequest_descriptor,
        new java.lang.String[] { });
    internal_static_rn_CancelResponse_descriptor =
      getDescriptor().getMessageTypes().get(3);
    internal_static_rn_CancelResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_CancelResponse_descriptor,
        new java.lang.String[] { });
    internal_static_rn_CompletionRequest_descriptor =
      getDescriptor().getMessageTypes().get(4);
    internal_static_rn_CompletionRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_CompletionRequest_descriptor,
        new java.lang.String[] { "Code", "Cursor", });
    internal_static_rn_CompletionResponse_descriptor =
      getDescriptor().getMessageTypes().get(5);
    internal_static_rn_CompletionResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_CompletionResponse_descriptor,
        new java.lang.String[] { "Matches", });
    internal_static_rn_StatusRequest_descriptor =
      getDescriptor().getMessageTypes().get(6);
    internal_static_rn_StatusRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_StatusRequest_descriptor,
        new java.lang.String[] { });
    internal_static_rn_StatusResponse_descriptor =
      getDescriptor().getMessageTypes().get(7);
    internal_static_rn_StatusResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_StatusResponse_descriptor,
        new java.lang.String[] { "Status", });
    internal_static_rn_StopRequest_descriptor =
      getDescriptor().getMessageTypes().get(8);
    internal_static_rn_StopRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_StopRequest_descriptor,
        new java.lang.String[] { });
    internal_static_rn_StopResponse_descriptor =
      getDescriptor().getMessageTypes().get(9);
    internal_static_rn_StopResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rn_StopResponse_descriptor,
        new java.lang.String[] { });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
