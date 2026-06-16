package com.iskollect.model;

import java.time.LocalDate;

public class BottleRecord {
    private int recordId;
    private int userId;
    private int bottles;
    private double basePoints;
    private double streakBonus;
    private double badgeBonus;
    private double points;
    private LocalDate date;

    public BottleRecord() {
    }

    public BottleRecord(int recordId, int userId, int bottles, double basePoints,
                       double streakBonus, double badgeBonus, double points, LocalDate date) {
        this.recordId = recordId;
        this.userId = userId;
        this.bottles = bottles;
        this.basePoints = basePoints;
        this.streakBonus = streakBonus;
        this.badgeBonus = badgeBonus;
        this.points = points;
        this.date = date;
    }

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getBottles() { return bottles; }
    public void setBottles(int bottles) { this.bottles = bottles; }
    public double getBasePoints() { return basePoints; }
    public void setBasePoints(double basePoints) { this.basePoints = basePoints; }
    public double getStreakBonus() { return streakBonus; }
    public void setStreakBonus(double streakBonus) { this.streakBonus = streakBonus; }
    public double getBadgeBonus() { return badgeBonus; }
    public void setBadgeBonus(double badgeBonus) { this.badgeBonus = badgeBonus; }
    public double getPoints() { return points; }
    public void setPoints(double points) { this.points = points; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    @Override
    public String toString() {
        return "BottleRecord{recordId=" + recordId + ", userId=" + userId + ", bottles="
                + bottles + ", basePoints=" + basePoints + ", streakBonus=" + streakBonus
                + ", badgeBonus=" + badgeBonus + ", points=" + points + ", date=" + date + "}";
    }
}
