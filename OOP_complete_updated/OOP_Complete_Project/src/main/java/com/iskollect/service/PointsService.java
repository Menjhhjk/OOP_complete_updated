package com.iskollect.service;

import com.iskollect.dao.UserDAO;
import com.iskollect.dao.BottleRecordDAO;
import com.iskollect.dao.PointsLedgerDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.User;

public class PointsService {
    private final UserDAO userDAO;
    private final BottleRecordDAO bottleRecordDAO;
    private final PointsLedgerDAO pointsLedgerDAO;

    public PointsService() {
        this(new UserDAO(), new BottleRecordDAO());
    }

    public PointsService(UserDAO userDAO, BottleRecordDAO bottleRecordDAO) {
        this.userDAO = userDAO;
        this.bottleRecordDAO = bottleRecordDAO;
        this.pointsLedgerDAO = new PointsLedgerDAO();
    }

    public double calculateBasePoints(int bottles) {
        return bottles * 0.5;
    }

    public double getTotalPoints(int userId) {
        try {
            User user = userDAO.findById(userId);
            return user == null ? 0 : user.getTotalPoints();
        } catch (DatabaseException e) {
            return 0;
        }
    }

    public boolean deductPoints(int userId, double amount) {
        if (amount < 0) {
            return false;
        }
        try {
            return userDAO.deductPointsAtomic(userId, amount);
        } catch (DatabaseException e) {
            return false;
        }
    }

    public void recalculatePoints(int userId) {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                return;
            }
            double balance = pointsLedgerDAO.getBalance(userId);
            userDAO.updatePoints(userId, balance);
        } catch (DatabaseException e) {
            System.err.println("recalculatePoints failed: " + e.getMessage());
        }
    }
}
