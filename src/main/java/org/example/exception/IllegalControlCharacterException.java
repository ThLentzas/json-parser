package org.example.exception;

public class IllegalControlCharacterException extends LexicalException {
    public IllegalControlCharacterException() {
    }

    public IllegalControlCharacterException(String message) {
        super(message);
    }
}
