package org.example.core.exception;

public class UnexpectedCharacterException extends LexicalException {

    public UnexpectedCharacterException() {
        super();
    }

    public UnexpectedCharacterException(String message) {
        super(message);
    }
}
