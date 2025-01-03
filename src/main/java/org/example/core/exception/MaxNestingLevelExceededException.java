package org.example.core.exception;

public class MaxNestingLevelExceededException extends RuntimeException {

    public MaxNestingLevelExceededException() {
        super();
    }

    public MaxNestingLevelExceededException(String message) {
        super(message);
    }
}
