package com.iskollect.dao;

import com.iskollect.exception.DatabaseException;
import com.iskollect.model.Redemption;
import com.iskollect.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RedemptionDAO {
    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public boolean insert(Redemption r) throws DatabaseException {
        String sql = "INSERT INTO redemptions "
                + "(user_id, coupon_id, redemption_date, coupon_code, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setInt(2, r.getCouponId());
            ps.setDate(3, Date.valueOf(r.getRedemptionDate()));
            ps.setString(4, r.getCouponCode());
            ps.setString(5, r.isFulfilled() ? "claimed" : "pending");
            boolean inserted = ps.executeUpdate() > 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setRedemptionId(keys.getInt(1));
                }
            }
            return inserted;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert redemption coupon.", e);
        }
    }

    public List<Redemption> getByUserId(int userId) throws DatabaseException {
        String sql = "SELECT rd.*, c.coupon_name, c.points_required AS points_deducted "
                + "FROM redemptions rd JOIN coupons c ON rd.coupon_id = c.coupon_id "
                + "WHERE rd.user_id = ? ORDER BY rd.redemption_date DESC, rd.redemption_id DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return collect(ps);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch redemptions for user " + userId, e);
        }
    }

    public void markFulfilled(int redemptionId) throws DatabaseException {
        String sql = "UPDATE redemptions SET status = 'claimed' WHERE redemption_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, redemptionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark redemption fulfilled.", e);
        }
    }

    public Redemption findByCouponCode(String code) throws DatabaseException {
        String sql = "SELECT rd.*, c.coupon_name, c.points_required AS points_deducted "
                + "FROM redemptions rd JOIN coupons c ON rd.coupon_id = c.coupon_id "
                + "WHERE rd.coupon_code = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find redemption by coupon code.", e);
        }
    }

    private List<Redemption> collect(PreparedStatement ps) throws SQLException {
        List<Redemption> redemptions = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                redemptions.add(map(rs));
            }
        }
        return redemptions;
    }

    private Redemption map(ResultSet rs) throws SQLException {
        Redemption coupon = new Redemption(
                rs.getInt("redemption_id"),
                rs.getInt("user_id"),
                rs.getInt("coupon_id"),
                rs.getDate("redemption_date").toLocalDate(),
                rs.getString("coupon_code"),
                "claimed".equalsIgnoreCase(rs.getString("status")),
                rs.getDouble("points_deducted")
        );
        try {
            coupon.setCouponName(rs.getString("coupon_name"));
        } catch (SQLException ignored) {
            coupon.setCouponName(null);
        }
        return coupon;
    }
}
