package org.example.exception;

public class UnterminatedValueException extends TokenizerException {

    public UnterminatedValueException() {
        super();
    }

    public UnterminatedValueException(String message) {
        super(message);
    }
}
