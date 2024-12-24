package org.example.exception;

public class SubsequenceIndexViolationException extends RuntimeException {
    public SubsequenceIndexViolationException() {
    }

    public SubsequenceIndexViolationException(String message) {
        super(message);
    }
}
