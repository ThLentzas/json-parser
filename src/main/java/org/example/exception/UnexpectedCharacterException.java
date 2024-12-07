package org.example.exception;

public class UnexpectedCharacterException extends TokenizerException {

    public UnexpectedCharacterException() {
        super();
    }

    public UnexpectedCharacterException(String message) {
        super(message);
    }
}
