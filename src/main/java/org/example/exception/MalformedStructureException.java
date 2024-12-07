package org.example.exception;

public class MalformedStructureException extends RuntimeException {
    public MalformedStructureException() {
    }

    public MalformedStructureException(String message) {
        super(message);
    }
}
