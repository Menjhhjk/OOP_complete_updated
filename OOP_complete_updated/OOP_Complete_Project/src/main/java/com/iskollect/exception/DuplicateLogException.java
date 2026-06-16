package com.iskollect.exception;

/**
 * Thrown by InOutService when a log attempt is rejected due to
 * a duplicate event within the configured duplicate-window.
 */
public class DuplicateLogException extends Exception {

    private final int existingLogId;

    public DuplicateLogException(int existingLogId, String message) {
        super(message);
        this.existingLogId = existingLogId;
    }

    /** The logId of the existing conflicting record. */
    public int getExistingLogId() {
        return existingLogId;
    }
}