package com.iskollect.exception;

/**
 * Thrown by service-layer input validation before any DB call is made.
 */
public class InvalidInputException extends Exception {

    public InvalidInputException(String message) {
        super(message);
    }
}