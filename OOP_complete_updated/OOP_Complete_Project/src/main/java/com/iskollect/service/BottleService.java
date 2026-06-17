package com.iskollect.service;

import com.iskollect.dao.StreakDAO;
import com.iskollect.dao.UserDAO;
import com.iskollect.dao.BottleRecordDAO;
import com.iskollect.dao.PointsLedgerDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.User;
import com.iskollect.model.SubmitResult;
import com.iskollect.model.BottleRecord;

import java.time.LocalDate;
import java.util.List;

public class BottleService {
    public static final int MAX_BOTTLES_PER_SUBMISSION = 1000;

    private final UserDAO         userDAO;
    private final BottleRecordDAO bottleRecordDAO;
    private final PointsLedgerDAO pointsLedgerDAO;
    private final PointsService   pointsService;
    private final StreakService   streakService;
    private final BadgeService    badgeService;

    public BottleService() {
        UserDAO         sharedUserDAO         = new UserDAO();
        BottleRecordDAO sharedBottleRecordDAO = new BottleRecordDAO();
        StreakDAO       sharedStreakDAO        = new StreakDAO();
        PointsLedgerDAO sharedLedgerDAO       = new PointsLedgerDAO();

        this.userDAO         = sharedUserDAO;
        this.bottleRecordDAO = sharedBottleRecordDAO;
        this.pointsLedgerDAO = sharedLedgerDAO;
        this.pointsService   = new PointsService(sharedUserDAO, sharedBottleRecordDAO);
        this.streakService   = new StreakService(sharedUserDAO, sharedStreakDAO, sharedLedgerDAO);
        this.badgeService    = new BadgeService(sharedUserDAO);
    }

    public SubmitResult submitBottles(int userId, int bottleCount) {
        if (bottleCount <= 0) {
            return SubmitResult.failure("Bottle count must be greater than zero.");
        }
        if (bottleCount > MAX_BOTTLES_PER_SUBMISSION) {
            return SubmitResult.failure("Number of bottles submitted exceeds the limit of "
                    + MAX_BOTTLES_PER_SUBMISSION + " per submission.");
        }
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                return SubmitResult.failure("User not found.");
            }

            System.out.println("DEBUG [BottleService] userId=" + userId
                    + " bottles=" + bottleCount
                    + " totalPoints_before=" + user.getTotalPoints()
                    + " weeklyBottles_before=" + user.getWeeklyBottles()
                    + " streak_before=" + user.getStreak());

            double basePoints = pointsService.calculateBasePoints(bottleCount);
            System.out.println("DEBUG [BottleService] basePoints=" + basePoints);
            int previousBottleTotal = user.getRawBottleCount();
            int newBottleTotal = previousBottleTotal + bottleCount;

            BottleRecord bottleRecord = new BottleRecord(
                    0, userId, bottleCount,
                    basePoints, 0, 0, 0,
                    LocalDate.now());
            bottleRecordDAO.insert(bottleRecord);
            System.out.println("DEBUG [BottleService] bottleRecord inserted, recordId=" + bottleRecord.getRecordId());

            double streakBonus = streakService.evaluateStreak(user, bottleCount);
            System.out.println("DEBUG [BottleService] streakBonus=" + streakBonus
                    + " streak_after=" + user.getStreak()
                    + " totalPoints_after_streak=" + user.getTotalPoints());

            BadgeService.BadgeResult badge = badgeService.evaluateBadgeForBottles(newBottleTotal);
            double badgeBonus = 0;
            if (!badgeService.awardReachedBadges(userId, previousBottleTotal, newBottleTotal).isEmpty()) {
                badgeBonus = badge.getBonusPoints();
            }
            System.out.println("DEBUG [BottleService] badge=" + badge.getTierName()
                    + " badgeBonus=" + badgeBonus);

            pointsLedgerDAO.insert(userId, basePoints, "bottle", bottleRecord.getRecordId());
            if (badgeBonus > 0) {
                pointsLedgerDAO.insert(userId, badgeBonus, "badge", bottleRecord.getRecordId());
            }

            double additionalPoints = basePoints + badgeBonus;
            double newTotal = user.getTotalPoints() + additionalPoints;
            userDAO.updatePoints(userId, newTotal);
            user.setTotalPoints(newTotal);
            System.out.println("DEBUG [BottleService] final totalPoints=" + newTotal);

            userDAO.updateWeeklyStats(userId,
                    user.getWeeklyBottles(),
                    user.getStreak(),
                    user.getLastSubmitDate());
            user.setRawBottleCount(newBottleTotal);

            double totalPoints = basePoints + streakBonus + badgeBonus;
            bottleRecord.setPoints(totalPoints);
            bottleRecord.setStreakBonus(streakBonus);
            bottleRecord.setBadgeBonus(badgeBonus);
            bottleRecord.setBasePoints(basePoints);

            System.out.println("DEBUG [BottleService] SubmitResult: base=" + basePoints
                    + " streak=" + streakBonus + " badge=" + badgeBonus + " total=" + totalPoints);

            return new SubmitResult(true, "Bottle submission recorded.",
                    basePoints, streakBonus, badgeBonus, totalPoints,
                    badge.getTierName(), user.getStreak());

        } catch (DatabaseException e) {
            System.err.println("DEBUG [BottleService] DatabaseException: " + e.getMessage());
            e.printStackTrace();
            return SubmitResult.failure("Database error: " + e.getMessage());
        }
    }

    public List<BottleRecord> getBottleHistory(int userId) {
        try {
            return bottleRecordDAO.getByUserId(userId);
        } catch (DatabaseException e) {
            return List.of();
        }
    }
}
