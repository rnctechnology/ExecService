package com.rnctech.nrdataservice.exception;

/**
 * @contributor zilin
 * @since 2020.10
 * handle Livy specific exception. 
 */

public class APILivyException extends APIException {

	private static final long serialVersionUID = 3297635290091709928L;

	public APILivyException() {
		super("Current user not authenticated.");
	}

	public APILivyException(String message, Throwable cause, Object... msgArgs) {
		super(message, cause);
	}


	public APILivyException(String message, Object... msgArgs) {
		super(message);
	}


	public APILivyException(Throwable cause) {
		super(cause);
	}


}
