package com.learn.registration.security;

import com.learn.registration.database.DatabaseHelper;
import com.learn.registration.database.DatabaseHelper.AdminUser;
import com.learn.registration.model.User;
import org.mindrot.jbcrypt.BCrypt;

/**
 * AUTH CONTROLLER — handles all authentication logic.
 *
 * Responsibilities:
 *   - Verify admin credentials (username + password) against admin_users table
 *   - Verify regular user credentials (email + password) against users table
 *   - Hash plain-text passwords before storage (used during registration)
 *   - Manage login / logout through SessionManager
 *
 * Security rules enforced here:
 *   1. Passwords are NEVER compared as plain text — always via BCrypt.checkpw()
 *   2. On failed login, a generic message is returned (no hint whether the
 *      username or password was wrong — prevents user enumeration attacks)
 *   3. BCrypt work factor is 12 — slow enough to resist brute-force
 */
public class AuthController {

    private final DatabaseHelper db;

    public AuthController() {
        this.db = DatabaseHelper.getInstance();
    }

    // ── Admin Login ───────────────────────────────────────────────────────────

    /**
     * Authenticates an admin user by username + password.
     *
     * Flow:
     *   1. Look up admin record by username
     *   2. Use BCrypt.checkpw() to verify the supplied password against the hash
     *   3. On success, store the session via SessionManager.login()
     *
     * @param username  plain-text username from the login form
     * @param password  plain-text password from the login form (char[] preferred
     *                  but String used here for FXML PasswordField compatibility)
     * @return          AuthResult with success flag and message
     */
    public AuthResult loginAdmin(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return AuthResult.fail("Username and password are required.");
        }

        AdminUser admin = db.getAdminByUsername(username.trim());

        // Generic message — don't tell attacker which field was wrong
        if (admin == null || !BCrypt.checkpw(password, admin.passwordHash)) {
            System.out.println("[Auth] Failed login attempt for username: " + username);
            return AuthResult.fail("Invalid username or password.");
        }

        SessionManager.login(admin.username, admin.role);
        db.logAudit("LOGIN", "admin_users", admin.id,
                    "Admin login: " + admin.username, admin.username);

        return AuthResult.success("Welcome, " + admin.username + "!");
    }

    /**
     * Authenticates a regular user by email + password.
     *
     * @param email     plain-text email from the login form
     * @param password  plain-text password from the login form
     * @return          AuthResult with success flag and message
     */
    public AuthResult loginUser(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            return AuthResult.fail("Email and password are required.");
        }

        User user = db.getUserByEmail(email.trim());

        if (user == null || isBlank(user.getPasswordHash())
                || !BCrypt.checkpw(password, user.getPasswordHash())) {
            System.out.println("[Auth] Failed login attempt for email: " + email);
            return AuthResult.fail("Invalid email or password.");
        }

        SessionManager.login(user.getEmail(), user.getRole());
        db.logAudit("LOGIN", "users", user.getId(),
                    "User login: " + user.getEmail(), user.getEmail());

        return AuthResult.success("Welcome, " + user.getFirstName() + "!");
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Logs out the current user and clears the session.
     */
    public void logout() {
        String username = SessionManager.getCurrentUsername();
        if (username != null) {
            db.logAudit("LOGOUT", "session", 0,
                        "Logout: " + username, username);
        }
        SessionManager.logout();
    }

    // ── Password Hashing ─────────────────────────────────────────────────────

    /**
     * Hashes a plain-text password using BCrypt with cost factor 12.
     * Call this in the registration flow before creating a User object.
     *
     * @param plainPassword  the password typed by the user
     * @return               a BCrypt hash safe to store in the database
     */
    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Validates password strength before hashing.
     *   - At least 8 characters
     *   - At least one digit
     *   - At least one uppercase letter
     *
     * @param password  plain-text password to validate
     * @return          null if valid, or an error message string
     */
    public String validatePasswordStrength(String password) {
        if (isBlank(password) || password.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one number.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        return null; // null = valid
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // =========================================================================
    // Inner class: AuthResult
    // =========================================================================

    /**
     * Returned by login methods — carries a success flag and a UI message.
     * Same pattern as UserController.ValidationResult for consistency.
     */
    public static class AuthResult {

        private final boolean success;
        private final String  message;

        private AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static AuthResult success(String message) {
            return new AuthResult(true, message);
        }

        public static AuthResult fail(String message) {
            return new AuthResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String  getMessage() { return message; }
    }
}
