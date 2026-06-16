package com.iskollect.model;

import java.time.LocalDateTime;

/**
 * Value object returned by InOutService to the controller after every
 * log attempt. Carries enough information for the UI to display
 * a meaningful confirmation or error message without exposing raw exceptions.
 */
public class LogResult {

    public enum Outcome {
        SUCCESS,       // Event recorded normally
        DUPLICATE,     // Same event type already logged within the duplicate window
        USER_NOT_FOUND, // user_id has no matching record (stub: always bypassed until registration module)
        INVALID_INPUT, // userId <= 0 or eventType is null
        DB_ERROR       // Unexpected database failure
    }

    private final Outcome outcome;
    private final InOutLog log;        // The persisted log (null if outcome != SUCCESS / DUPLICATE)
    private final String message;      // Human-readable summary for the UI
    private final LocalDateTime at;    // Timestamp of this result

    // ── Constructors ──────────────────────────────────────────────────────

    public LogResult(Outcome outcome, InOutLog log, String message) {
        this.outcome = outcome;
        this.log     = log;
        this.message = message;
        this.at      = LocalDateTime.now();
    }

    // ── Static factories ──────────────────────────────────────────────────

    public static LogResult success(InOutLog log) {
        return new LogResult(
            Outcome.SUCCESS,
            log,
            String.format("Logged: User %d — %s at %s",
                log.getUserId(), log.getEventType(), log.getTimestamp())
        );
    }

    public static LogResult duplicate(InOutLog existing) {
        return new LogResult(
            Outcome.DUPLICATE,
            existing,
            String.format("Duplicate: User %d already has an active %s log (ID %d).",
                existing.getUserId(), existing.getEventType(), existing.getLogId())
        );
    }

    public static LogResult userNotFound(int userId) {
        return new LogResult(
            Outcome.USER_NOT_FOUND,
            null,
            // ── STUB NOTE ────────────────────────────────────────────────
            // This outcome is never returned in the current build.
            // UserValidator.exists() always returns true until the
            // User & Device Registration Module is wired in.
            // ─────────────────────────────────────────────────────────────
            "User ID " + userId + " not found. Registration module not active."
        );
    }

    public static LogResult invalidInput(String reason) {
        return new LogResult(Outcome.INVALID_INPUT, null, "Invalid input: " + reason);
    }

    public static LogResult dbError(String detail) {
        return new LogResult(Outcome.DB_ERROR, null, "Database error: " + detail);
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public Outcome getOutcome()    { return outcome; }
    public InOutLog getLog()       { return log; }
    public String getMessage()     { return message; }
    public LocalDateTime getAt()   { return at; }

    public boolean isSuccess()     { return outcome == Outcome.SUCCESS; }
}
