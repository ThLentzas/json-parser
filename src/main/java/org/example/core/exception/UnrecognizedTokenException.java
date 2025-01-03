package org.example.core.exception;

public class UnrecognizedTokenException extends LexicalException {

    public UnrecognizedTokenException() {
        super();
    }

    public UnrecognizedTokenException(String message) {
        super(message);
    }
}
