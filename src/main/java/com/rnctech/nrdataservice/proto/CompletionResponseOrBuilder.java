// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exec.proto

package com.rnctech.nrdataservice.proto;

public interface CompletionResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rn.CompletionResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>repeated string matches = 1;</code>
   * @return A list containing the matches.
   */
  java.util.List<java.lang.String>
      getMatchesList();
  /**
   * <code>repeated string matches = 1;</code>
   * @return The count of matches.
   */
  int getMatchesCount();
  /**
   * <code>repeated string matches = 1;</code>
   * @param index The index of the element to return.
   * @return The matches at the given index.
   */
  java.lang.String getMatches(int index);
  /**
   * <code>repeated string matches = 1;</code>
   * @param index The index of the value to return.
   * @return The bytes of the matches at the given index.
   */
  com.google.protobuf.ByteString
      getMatchesBytes(int index);
}
