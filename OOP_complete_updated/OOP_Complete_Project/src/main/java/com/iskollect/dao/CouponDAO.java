package com.iskollect.dao;

import com.iskollect.exception.DatabaseException;
import com.iskollect.model.Coupon;
import com.iskollect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CouponDAO {
    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public List<Coupon> getAll() throws DatabaseException {
        String sql = "SELECT * FROM coupons ORDER BY points_required ASC";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Coupon> coupons = new ArrayList<>();
            while (rs.next()) {
                coupons.add(map(rs));
            }
            return coupons;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch coupons.", e);
        }
    }

    public Coupon findById(int couponId) throws DatabaseException {
        String sql = "SELECT * FROM coupons WHERE coupon_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, couponId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find coupon " + couponId, e);
        }
    }

    public boolean insert(Coupon r) throws DatabaseException {
        String sql = "INSERT INTO coupons (coupon_name, points_required) VALUES (?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getName());
            ps.setDouble(2, r.getPointsRequired());
            boolean inserted = ps.executeUpdate() > 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setCouponId(keys.getInt(1));
                }
            }
            return inserted;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert coupon.", e);
        }
    }

    private Coupon map(ResultSet rs) throws SQLException {
        return new Coupon(
                rs.getInt("coupon_id"),
                rs.getString("coupon_name"),
                rs.getDouble("points_required")
        );
    }
}
