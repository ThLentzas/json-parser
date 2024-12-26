package org.example.exception;

public class SubsequenceIndexViolationException extends RuntimeException {
    public SubsequenceIndexViolationException() {
        super();
    }

    public SubsequenceIndexViolationException(String message) {
        super(message);
    }
}
