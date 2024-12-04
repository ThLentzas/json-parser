package org.example.exception;

public class UnterminatedValueException extends RuntimeException {

    public UnterminatedValueException() {
    }

    public UnterminatedValueException(String message) {
        super(message);
    }
}
