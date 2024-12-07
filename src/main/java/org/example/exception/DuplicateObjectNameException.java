package org.example.exception;

public class DuplicateObjectNameException extends RuntimeException {

    public DuplicateObjectNameException() {
    }

    public DuplicateObjectNameException(String message) {
        super(message);
    }
}
