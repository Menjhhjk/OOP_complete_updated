package com.iskollect.service;

import com.iskollect.exception.InvalidInputException;
import com.iskollect.model.ReportResult;
import com.iskollect.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    public ReportResult getBottleSummary(int userId, LocalDate from, LocalDate to) {
        String type = "BOTTLE_SUMMARY";
        try {
            validateUserAndDates(userId, from, to);
            if (tooOld(from)) {
                return ReportResult.failure(type, "From date cannot be more than 2 years in the past.");
            }
            String sql = "SELECT br.user_id, COALESCE(SUM(br.bottles_collected), 0) AS total_bottles, "
                    + "COALESCE((SELECT SUM(pl.points_change) FROM points_ledger pl "
                    + "WHERE pl.user_id = br.user_id AND pl.created_at::date BETWEEN ? AND ?), 0) AS total_points "
                    + "FROM bottle_records br WHERE br.user_id = ? AND br.collection_date BETWEEN ? AND ? "
                    + "GROUP BY br.user_id";
            List<Map<String, Object>> rows = query(sql, from, to, userId, from, to);
            Map<String, Object> totals = rows.isEmpty() ? Map.of("total_bottles", 0, "total_points", 0) : rows.get(0);
            return ReportResult.success(type, rows, totals);
        } catch (InvalidInputException | SQLException e) {
            return ReportResult.failure(type, e.getMessage());
        }
    }

    public ReportResult getWeeklyLeaderboard() {
        String type = "WEEKLY_LEADERBOARD";
        try {
            String sql = "SELECT u.user_id, u.username AS name, "
                    + "COALESCE(SUM(br.bottles_collected), 0) AS weekly_bottles "
                    + "FROM users u LEFT JOIN bottle_records br ON u.user_id = br.user_id "
                    + "AND br.week_start_date = DATE_TRUNC('week', CURRENT_DATE)::date "
                    + "GROUP BY u.user_id, u.username ORDER BY weekly_bottles DESC, name ASC";
            return ReportResult.success(type, query(sql), Map.of());
        } catch (SQLException e) {
            return ReportResult.failure(type, e.getMessage());
        }
    }

    public ReportResult getPointsLedger(int userId, LocalDate from, LocalDate to) {
        String type = "POINTS_LEDGER";
        try {
            validateUserAndDates(userId, from, to);
            if (tooOld(from)) {
                return ReportResult.failure(type, "From date cannot be more than 2 years in the past.");
            }
            String sql = "SELECT source AS entry_type, ledger_id AS entry_id, created_at::date AS entry_date, "
                    + "points_change AS points_delta, ref_id FROM points_ledger "
                    + "WHERE user_id = ? AND created_at::date BETWEEN ? AND ? "
                    + "ORDER BY entry_date DESC, entry_id DESC";
            return ReportResult.success(type, query(sql, userId, from, to), Map.of());
        } catch (InvalidInputException | SQLException e) {
            return ReportResult.failure(type, e.getMessage());
        }
    }

    public ReportResult getRedemptionReport(Boolean fulfilledOnly) {
        String type = "REDEMPTION_REPORT";
        try {
            String sql = "SELECT rd.*, u.username AS user_name, c.coupon_name "
                    + "FROM redemptions rd JOIN users u ON rd.user_id = u.user_id "
                    + "JOIN coupons c ON rd.coupon_id = c.coupon_id "
                    + (fulfilledOnly == null ? "" : "WHERE rd.status = ? ")
                    + "ORDER BY rd.redemption_date DESC, rd.redemption_id DESC";
            List<Map<String, Object>> rows = fulfilledOnly == null ? query(sql)
                    : query(sql, fulfilledOnly ? "claimed" : "pending");
            return ReportResult.success(type, rows, Map.of("total_redemptions", rows.size()));
        } catch (SQLException e) {
            return ReportResult.failure(type, e.getMessage());
        }
    }

    public ReportResult getSystemSummary() {
        String type = "SYSTEM_SUMMARY";
        try {
            String sql = "SELECT "
                    + "(SELECT COALESCE(SUM(bottles_collected), 0) FROM bottle_records) AS total_bottles_collected, "
                    + "(SELECT COALESCE(SUM(points_change), 0) FROM points_ledger WHERE points_change > 0) AS total_points_issued, "
                    + "(SELECT COUNT(*) FROM redemptions) AS total_redemptions";
            List<Map<String, Object>> rows = query(sql);
            return ReportResult.success(type, rows, rows.isEmpty() ? Map.of() : rows.get(0));
        } catch (SQLException e) {
            return ReportResult.failure(type, e.getMessage());
        }
    }

    private void validateUserAndDates(int userId, LocalDate from, LocalDate to) throws InvalidInputException {
        if (userId <= 0) {
            throw new InvalidInputException("userId must be greater than zero.");
        }
        if (from == null || to == null) {
            throw new InvalidInputException("from and to dates are required.");
        }
        if (from.isAfter(to)) {
            throw new InvalidInputException("from date must not be after to date.");
        }
    }

    private boolean tooOld(LocalDate from) {
        return from.isBefore(LocalDate.now().minusYears(2));
    }

    private List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof LocalDate) {
                    ps.setDate(i + 1, Date.valueOf((LocalDate) param));
                } else if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else if (param instanceof Boolean) {
                    ps.setBoolean(i + 1, (Boolean) param);
                } else {
                    ps.setObject(i + 1, param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rows(rs);
            }
        }
    }

    private List<Map<String, Object>> rows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
