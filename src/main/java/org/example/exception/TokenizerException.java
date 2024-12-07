package org.example.exception;

public class TokenizerException extends RuntimeException {
    public TokenizerException() {
        super();
    }

    public TokenizerException(String message) {
        super(message);
    }
}
