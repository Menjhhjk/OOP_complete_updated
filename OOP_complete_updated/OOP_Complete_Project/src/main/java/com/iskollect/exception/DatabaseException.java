package com.iskollect.exception;

/**
 * Wraps a raw SQLException into an application-level checked exception.
 * DAOs throw this; Services catch it and return a LogResult.DB_ERROR.
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }
}