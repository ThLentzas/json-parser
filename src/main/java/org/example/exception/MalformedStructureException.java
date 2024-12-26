package org.example.exception;

public class MalformedStructureException extends RuntimeException {

    public MalformedStructureException() {
        super();
    }

    public MalformedStructureException(String message) {
        super(message);
    }
}
