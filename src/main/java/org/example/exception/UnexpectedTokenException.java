package org.example.exception;

public class UnexpectedTokenException extends RuntimeException {
    public UnexpectedTokenException() {
        super();
    }

    public UnexpectedTokenException(String message) {
        super(message);
    }
}
