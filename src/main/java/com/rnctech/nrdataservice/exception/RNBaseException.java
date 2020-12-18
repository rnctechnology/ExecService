package com.rnctech.nrdataservice.exception;

/**
 * @contributor zilin
 * @since 2020.10
 */

public class RNBaseException extends Exception {

	private static final long serialVersionUID = 5199681935290826055L;

	public RNBaseException() {
	}


	public RNBaseException(String message, Throwable cause, Object... msgArgs) {
		super(message, cause);
	}


	public RNBaseException(String message, Object... msgArgs) {
		super(message);
	}

	public RNBaseException(Throwable cause) {
		super(cause);
	}


}
