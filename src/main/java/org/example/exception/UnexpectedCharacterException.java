package org.example.exception;

public class UnexpectedCharacterException extends RuntimeException {

    public UnexpectedCharacterException() {
    }

    public UnexpectedCharacterException(String message) {
        super(message);
    }
}
