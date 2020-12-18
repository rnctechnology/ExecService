package com.rnctech.nrdataservice.exception;

/**
 * resource read / write related exception
 * @contributor zilin
 * @since 2020.10
 */
public class ResourceException extends RNBaseException {
	
  public ResourceException() {
  }

  public ResourceException(String message) {
    super(message);
  }

  public ResourceException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceException(Throwable cause) {
    super(cause);
  }

  public ResourceException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
