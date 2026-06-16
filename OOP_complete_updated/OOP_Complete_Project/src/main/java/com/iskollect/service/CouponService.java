package com.iskollect.service;

import com.iskollect.dao.RedemptionDAO;
import com.iskollect.dao.CouponDAO;
import com.iskollect.dao.UserDAO;
import com.iskollect.dao.PointsLedgerDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.exception.InsufficientPointsException;
import com.iskollect.model.RedeemResult;
import com.iskollect.model.Redemption;
import com.iskollect.model.Coupon;
import com.iskollect.model.User;
import com.iskollect.util.CouponGenerator;
import com.iskollect.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CouponService {
    private final CouponDAO couponDAO = new CouponDAO();
    private final RedemptionDAO redemptionDAO = new RedemptionDAO();
    private final UserDAO userDAO = new UserDAO();
    private final PointsLedgerDAO pointsLedgerDAO = new PointsLedgerDAO();

    public List<Coupon> getAllCoupons() {
        try {
            return couponDAO.getAll();
        } catch (DatabaseException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public RedeemResult redeem(int userId, int couponId) {
        Connection conn = DBConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            User user = userDAO.findById(userId);
            Coupon coupon = couponDAO.findById(couponId);
            if (user == null || coupon == null) {
                throw new DatabaseException("User or coupon not found.");
            }
            if (user.getTotalPoints() < coupon.getPointsRequired()) {
                throw new InsufficientPointsException("Insufficient points for selected coupon.");
            }

            String couponCode = CouponGenerator.generate();
            Redemption redemption = new Redemption(0, userId, couponId, LocalDate.now(),
                    couponCode, false, coupon.getPointsRequired());
            redemptionDAO.insert(redemption);
            pointsLedgerDAO.insert(userId, -coupon.getPointsRequired(), "redemption", redemption.getRedemptionId());
            double remainingPoints = user.getTotalPoints() - coupon.getPointsRequired();
            userDAO.updatePoints(userId, remainingPoints);
            conn.commit();

            return new RedeemResult(true, "Coupon redemption.", couponCode, coupon.getName(),
                    coupon.getPointsRequired(), remainingPoints);
        } catch (InsufficientPointsException | DatabaseException | SQLException e) {
            rollback(conn);
            return RedeemResult.failure(e.getMessage());
        } finally {
            restoreAutoCommit(conn);
        }
    }

    public List<Redemption> getRedemptionHistory(int userId) {
        try {
            return redemptionDAO.getByUserId(userId);
        } catch (DatabaseException e) {
            return List.of();
        }
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
    }
}
