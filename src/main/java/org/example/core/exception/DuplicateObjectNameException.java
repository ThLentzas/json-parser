package org.example.core.exception;

public class DuplicateObjectNameException extends RuntimeException {

    public DuplicateObjectNameException() {
        super();
    }

    public DuplicateObjectNameException(String message) {
        super(message);
    }
}
