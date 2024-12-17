package org.example.exception;

public class UnrecognizedTokenException extends LexicalException {

    public UnrecognizedTokenException() {
        super();
    }

    public UnrecognizedTokenException(String message) {
        super(message);
    }
}
