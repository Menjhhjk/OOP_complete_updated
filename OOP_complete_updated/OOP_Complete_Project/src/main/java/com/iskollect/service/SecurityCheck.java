package com.iskollect.service;

import com.iskollect.exception.DatabaseException;
import com.iskollect.model.User;
import com.iskollect.dao.UserDAO;
import com.iskollect.util.SessionManager;

import java.time.Duration;
import java.time.LocalDateTime;

public class SecurityCheck {

    private static final int MAX_INACTIVITY_MINUTES = 30;

    private final UserDAO userDAO = new UserDAO();

    public boolean isSessionValid() {
        User currentUser = SessionManager.getSession();

        if (currentUser == null) {
            System.out.println("[SecurityService] Blocked: No active local session found.");
            return false;
        }

        int userId = currentUser.getUserId();

        if (isSessionExpired(currentUser.getLastActivity())) {
            System.out.println("[SecurityService] Blocked: Inactivity idle timeout detected.");
            handleForcedLogout(userId);
            return false;
        }

        String localToken = currentUser.getSessionToken();
        String dbToken = userDAO.getSessionTokenDB(userId);

        if (dbToken == null || !dbToken.equals(localToken)) {
            System.out.println("[SecurityService] Blocked: Token symmetry mismatch or remote session revoked.");
            handleForcedLogout(userId);
            return false;
        }

        try {
            currentUser.setLastActivity(LocalDateTime.now());
            userDAO.updateLastActivity(userId);
            return true;
        } catch (DatabaseException e) {
            System.err.println("[SecurityCheck] Error updating activity: " + e.getMessage());
            return false;
        }
    }

    private boolean isSessionExpired(LocalDateTime lastActivity) {
        if (lastActivity == null) {
            return false;
        }
        long minutesIdle = Duration.between(lastActivity, LocalDateTime.now()).toMinutes();
        return minutesIdle >= MAX_INACTIVITY_MINUTES;
    }

    private void handleForcedLogout(int userId) {
        try {
            userDAO.updateSessionToken(userId, null);
            System.out.println("[SecurityCheck] Remote session token revoked successfully.");
        } catch (DatabaseException e) {
            System.err.println("[SecurityCheck] Warning: Failed to revoke remote token. " + e.getMessage());
        } finally {
            SessionManager.clearSession();
            System.out.println("[SecurityCheck] Local session memory cleared safely.");
        }
    }
}
