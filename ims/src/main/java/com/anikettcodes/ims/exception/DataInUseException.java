package com.anikettcodes.ims.exception;

public class DataInUseException extends RuntimeException {
    public DataInUseException(String message) {
        super(message);
    }
}
