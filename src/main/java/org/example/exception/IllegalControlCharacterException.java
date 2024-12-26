package org.example.exception;

public class IllegalControlCharacterException extends LexicalException {

    public IllegalControlCharacterException() {
        super();
    }

    public IllegalControlCharacterException(String message) {
        super(message);
    }
}
