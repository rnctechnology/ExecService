package com.rnctech.nrdataservice.exception;

/**
 * api client/service related exception such as Livy http api / Jupyter http api.
 * @contributor zilin
 * @since 2020.10
 */
public class APIException extends RNBaseException {
	
  public APIException() {
  }

  public APIException(String message) {
    super(message);
  }

  public APIException(String message, Throwable cause) {
    super(message, cause);
  }

  public APIException(Throwable cause) {
    super(cause);
  }

  public APIException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
