package org.example.core.exception;

public class IllegalControlCharacterException extends LexicalException {

    public IllegalControlCharacterException() {
        super();
    }

    public IllegalControlCharacterException(String message) {
        super(message);
    }
}
