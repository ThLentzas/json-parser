package org.example.exception;

public class UTF8DecoderException extends RuntimeException {
    public UTF8DecoderException() {
        super();
    }

    public UTF8DecoderException(String message) {
        super(message);
    }
}
