package com.iskollect.scheduler;

import com.iskollect.dao.UserDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.service.BadgeService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeeklyResetScheduler {
    private LocalDate lastResetDate;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final UserDAO userDAO = new UserDAO();
    private final BadgeService badgeService = new BadgeService();

    public void start() {
        long initialDelay = 0;
        if (lastResetDate != null && ChronoUnit.DAYS.between(lastResetDate, LocalDate.now()) <= 7) {
            initialDelay = 7 - ChronoUnit.DAYS.between(lastResetDate, LocalDate.now());
        }
        scheduler.scheduleAtFixedRate(this::resetWeeklyData, initialDelay, 7, TimeUnit.DAYS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public void resetWeeklyData() {
        try {
            List<Integer> userIds = userDAO.getAllUserIds();
            for (Integer userId : userIds) {
                badgeService.resetWeeklyData(userId);
            }
            lastResetDate = LocalDate.now();
        } catch (DatabaseException e) {
            System.err.println("Weekly reset failed: " + e.getMessage());
        }
    }
}
