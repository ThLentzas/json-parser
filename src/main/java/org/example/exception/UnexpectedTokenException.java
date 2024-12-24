package org.example.exception;

public class UnexpectedTokenException extends RuntimeException {
    public UnexpectedTokenException() {
    }

    public UnexpectedTokenException(String message) {
        super(message);
    }
}
