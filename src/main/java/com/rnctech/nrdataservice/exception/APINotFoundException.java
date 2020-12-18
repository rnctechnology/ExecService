package com.rnctech.nrdataservice.exception;

/*
 * http api not exist exception
 * @contributor zilin 
 * @since 2020.10
 */

public class APINotFoundException extends APIException {
	
  public APINotFoundException() {
  }

  public APINotFoundException(String message) {
    super(message);
  }

  public APINotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public APINotFoundException(Throwable cause) {
    super(cause);
  }

  public APINotFoundException(String message, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
