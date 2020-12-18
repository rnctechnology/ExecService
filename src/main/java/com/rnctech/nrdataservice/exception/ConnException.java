package com.rnctech.nrdataservice.exception;


public class ConnException extends RuntimeException {
    public ConnException() {
    }

    public ConnException(String message) {
        super(message);
    }

    public ConnException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnException(Throwable cause) {
        super(cause);
    }

    public ConnException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
