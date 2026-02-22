package org.example.versioncontrolserver.exception;

public class PushRejectedException extends RuntimeException {
    public PushRejectedException(String message) {
        super(message);
    }
}
