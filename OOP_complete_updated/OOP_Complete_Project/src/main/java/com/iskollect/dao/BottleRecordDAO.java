package com.iskollect.dao;

import com.iskollect.exception.DatabaseException;
import com.iskollect.model.BottleRecord;
import com.iskollect.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BottleRecordDAO {
    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public boolean insert(BottleRecord t) throws DatabaseException {
        String sql = "INSERT INTO bottle_records "
                + "(user_id, bottles_collected, collection_date, week_start_date) "
                + "VALUES (?, ?, ?, DATE_TRUNC('week', ?::date)::date)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getUserId());
            ps.setInt(2, t.getBottles());
            ps.setDate(3, Date.valueOf(t.getDate()));
            ps.setDate(4, Date.valueOf(t.getDate()));
            boolean inserted = ps.executeUpdate() > 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setRecordId(keys.getInt(1));
                }
            }
            return inserted;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert bottleRecord.", e);
        }
    }

    public List<BottleRecord> getByUserId(int userId) throws DatabaseException {
        String sql = "SELECT br.record_id, br.user_id, br.bottles_collected, br.collection_date, "
                + "COALESCE(SUM(pl.points_change) FILTER (WHERE pl.points_change > 0), 0) AS points "
                + "FROM bottle_records br LEFT JOIN points_ledger pl "
                + "ON pl.user_id = br.user_id AND pl.ref_id = br.record_id "
                + "AND pl.source IN ('bottle', 'streak', 'badge') "
                + "WHERE br.user_id = ? "
                + "GROUP BY br.record_id, br.user_id, br.bottles_collected, br.collection_date "
                + "ORDER BY br.collection_date DESC, br.record_id DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return collect(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch bottleRecords for user " + userId, e);
        }
    }

    public List<BottleRecord> getByDateRange(int userId, LocalDate from, LocalDate to) throws DatabaseException {
        String sql = "SELECT br.record_id, br.user_id, br.bottles_collected, br.collection_date, "
                + "COALESCE(SUM(pl.points_change) FILTER (WHERE pl.points_change > 0), 0) AS points "
                + "FROM bottle_records br LEFT JOIN points_ledger pl "
                + "ON pl.user_id = br.user_id AND pl.ref_id = br.record_id "
                + "AND pl.source IN ('bottle', 'streak', 'badge') "
                + "WHERE br.user_id = ? AND br.collection_date BETWEEN ? AND ? "
                + "GROUP BY br.record_id, br.user_id, br.bottles_collected, br.collection_date "
                + "ORDER BY br.collection_date DESC, br.record_id DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            return collect(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch bottleRecords by date range.", e);
        }
    }

    public int getTotalBottles(int userId) throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(bottles_collected), 0) FROM bottle_records WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get total bottles.", e);
        }
    }

    public double getTotalPoints(int userId) throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(points_change), 0) FROM points_ledger WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get total bottleRecord points.", e);
        }
    }

    private List<BottleRecord> collect(PreparedStatement ps) throws SQLException {
        List<BottleRecord> bottleRecords = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bottleRecords.add(map(rs));
            }
        }
        return bottleRecords;
    }

    private BottleRecord map(ResultSet rs) throws SQLException {
        double points = rs.getDouble("points");
        return new BottleRecord(
                rs.getInt("record_id"),
                rs.getInt("user_id"),
                rs.getInt("bottles_collected"),
                points,
                0,
                0,
                points,
                rs.getDate("collection_date").toLocalDate()
        );
    }
}
