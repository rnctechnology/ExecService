package com.rnctech.nrdataservice.exception;

/**
 * @contributor zilin
 * @since 2020.03
 */

public class AuthException extends RNBaseException {
	
  public AuthException() {
  }

  public AuthException(String message) {
    super(message);
  }

  public AuthException(String message, Throwable cause) {
    super(message, cause);
  }

  public AuthException(Throwable cause) {
    super(cause);
  }

  public AuthException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
