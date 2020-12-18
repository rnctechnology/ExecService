package com.rnctech.nrdataservice.utils;

/**
 * @author zilin chen
 * @since 2020.09
 */

public interface MessageListener {

  void onAppend(MessageOutputStream out, byte[] line);

  void onUpdate(MessageOutputStream out);
}
