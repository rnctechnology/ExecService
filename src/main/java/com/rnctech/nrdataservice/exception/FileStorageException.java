package com.rnctech.nrdataservice.exception;

public class FileStorageException extends RNBaseException {

	public FileStorageException() {
	}
	
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

	public FileStorageException(String message, Throwable cause, Object... msgArgs) {
		super(message, cause, msgArgs);
	}

	public FileStorageException(String message, Object... msgArgs) {
		super(message, msgArgs);
	}

	public FileStorageException(Throwable cause) {
		super(cause);
	}

}
