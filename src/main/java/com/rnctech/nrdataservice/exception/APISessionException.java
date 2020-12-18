package com.rnctech.nrdataservice.exception;

/*
 * client-server session related exception such as session not found / expired / dead
 * @contributor zilin 
 * @since 2020.10
 */

public class APISessionException extends APIException {
	  public APISessionException() {
	  }

	  public APISessionException(String message) {
	    super(message);
	  }

	  public APISessionException(String message, Throwable cause) {
	    super(message, cause);
	  }

	  public APISessionException(Throwable cause) {
	    super(cause);
	  }

	  public APISessionException(String message, Throwable cause, boolean enableSuppression,
	                              boolean writableStackTrace) {
	    super(message, cause, enableSuppression, writableStackTrace);
	  }
}
