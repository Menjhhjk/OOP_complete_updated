package com.iskollect.model;

public final class SubmitResult {
    private final boolean success;
    private final String message;
    private final double basePoints;
    private final double streakBonus;
    private final double badgeBonus;
    private final double totalPoints;
    private final String newBadgeTier;
    private final int currentStreak;

    public SubmitResult(boolean success, String message, double basePoints, double streakBonus,
                        double badgeBonus, double totalPoints, String newBadgeTier, int currentStreak) {
        this.success = success;
        this.message = message;
        this.basePoints = basePoints;
        this.streakBonus = streakBonus;
        this.badgeBonus = badgeBonus;
        this.totalPoints = totalPoints;
        this.newBadgeTier = newBadgeTier;
        this.currentStreak = currentStreak;
    }

    public static SubmitResult failure(String message) {
        return new SubmitResult(false, message, 0, 0, 0, 0, null, 0);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public double getBasePoints() { return basePoints; }
    public double getStreakBonus() { return streakBonus; }
    public double getBadgeBonus() { return badgeBonus; }
    public double getTotalPoints() { return totalPoints; }
    public String getNewBadgeTier() { return newBadgeTier; }
    public int getCurrentStreak() { return currentStreak; }
}
