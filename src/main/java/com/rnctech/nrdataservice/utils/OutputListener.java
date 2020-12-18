package com.rnctech.nrdataservice.utils;

/**
 * @author zilin chen
 * @since 2020.09
 */
public interface OutputListener {

  void onUpdateAll(RNCOutputStream out);

  void onAppend(int index, MessageOutputStream out, byte[] line);

  void onUpdate(int index, MessageOutputStream out);
}
