package com.iskollect.dao;

import com.iskollect.exception.DatabaseException;
import com.iskollect.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;

public class StreakDAO {

    private Connection conn() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    public boolean streakExists(int userId) throws DatabaseException {
        String sql = "SELECT 1 FROM streaks WHERE user_id = ? LIMIT 1";
        try (Connection connection = conn();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking if streak exists: " + e.getMessage());
        }
    }

    public void logStreak(int userId, int streakDays, double bonusPoints) throws DatabaseException {
        String sql = "INSERT INTO streaks (user_id, streak_days, bonus_points, date_logged) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, streakDays);
            ps.setDouble(3, bonusPoints);
            ps.setDate(4, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to log streak bonus.", e);
        }
    }

    public int currentConsecutiveDaysBeforeToday(int userId, LocalDate today) throws DatabaseException {
        String sql = "SELECT DISTINCT collection_date FROM bottle_records "
                + "WHERE user_id = ? AND collection_date < ? ORDER BY collection_date DESC";
        LocalDate expected = today.minusDays(1);
        int count = 0;
        try (PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("collection_date").toLocalDate();
                    if (date.equals(expected)) {
                        count++;
                        expected = expected.minusDays(1);
                    } else if (date.isBefore(expected)) {
                        break;
                    }
                }
            }
            return count;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to calculate current streak.", e);
        }
    }

    public boolean hasStreakBonusLogged(int userId, int streakDays, LocalDate today) throws DatabaseException {
        String sql = "SELECT 1 FROM streaks WHERE user_id = ? AND streak_days = ? "
                + "AND date_logged::date = ? LIMIT 1";
        try (PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, streakDays);
            ps.setDate(3, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check streak bonus history.", e);
        }
    }

    public int currentConsecutiveDays(int userId, LocalDate today) throws DatabaseException {
        String sql = "SELECT DISTINCT collection_date FROM bottle_records "
                + "WHERE user_id = ? AND collection_date <= ? ORDER BY collection_date DESC";
        LocalDate expected = today;
        int count = 0;
        try (PreparedStatement ps = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("collection_date").toLocalDate();
                    if (date.equals(expected)) {
                        count++;
                        expected = expected.minusDays(1);
                    } else if (date.isBefore(expected)) {
                        break;
                    }
                }
            }
            return count;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to calculate current streak.", e);
        }
    }
}