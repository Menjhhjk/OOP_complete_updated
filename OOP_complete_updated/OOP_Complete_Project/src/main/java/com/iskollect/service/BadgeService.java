package com.iskollect.service;

import com.iskollect.dao.UserDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.User;
import com.iskollect.util.DBConnection;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class BadgeService {
    private final UserDAO userDAO;

    public BadgeService() {
        this(new UserDAO());
    }

    public BadgeService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public BadgeResult evaluateBadge(int weeklyBottles) {
        if (weeklyBottles >= 31) {
            return new BadgeResult("Constellation", 10);
        }
        if (weeklyBottles >= 21) {
            return new BadgeResult("Gold", 5);
        }
        if (weeklyBottles >= 11) {
            return new BadgeResult("Emerald", 3);
        }
        if (weeklyBottles >= 6) {
            return new BadgeResult("Silver", 1);
        }
        return new BadgeResult("Bronze", 0);
    }

    public BadgeResult getCurrentBadge(int userId) {
        try {
            User user = userDAO.findById(userId);
            return user == null ? new BadgeResult("Bronze", 0) : evaluateBadge(user.getWeeklyBottles());
        } catch (DatabaseException e) {
            return new BadgeResult("Bronze", 0);
        }
    }

    public void resetWeeklyData(int userId) {
        try {
            userDAO.resetWeeklyStats(userId);
        } catch (DatabaseException e) {
            System.err.println("resetWeeklyData failed: " + e.getMessage());
        }
    }

    public boolean awardWeeklyBadge(int userId, BadgeResult badge) throws DatabaseException {
        String sql = "INSERT INTO user_badges (user_id, badge_id, date_awarded, week_start_date) "
                + "SELECT ?, badge_id, ?, DATE_TRUNC('week', ?::date)::date "
                + "FROM badges WHERE badge_name = ?";
        LocalDate today = LocalDate.now();
        try (PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(today));
            ps.setDate(3, Date.valueOf(today));
            ps.setString(4, badge.getTierName());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to award weekly badge.", e);
        }
    }

    public List<BadgeHistoryEntry> getBadgeHistory(int userId, int limit) {
        String sql = "SELECT b.badge_name, ub.date_awarded, ub.week_start_date, "
                + "COALESCE((SELECT SUM(br.bottles_collected) FROM bottle_records br "
                + "WHERE br.user_id = ub.user_id AND br.week_start_date = ub.week_start_date), 0) AS total_bottles "
                + "FROM user_badges ub JOIN badges b ON ub.badge_id = b.badge_id "
                + "WHERE ub.user_id = ? "
                + "ORDER BY ub.user_badge_id DESC "
                + "LIMIT ?";
        List<BadgeHistoryEntry> result = new java.util.ArrayList<>();
        try (java.sql.PreparedStatement ps =
                     DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new BadgeHistoryEntry(
                            rs.getString("badge_name"),
                            rs.getDate("date_awarded").toLocalDate(),
                            rs.getDate("week_start_date").toLocalDate(),
                            rs.getInt("total_bottles")
                    ));
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("getBadgeHistory failed: " + e.getMessage());
        }
        return result;
    }

    public List<BadgeHistoryEntry> getAllBadgeHistory(int userId) {
        String sql = "SELECT b.badge_name, ub.date_awarded, ub.week_start_date, "
                + "COALESCE((SELECT SUM(br.bottles_collected) FROM bottle_records br "
                + "WHERE br.user_id = ub.user_id AND br.week_start_date = ub.week_start_date), 0) AS total_bottles "
                + "FROM user_badges ub JOIN badges b ON ub.badge_id = b.badge_id "
                + "WHERE ub.user_id = ? "
                + "ORDER BY ub.user_badge_id DESC";
        List<BadgeHistoryEntry> result = new java.util.ArrayList<>();
        try (java.sql.PreparedStatement ps =
                     DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new BadgeHistoryEntry(
                            rs.getString("badge_name"),
                            rs.getDate("date_awarded").toLocalDate(),
                            rs.getDate("week_start_date").toLocalDate(),
                            rs.getInt("total_bottles")
                    ));
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("getAllBadgeHistory failed: " + e.getMessage());
        }
        return result;
    }

    public static final class BadgeHistoryEntry {
        private final String badgeName;
        private final LocalDate dateAwarded;
        private final LocalDate weekStartDate;
        private final int totalBottles;

        public BadgeHistoryEntry(String badgeName, LocalDate dateAwarded,
                                 LocalDate weekStartDate, int totalBottles) {
            this.badgeName     = badgeName;
            this.dateAwarded   = dateAwarded;
            this.weekStartDate = weekStartDate;
            this.totalBottles  = totalBottles;
        }

        public BadgeHistoryEntry(String badgeName, LocalDate dateAwarded) {
            this(badgeName, dateAwarded, dateAwarded, 0);
        }

        public String    getBadgeName()     { return badgeName; }
        public LocalDate getDateAwarded()   { return dateAwarded; }
        public LocalDate getWeekStartDate() { return weekStartDate; }
        public int       getTotalBottles()  { return totalBottles; }
    }

    public static final class BadgeResult {
        private final String tierName;
        private final double bonusPoints;

        public BadgeResult(String tierName, double bonusPoints) {
            this.tierName = tierName;
            this.bonusPoints = bonusPoints;
        }

        public String getTierName()    { return tierName; }
        public double getBonusPoints() { return bonusPoints; }
    }
}
