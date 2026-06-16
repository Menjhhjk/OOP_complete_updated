package com.iskollect.service;

import com.iskollect.dao.InOutLogDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.exception.InvalidInputException;
import com.iskollect.model.InOutLog;
import com.iskollect.model.InOutLog.EntryMethod;
import com.iskollect.model.InOutLog.EventType;
import com.iskollect.model.InOutLog.LogStatus;
import com.iskollect.model.User;
import com.iskollect.dao.UserDAO;
import com.iskollect.util.SessionManager;
import com.iskollect.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class AuthService {
    private static final String PUP_WEBMAIL_DOMAIN = "@iskolarngbayan.pup.edu.ph";
    private static final Pattern WEBMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{1,63}" + Pattern.quote(PUP_WEBMAIL_DOMAIN) + "$");

    private final UserDAO userDAO;
    private final InOutLogDAO inOutLogDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.inOutLogDAO = new InOutLogDAO();
    }

    public boolean register(User user)
            throws DatabaseException, InvalidInputException {

        //validation for inputs
        if (user.getUsername().trim().isEmpty() || user.getWebmail().trim().isEmpty() || user.getPassword().trim().isEmpty()) {
            throw new InvalidInputException("All fields are required. Please fill out the form entirely.");
        }

        String displayName = user.getUsername().trim();
        String webmail = user.getWebmail().trim();

        if (!isValidWebmail(webmail)) {
            throw new InvalidInputException("Please use a valid PUP webmail address.");
        }

        if (displayName.length() > 50) {
            throw new InvalidInputException("Display name must be 1-50 characters.");
        }

        String username = webmail.substring(0, webmail.indexOf('@')).toLowerCase();
        user.setName(displayName);
        user.setUsername(username);

        if (user.getPassword().trim().length() < 8) {
            throw new InvalidInputException("Password must be at least 8 characters long.");
        }

        String regex = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$";

        if (!user.getPassword().trim().matches(regex)) {
            throw new InvalidInputException("Password must include one number and one special character.");
        }

        //pass to userDAO to register the user to the DB
        return userDAO.registerUser(user);
    }

    public boolean login(String webmail, String plainPassword) throws DatabaseException, InvalidInputException {

        //validation for inputs
        if (webmail.isEmpty() || plainPassword.isEmpty()) {
            throw new InvalidInputException("All fields are required. Please fill out the form entirely.");
        }

        if (!isValidWebmail(webmail)) {
            throw new InvalidInputException("Please use a valid PUP webmail address.");
        }

        //checks if the inputted webmail exists
        User user = userDAO.searchUser(webmail);

        if (user == null) {
            System.out.println("[AuthService] Authentication failed: Webmail not found.");
            return false;
        }

        //compares the inputted password to the hashed password in the DB to check if the inputted password is correct
        boolean isPasswordCorrect = PasswordUtil.checkPassword(plainPassword, user.getPassword());

        if (isPasswordCorrect) {
            //begins the session of the user
            SessionManager.setSession(user);
            userDAO.updateSessionToken(user.getUserId(), user.getSessionToken());
            logActivity(user.getUserId(), EventType.LOGIN, "User logged in.");
            return true;
        }

        System.out.println("[AuthService] Authentication failed: Incorrect password.");
        return false;
    }

    public void logout() throws DatabaseException {
        User currentUser = SessionManager.getSession();
        //clears the session and current token
        if (currentUser != null) {
            userDAO.updateSessionToken(currentUser.getUserId(), null);
            logActivity(currentUser.getUserId(), EventType.LOGOUT, "User logged out.");
        }
        SessionManager.clearSession();
    }

    private boolean isValidWebmail(String webmail) {
        return webmail != null && WEBMAIL_PATTERN.matcher(webmail.trim()).matches();
    }

    private void logActivity(int userId, EventType eventType, String note) {
        try {
            InOutLog log = new InOutLog(
                    userId,
                    eventType,
                    EntryMethod.MANUAL,
                    LocalDateTime.now(),
                    note,
                    LogStatus.VALID
            );
            inOutLogDAO.insert(log);
        } catch (DatabaseException e) {
            System.err.println("[AuthService] Activity log failed: " + e.getMessage());
        }
    }
}
