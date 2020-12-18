package com.rnctech.nrdataservice.exception;

/**
 * shell/cmd execution related exception.
 * @contributor zilin
 * @since 2020.09
 */
public class ShellException extends RNBaseException {
	
  public ShellException() {
  }

  public ShellException(String message) {
    super(message);
  }

  public ShellException(String message, Throwable cause) {
    super(message, cause);
  }

  public ShellException(Throwable cause) {
    super(cause);
  }

  public ShellException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
