package com.openspec.router.adapter;

public class AdapterValidationException extends RuntimeException {
    public AdapterValidationException(String message) {
        super(message);
    }

    public AdapterValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
