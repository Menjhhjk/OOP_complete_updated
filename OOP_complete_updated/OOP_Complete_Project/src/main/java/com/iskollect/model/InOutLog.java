package com.iskollect.model;

import java.time.LocalDateTime;

/**
 * Represents a single ingress or egress event logged by staff.
 *
 * NOTE: user_id is stored as a plain int with no FK enforcement yet.
 * The foreign key constraint to the users table will be added once
 * the User & Device Registration Module is complete.
 */
public class InOutLog {
    private int logId;
    private int userId;
    private EventType eventType;
    private LocalDateTime timestamp;
    private String staffNote;

    public enum EventType {
        INGRESS,
        EGRESS
    }

    public enum EntryMethod {
        MANUAL
    }

    public enum LogStatus {
        VALID,
        DUPLICATE,
        UNRESOLVED
    }

    public InOutLog() {
    }

    public InOutLog(int logId, int userId, EventType eventType,
                    LocalDateTime timestamp, String staffNote) {
        this.logId = logId;
        this.userId = userId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.staffNote = staffNote;
    }

    public InOutLog(int userId, EventType eventType, EntryMethod entryMethod,
                    LocalDateTime timestamp, String staffNote, LogStatus status) {
        this.userId = userId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.staffNote = staffNote;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public EntryMethod getEntryMethod() { return EntryMethod.MANUAL; }
    public void setEntryMethod(EntryMethod entryMethod) { }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getStaffNote() { return staffNote; }
    public void setStaffNote(String staffNote) { this.staffNote = staffNote; }
    public LogStatus getStatus() { return LogStatus.VALID; }
    public void setStatus(LogStatus status) { }

    @Override
    public String toString() {
        return String.format("InOutLog{logId=%d, userId=%d, action=%s, performedAt=%s}",
                logId, userId, eventType, timestamp);
    }
}
