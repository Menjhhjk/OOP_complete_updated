package com.iskollect.util;

/**
 * STUB — User & Device Registration Module not yet implemented
 *
 * This class stands in for real user existence validation.
 * Currently, exists() always returns true so that the Ingress /
 * Egress monitoring logic can run and be tested independently.
 *
 * WHEN the User & Device Registration Module is ready:
 *  1. Inject or instantiate a real UserDAO here.
 *  2. Replace the stub body with:
 *      return userDAO.findById(userId) != null;
 *  3. Remove this comment block.
 */
public class UserValidator {

    /**
     * Returns true if the given userId corresponds to a registered user.
     *
     * STUB: unconditionally returns true.
     * Replace with a real UserDAO lookup when the registration module is active.
     *
     * @param userId the user ID entered by staff
     * @return true if the user exists; false otherwise
     */
    public boolean exists(int userId) {
        // ── STUB ──────────────────────────────────────────────────────────
        // TODO: replace with → return userDAO.findById(userId) != null;
        // ─────────────────────────────────────────────────────────────────
        return true;
    }
}