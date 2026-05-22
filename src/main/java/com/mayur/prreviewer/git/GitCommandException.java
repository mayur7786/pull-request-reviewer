package com.mayur.prreviewer.git;

public class GitCommandException extends RuntimeException {

    // Creates a git error with a message only.
    public GitCommandException(String message) {
        super(message);
    }

    // Creates a git error with both message and cause.
    public GitCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
