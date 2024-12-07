package org.example.exception;

public class UnrecognizedTokenException extends TokenizerException {

    public UnrecognizedTokenException() {
        super();
    }

    public UnrecognizedTokenException(String message) {
        super(message);
    }
}
