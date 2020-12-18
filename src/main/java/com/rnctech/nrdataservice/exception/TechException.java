package com.rnctech.nrdataservice.exception;

/**
 * base exception for run different technology such as java / python / sql.
 * @contributor zilin
 * @since 2020.10
 */
public class TechException extends RNBaseException {
	
  public TechException() {
  }

  public TechException(String message) {
    super(message);
  }

  public TechException(String message, Throwable cause) {
    super(message, cause);
  }

  public TechException(Throwable cause) {
    super(cause);
  }

  public TechException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
