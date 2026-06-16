package com.iskollect.dao;

import com.iskollect.exception.DatabaseException;
import com.iskollect.model.InOutLog;
import com.iskollect.model.InOutLog.EventType;
import com.iskollect.model.InOutLog.LogStatus;
import com.iskollect.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the inout_logs table.
 *
 * All queries use PreparedStatements to prevent SQL injection.
 * No business logic lives here — only CRUD and query operations.
 *
 * ── DDL (run once to create the table) ────────────────────────────────
 *
 *   CREATE TABLE inout_logs (
 *       log_id       INTEGER       GENERATED ALWAYS AS IDENTITY,
 *       user_id      INT           NOT NULL,
 *       event_type   VARCHAR(10)   NOT NULL,   -- 'INGRESS' | 'EGRESS'
 *       entry_method VARCHAR(20)   NOT NULL,   -- 'MANUAL'
 *       timestamp    TIMESTAMP     NOT NULL,
 *       staff_note   TEXT          NULL,
 *       status       VARCHAR(20)   NOT NULL,   -- 'VALID' | 'DUPLICATE' | 'UNRESOLVED'
 *       PRIMARY KEY (log_id)
 *       -- FK to users will be added by the Registration Module:
 *       -- CONSTRAINT fk_inout_user FOREIGN KEY (user_id)
 *       --     REFERENCES users(user_id) ON DELETE RESTRICT
 *   );
 *
 * ──────────────────────────────────────────────────────────────────────
 */
public class InOutLogDAO {

    // ── SQL statements ────────────────────────────────────────────────────

    private static final String SQL_INSERT =
        "INSERT INTO inout_logs (user_id, action, performed_at, notes) VALUES (?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
        "SELECT * FROM inout_logs WHERE log_id = ?";

    private static final String SQL_GET_BY_USER =
        "SELECT * FROM inout_logs WHERE user_id = ? ORDER BY performed_at DESC";

    private static final String SQL_GET_ALL =
        "SELECT * FROM inout_logs ORDER BY performed_at DESC";

    private static final String SQL_GET_BY_DATE_RANGE =
        "SELECT * FROM inout_logs WHERE user_id = ? AND performed_at::date BETWEEN ? AND ? " +
        "ORDER BY performed_at DESC";

    private static final String SQL_GET_LAST_EVENT =
        "SELECT * FROM inout_logs WHERE user_id = ? ORDER BY performed_at DESC LIMIT 1";

    private static final String SQL_GET_LAST_EVENT_OF_TYPE =
        "SELECT * FROM inout_logs WHERE user_id = ? AND action = ? " +
        "ORDER BY performed_at DESC LIMIT 1";

    private static final String SQL_GET_RECENT_SAME_EVENT =
        "SELECT * FROM inout_logs " +
        "WHERE user_id = ? AND action = ? " +
        "AND performed_at >= ? " +
        "ORDER BY performed_at DESC LIMIT 1";

    private static final String SQL_COUNT_BY_DATE =
        "SELECT COUNT(*) FROM inout_logs WHERE performed_at::date = ?";

    private static final String SQL_GET_BY_DATE =
        "SELECT * FROM inout_logs WHERE performed_at::date = ? ORDER BY performed_at DESC";

    // ── Connection helper ─────────────────────────────────────────────────

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Write operations ──────────────────────────────────────────────────

    /**
     * Persists a new InOutLog to the database.
     * Sets the auto-generated logId back onto the provided object.
     *
     * @param log the log to insert (logId will be populated after insert)
     * @throws DatabaseException if the INSERT fails
     */
    public void insert(InOutLog log) throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getUserId());
            ps.setString(2, log.getEventType().name());
            ps.setTimestamp(3, Timestamp.valueOf(log.getTimestamp()));
            ps.setString(4, log.getStaffNote());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setLogId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert InOutLog for user " + log.getUserId(), e);
        }
    }

    /**
     * Updates the status field of an existing log (e.g., UNRESOLVED → VALID
     * after a user is registered retroactively).
     *
     * @param logId  the log to update
     * @param status the new status
     * @throws DatabaseException if the UPDATE fails
     */
    public void updateStatus(int logId, LogStatus status) throws DatabaseException {
        // The Supabase inout_logs table has no status column.
    }

    // ── Read operations ───────────────────────────────────────────────────

    /**
     * Finds a single log by its primary key.
     *
     * @param logId the log_id to look up
     * @return the matching InOutLog, or null if not found
     * @throws DatabaseException on SQL error
     */
    public InOutLog findById(int logId) throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, logId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find log by ID " + logId, e);
        }
    }

    /**
     * Returns all logs for a given user, newest first.
     *
     * @param userId the user whose logs to retrieve
     * @return list of InOutLog (may be empty)
     * @throws DatabaseException on SQL error
     */
    public List<InOutLog> getByUserId(int userId) throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_GET_BY_USER)) {
            ps.setInt(1, userId);
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch logs for user " + userId, e);
        }
    }

    /**
     * Returns all logs across all users, newest first.
     * Used for staff overview / daily report.
     *
     * @return list of all InOutLog records
     * @throws DatabaseException on SQL error
     */
    public List<InOutLog> getAll() throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_GET_ALL)) {
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all logs.", e);
        }
    }

    /**
     * Returns logs for a user filtered to a date range (inclusive).
     *
     * @param userId target user
     * @param from      start date (inclusive)
     * @param to        end date (inclusive)
     * @return filtered list, newest first
     * @throws DatabaseException on SQL error
     */
    public List<InOutLog> getByUserAndDateRange(int userId, LocalDate from, LocalDate to)
            throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_GET_BY_DATE_RANGE)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch logs for user " + userId
                + " between " + from + " and " + to, e);
        }
    }

    /**
     * Returns the single most recent log for a user, regardless of type.
     * Used by InOutService to determine the user's current in/out state.
     *
     * @param userId target user
     * @return the most recent log, or null if no logs exist
     * @throws DatabaseException on SQL error
     */
    public InOutLog getLastEvent(int userId) throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_GET_LAST_EVENT)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get last event for user " + userId, e);
        }
    }

    /**
     * Returns the most recent log of a specific type (INGRESS or EGRESS) for a user.
     * Used for duplicate detection.
     *
     * @param userId target user
     * @param eventType the type to filter by
     * @return the most recent log of the given type, or null
     * @throws DatabaseException on SQL error
     */
    public InOutLog getLastEventOfType(int userId, EventType eventType) throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_GET_LAST_EVENT_OF_TYPE)) {
            ps.setInt(1, userId);
            ps.setString(2, eventType.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get last " + eventType + " for user " + userId, e);
        }
    }

    /**
     * Looks for an existing log of the same type within a time window.
     * Used by InOutService to enforce the duplicate-window check.
     *
     * @param userId   target user
     * @param eventType   type to check (INGRESS or EGRESS)
     * @param windowStart the earliest timestamp to consider (now - window)
     * @return the conflicting log if found, or null
     * @throws DatabaseException on SQL error
     */
    public InOutLog getRecentSameEvent(int userId, EventType eventType, LocalDateTime windowStart)
            throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_GET_RECENT_SAME_EVENT)) {
            ps.setInt(1, userId);
            ps.setString(2, eventType.name());
            ps.setTimestamp(3, Timestamp.valueOf(windowStart));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check for recent " + eventType
                + " for user " + userId, e);
        }
    }

    /**
     * Returns all logs recorded on a specific calendar date.
     * Used for the daily summary report.
     *
     * @param date the date to filter by
     * @return list of logs, newest first
     * @throws DatabaseException on SQL error
     */
    public List<InOutLog> getByDate(LocalDate date) throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_GET_BY_DATE)) {
            ps.setDate(1, Date.valueOf(date));
            return collectResults(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch logs for date " + date, e);
        }
    }

    /**
     * Returns the total number of log events recorded on a specific date.
     * Useful for lightweight dashboard counts.
     *
     * @param date the date to count
     * @return count of log records
     * @throws DatabaseException on SQL error
     */
    public int countByDate(LocalDate date) throws DatabaseException {
        try (PreparedStatement ps = conn().prepareStatement(SQL_COUNT_BY_DATE)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count logs for date " + date, e);
        }
    }

    // ── Mapping helper ────────────────────────────────────────────────────

    /**
     * Maps a single ResultSet row to an InOutLog object.
     */
    private InOutLog map(ResultSet rs) throws SQLException {
        return new InOutLog(
            rs.getInt("log_id"),
            rs.getInt("user_id"),
            EventType.valueOf(rs.getString("action")),
            rs.getTimestamp("performed_at").toLocalDateTime(),
            rs.getString("notes")
        );
    }

    /**
     * Executes a PreparedStatement and collects all rows into a List.
     */
    private List<InOutLog> collectResults(PreparedStatement ps) throws SQLException {
        List<InOutLog> logs = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(map(rs));
            }
        }
        return logs;
    }
}
