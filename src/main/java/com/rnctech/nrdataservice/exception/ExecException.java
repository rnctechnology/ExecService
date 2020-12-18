package com.rnctech.nrdataservice.exception;

/**
 * @contributor zilin
 * @since 2020.10
 */
public class ExecException extends RNBaseException {
	
  public ExecException() {
  }

  public ExecException(String message) {
    super(message);
  }

  public ExecException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExecException(Throwable cause) {
    super(cause);
  }

  public ExecException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
