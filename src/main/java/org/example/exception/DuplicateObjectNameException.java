package org.example.exception;

public class DuplicateObjectNameException extends RuntimeException {

    public DuplicateObjectNameException() {
        super();
    }

    public DuplicateObjectNameException(String message) {
        super(message);
    }
}
