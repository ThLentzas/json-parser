package org.example.exception;

public class UnrecognizedTokenException extends RuntimeException {

    public UnrecognizedTokenException() {
    }

    public UnrecognizedTokenException(String message) {
        super(message);
    }
}
