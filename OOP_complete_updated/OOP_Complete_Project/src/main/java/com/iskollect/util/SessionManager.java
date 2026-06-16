package com.iskollect.util;

import com.iskollect.dao.BottleRecordDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.User;
import com.iskollect.service.PointsService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class SessionManager {
    private static User loggedInUser;
    private static final PointsService pointsService = new PointsService();
    private static final BottleRecordDAO bottlerecordDAO = new BottleRecordDAO();
    private static final Set<Runnable> pointUpdateListeners =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static synchronized void setSession(User user) {
        if (user != null) {
            String token = java.util.UUID.randomUUID().toString();
            user.setSessionToken(token);
        }
        loggedInUser = user;
    }

    public static void refreshUserSession() {
        User user = getSession();
        if (user != null) {
            try {
                double latestPoints = pointsService.getTotalPoints(user.getUserId());
                int latestBottles = bottlerecordDAO.getTotalBottles(user.getUserId());
                user.setTotalPoints(latestPoints);
                user.setRawBottleCount(latestBottles);
            } catch (DatabaseException e) {
                System.err.println("Error refreshing data: " + e.getMessage());
            }
        }
    }

    public static synchronized void addPointUpdateListener(Runnable listener) {
        if (listener != null) {
            pointUpdateListeners.add(listener);
        }
    }

    public static void notifyPointUpdate() {
        refreshUserSession();
        List<Runnable> listeners;
        synchronized (SessionManager.class) {
            listeners = new ArrayList<>(pointUpdateListeners);
        }
        for (Runnable listener : listeners) {
            if (listener != null) {
                listener.run();
            }
        }
    }

    public static User getSession() { return loggedInUser; }

    public static synchronized void clearSession() {
        loggedInUser = null;
        pointUpdateListeners.clear();
    }
}
