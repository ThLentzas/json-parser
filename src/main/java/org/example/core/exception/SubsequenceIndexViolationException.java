package org.example.core.exception;

public class SubsequenceIndexViolationException extends RuntimeException {

    public SubsequenceIndexViolationException() {
        super();
    }

    public SubsequenceIndexViolationException(String message) {
        super(message);
    }
}
