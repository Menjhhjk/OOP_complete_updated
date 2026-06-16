package com.iskollect.dao;

import com.iskollect.exception.DatabaseException;
import com.iskollect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PointsLedgerDAO {
    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public int insert(int userId, double pointsChange, String source, Integer refId) throws DatabaseException {
        String sql = "INSERT INTO points_ledger (user_id, points_change, source, ref_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDouble(2, pointsChange);
            ps.setString(3, source);
            if (refId == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, refId);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert points ledger entry.", e);
        }
    }

    public double getBalance(int userId) throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(points_change), 0) FROM points_ledger WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get points balance.", e);
        }
    }
}
