package org.example.exception;

public class UnterminatedValueException extends LexicalException {

    public UnterminatedValueException() {
        super();
    }

    public UnterminatedValueException(String message) {
        super(message);
    }
}
