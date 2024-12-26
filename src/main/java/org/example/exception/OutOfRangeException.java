package org.example.exception;

public class OutOfRangeException extends RuntimeException {

    public OutOfRangeException() {
    }

    public OutOfRangeException(String message) {
        super(message);
    }
}
