package com.iskollect.service;

import com.iskollect.dao.InOutLogDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.InOutLog;
import com.iskollect.model.InOutLog.EntryMethod;
import com.iskollect.model.InOutLog.EventType;
import com.iskollect.model.InOutLog.LogStatus;
import com.iskollect.model.LogResult;
import com.iskollect.util.UserValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic layer for Iskollect Ingress / Egress Monitoring.
 *
 * Responsibilities:
 *   1. Validate staff input (userId, eventType)
 *   2. Check user existence via UserValidator (stub until registration module)
 *   3. Enforce duplicate-window logic to prevent double-logging
 *   4. Determine the correct LogStatus for each event
 *   5. Persist the log via InOutLogDAO
 *   6. Return a LogResult value object to the calling controller
 *
 * This class is intentionally decoupled from the User & Device
 * Registration Module. See UserValidator for the swap-in point.
 */
public class InOutService {

    // ── Configuration ─────────────────────────────────────────────────────

    /**
     * Duplicate-window in minutes.
     *
     * If the same user logs the same event type (e.g., INGRESS) within
     * this many minutes of their last INGRESS, the new log is flagged as
     * DUPLICATE and the service returns LogResult.DUPLICATE instead of
     * inserting a second record.
     *
     * Adjust this value to match the physical gate setup:
     *   - 5 minutes is appropriate for a single manned entry point.
     */
    private static final int DUPLICATE_WINDOW_MINUTES = 5;

    // ── Dependencies ──────────────────────────────────────────────────────

    private final InOutLogDAO     logDAO;
    private final UserValidator userValidator;

    // ── Constructors ──────────────────────────────────────────────────────

    /** Default constructor — wires up real implementations. */
    public InOutService() {
        this.logDAO           = new InOutLogDAO();
        this.userValidator = new UserValidator();  // stub until registration module
    }

    /** Injection constructor — used for unit testing with mocks. */
    public InOutService(InOutLogDAO logDAO, UserValidator userValidator) {
        this.logDAO           = logDAO;
        this.userValidator = userValidator;
    }

    // ── Core: Log an event ────────────────────────────────────────────────

    /**
     * Records an ingress or egress event entered manually by staff.
     *
     * Processing order:
     *   1. Validate inputs
     *   2. Check user existence (stub: always passes)
     *   3. Check for duplicate within the window
     *   4. Build, persist, and return the log
     *
     * @param userId  the user ID typed by staff
     * @param eventType  INGRESS or EGRESS
     * @param staffNote  optional free-text note (may be null or blank)
     * @return LogResult describing outcome and carrying the persisted log
     */
    public LogResult logEvent(int userId, EventType eventType, String staffNote) {

        // ── Step 1: Input validation ───────────────────────────────────────
        if (userId <= 0) {
            return LogResult.invalidInput("User ID must be a positive integer.");
        }
        if (eventType == null) {
            return LogResult.invalidInput("Event type (INGRESS / EGRESS) must be specified.");
        }

        // ── Step 2: User existence check ───────────────────────────────
        // ┌─────────────────────────────────────────────────────────────────┐
        // │ STUB: userValidator.exists() always returns true.            │
        // │ Replace with real UserDAO lookup when registration is ready. │
        // └─────────────────────────────────────────────────────────────────┘
        if (!userValidator.exists(userId)) {
            // Log an UNRESOLVED entry so the event is not silently lost.
            // Staff can resolve it once the user registers.
            return persistUnresolved(userId, eventType, staffNote);
        }

        // ── Step 3: Duplicate-window check ────────────────────────────────
        try {
            LocalDateTime windowStart = LocalDateTime.now()
                    .minusMinutes(DUPLICATE_WINDOW_MINUTES);

            InOutLog recent = logDAO.getRecentSameEvent(userId, eventType, windowStart);

            if (recent != null) {
                return LogResult.duplicate(recent);
            }

        } catch (DatabaseException e) {
            return LogResult.dbError("Duplicate check failed: " + e.getMessage());
        }

        // ── Step 4: Build and persist the valid log ────────────────────────
        InOutLog log = new InOutLog(
            userId,
            eventType,
            EntryMethod.MANUAL,
            LocalDateTime.now(),
            normalizeNote(staffNote),
            LogStatus.VALID
        );

        try {
            logDAO.insert(log);   // logId is set on log by the DAO after insert
            return LogResult.success(log);
        } catch (DatabaseException e) {
            return LogResult.dbError("Could not persist log: " + e.getMessage());
        }
    }

    // ── Convenience wrappers ──────────────────────────────────────────────

    /**
     * Shorthand for logging an INGRESS event with no staff note.
     *
     * @param userId the user entering
     * @return LogResult
     */
    public LogResult logIngress(int userId) {
        return logEvent(userId, EventType.INGRESS, null);
    }

    /**
     * Shorthand for logging an EGRESS event with no staff note.
     *
     * @param userId the user exiting
     * @return LogResult
     */
    public LogResult logEgress(int userId) {
        return logEvent(userId, EventType.EGRESS, null);
    }

    // ── State queries ─────────────────────────────────────────────────────

    /**
     * Returns the most recent log for a user.
     * Used by the UI to show the user's current IN / OUT status.
     *
     * @param userId target user
     * @return the most recent InOutLog, or null if no logs exist
     */
    public InOutLog getCurrentStatus(int userId) {
        try {
            return logDAO.getLastEvent(userId);
        } catch (DatabaseException e) {
            System.err.println("getCurrentStatus failed for user " + userId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns true if the user's most recent log is an INGRESS.
     * Returns false if the last log is EGRESS, or if no logs exist.
     *
     * @param userId target user
     * @return true if user is currently considered inside the premises
     */
    public boolean isCurrentlyInside(int userId) {
        InOutLog last = getCurrentStatus(userId);
        return last != null && last.getEventType() == EventType.INGRESS;
    }

    // ── History queries ───────────────────────────────────────────────────

    /**
     * Returns the full log history for a user, newest first.
     *
     * @param userId target user
     * @return list of InOutLog (may be empty)
     */
    public List<InOutLog> getUserHistory(int userId) {
        try {
            return logDAO.getByUserId(userId);
        } catch (DatabaseException e) {
            System.err.println("getUserHistory failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns logs for a user filtered to a date range.
     *
     * @param userId target user
     * @param from      start date inclusive
     * @param to        end date inclusive
     * @return filtered list of InOutLog
     */
    public List<InOutLog> getUserHistoryByDateRange(int userId, LocalDate from, LocalDate to) {
        try {
            return logDAO.getByUserAndDateRange(userId, from, to);
        } catch (DatabaseException e) {
            System.err.println("getUserHistoryByDateRange failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns all logs recorded today.
     * Used for the staff daily monitoring dashboard.
     *
     * @return all logs for today, newest first
     */
    public List<InOutLog> getTodayLogs() {
        try {
            return logDAO.getByDate(LocalDate.now());
        } catch (DatabaseException e) {
            System.err.println("getTodayLogs failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns the total number of events logged today.
     * Used for dashboard counters.
     *
     * @return count of today's log entries
     */
    public int getTodayCount() {
        try {
            return logDAO.countByDate(LocalDate.now());
        } catch (DatabaseException e) {
            System.err.println("getTodayCount failed: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Returns all logs in the system, newest first.
     * Used by staff for full audit review.
     *
     * @return all InOutLog records
     */
    public List<InOutLog> getAllLogs() {
        try {
            return logDAO.getAll();
        } catch (DatabaseException e) {
            System.err.println("getAllLogs failed: " + e.getMessage());
            return List.of();
        }
    }

    // ── UNRESOLVED handling ───────────────────────────────────────────────

    /**
     * Logs the event with status UNRESOLVED when the user is not found.
     *
     * This preserves the physical event in the audit trail and allows
     * retroactive resolution once the user registers.
     *
     * Called when userValidator.exists() returns false.
     * Currently unreachable (stub always returns true), but wired in
     * so no events are silently dropped once real validation is active.
     *
     * @param userId  the unrecognised user ID
     * @param eventType  INGRESS or EGRESS
     * @param staffNote  optional note
     * @return LogResult with outcome USER_NOT_FOUND
     */
    private LogResult persistUnresolved(int userId, EventType eventType, String staffNote) {
        InOutLog log = new InOutLog(
            userId,
            eventType,
            EntryMethod.MANUAL,
            LocalDateTime.now(),
            normalizeNote(staffNote),
            LogStatus.UNRESOLVED
        );
        try {
            logDAO.insert(log);
        } catch (DatabaseException e) {
            System.err.println("Failed to persist UNRESOLVED log: " + e.getMessage());
        }
        return LogResult.userNotFound(userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Trims a staff note and converts blank strings to null.
     * Ensures the DB stores NULL rather than empty strings.
     *
     * @param note raw note input
     * @return trimmed note, or null if blank
     */
    private String normalizeNote(String note) {
        if (note == null) return null;
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
