package com.rnctech.nrdataservice.exception;

/**
 * @contributor zilin
 * @since 2020.10
 * MR related exception such as MDAC not accessible / property not exist / job not there etc. 
 */

public class APIMRException extends APIException {

	private static final long serialVersionUID = 3297635290091709928L;

	public APIMRException() {
		super("Current user not authenticated.");
	}

	public APIMRException(String message, Throwable cause, Object... msgArgs) {
		super(message, cause);
	}


	public APIMRException(String message, Object... msgArgs) {
		super(message);
	}


	public APIMRException(Throwable cause) {
		super(cause);
	}


}
