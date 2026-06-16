package com.iskollect.service;

import com.iskollect.dao.PointsLedgerDAO;
import com.iskollect.dao.StreakDAO;
import com.iskollect.dao.UserDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.User;
import java.time.LocalDate;

public class StreakService {
    private final UserDAO         userDAO;
    private final StreakDAO        streakDAO;
    private final PointsLedgerDAO  pointsLedgerDAO;

    public StreakService() {
        this(new UserDAO(), new StreakDAO(), new PointsLedgerDAO());
    }

    public StreakService(UserDAO userDAO, StreakDAO streakDAO, PointsLedgerDAO pointsLedgerDAO) {
        this.userDAO         = userDAO;
        this.streakDAO       = streakDAO;
        this.pointsLedgerDAO = pointsLedgerDAO;
    }

    public double evaluateStreak(User user, int bottles) throws DatabaseException {
        LocalDate today = LocalDate.now();
        LocalDate lastSubmit = user.getLastSubmitDate();

        int newStreak = 1;
        if (lastSubmit != null && lastSubmit.equals(today)) {
            newStreak = user.getStreak();
        } else if (lastSubmit != null && lastSubmit.equals(today.minusDays(1))) {
            newStreak = user.getStreak() + 1;
        } else {
            newStreak = 1;
        }

        System.out.println("DEBUG [StreakService] userId=" + user.getUserId() + " newStreak=" + newStreak);

        if (!streakDAO.streakExists(user.getUserId())) {
            streakDAO.logStreak(user.getUserId(), newStreak, 0.0);
        }

        double basePoints = bottles * 0.5;
        double bonus = 0;
        if (newStreak >= 5) {
            bonus = basePoints * 1.0;
        } else if (newStreak >= 3) {
            bonus = basePoints * 0.5;
        }

        user.setStreak(newStreak);
        user.setWeeklyBottles(user.getWeeklyBottles() + bottles);
        user.setLastSubmitDate(today);

        userDAO.updateWeeklyStats(user.getUserId(), user.getWeeklyBottles(), newStreak, today);

        if (bonus > 0) {
            streakDAO.logStreak(user.getUserId(), newStreak, bonus);
            pointsLedgerDAO.insert(user.getUserId(), bonus, "streak", null);
            double newTotal = user.getTotalPoints() + bonus;
            userDAO.updatePoints(user.getUserId(), newTotal);
            user.setTotalPoints(newTotal);
        }

        return bonus;
    }

    public int getStreakCount(int userId) {
        try {
            User user = userDAO.findById(userId);
            return user == null ? 0 : user.getStreak();
        } catch (DatabaseException e) {
            return 0;
        }
    }
}