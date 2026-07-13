package com.learn.registration.controller;

import com.learn.registration.database.DatabaseHelper;
import com.learn.registration.model.User;
import com.learn.registration.security.AuthController;
import com.learn.registration.security.SessionManager;

import java.util.List;

/**
 * CONTROLLER — the brain of the MVC pattern.
 *
 * Security additions:
 *   - registerUser() now accepts a plain-text password, hashes it via
 *     AuthController.hashPassword(), and stores only the hash.
 *   - Every write operation (insert / update / delete) writes an audit log
 *     entry via DatabaseHelper.logAudit(), recording who did what and when.
 *   - deleteUser() checks that the caller has the "admin" role before proceeding.
 */
public class UserController {

    private final DatabaseHelper db;
    private final AuthController auth;

    public UserController() {
        this.db   = DatabaseHelper.getInstance();
        this.auth = new AuthController();
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Validates, hashes the password, then saves a new user registration.
     *
     * @param user           User built from form input (passwordHash field ignored)
     * @param plainPassword  plain-text password from the PasswordField
     * @param confirmPassword repeated password for confirmation
     * @return               ValidationResult with success flag + message
     */
    public ValidationResult registerUser(User user,
                                         String plainPassword,
                                         String confirmPassword) {
        // ── Field validation ──────────────────────────────────────────────────
        if (isBlank(user.getFirstName()))
            return ValidationResult.fail("First name is required.");
        if (isBlank(user.getLastName()))
            return ValidationResult.fail("Last name is required.");
        if (isBlank(user.getEmail()))
            return ValidationResult.fail("Email address is required.");
        if (!isValidEmail(user.getEmail()))
            return ValidationResult.fail("Please enter a valid email address.");
        if (isBlank(user.getPhone()))
            return ValidationResult.fail("Phone number is required.");
        if (isBlank(user.getGender()))
            return ValidationResult.fail("Please select a gender.");
        if (isBlank(user.getCountry()))
            return ValidationResult.fail("Please select a country.");
        if (!user.isTermsAccepted())
            return ValidationResult.fail("You must accept the Terms & Conditions.");

        // ── Password validation ───────────────────────────────────────────────
        if (isBlank(plainPassword))
            return ValidationResult.fail("Password is required.");

        String pwError = auth.validatePasswordStrength(plainPassword);
        if (pwError != null)
            return ValidationResult.fail(pwError);

        if (!plainPassword.equals(confirmPassword))
            return ValidationResult.fail("Passwords do not match.");

        // ── Hash the password — plain text never reaches the DB ───────────────
        String hash = auth.hashPassword(plainPassword);
        user.setPasswordHash(hash);
        user.setRole("user"); // new registrations are always regular users

        // ── Persist ───────────────────────────────────────────────────────────
        boolean saved = db.insertUser(user);
        if (!saved)
            return ValidationResult.fail(
                    "Registration failed. The email address may already be in use.");

        // ── Audit log ─────────────────────────────────────────────────────────
        db.logAudit("INSERT", "users", 0,
                    "New user registered: " + user.getEmail(),
                    SessionManager.isLoggedIn()
                        ? SessionManager.getCurrentUsername()
                        : "self-registration");

        return ValidationResult.success(
                "Registration successful! Welcome, " + user.getFirstName() + ".");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        return db.getAllUsers();
    }

    public User getUserById(int id) {
        return db.getUserById(id);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Validates then updates an existing user's profile data.
     * Password is NOT changed here — add a dedicated change-password flow
     * if needed, so password updates can be audited separately.
     */
    public ValidationResult updateUser(User user) {
        if (user.getId() <= 0)
            return ValidationResult.fail("Invalid user id — cannot update.");
        if (isBlank(user.getFirstName()))
            return ValidationResult.fail("First name is required.");
        if (isBlank(user.getLastName()))
            return ValidationResult.fail("Last name is required.");
        if (isBlank(user.getEmail()))
            return ValidationResult.fail("Email address is required.");
        if (!isValidEmail(user.getEmail()))
            return ValidationResult.fail("Please enter a valid email address.");

        boolean updated = db.updateUser(user);
        if (!updated)
            return ValidationResult.fail("Update failed. Please try again.");

        // Audit log
        db.logAudit("UPDATE", "users", user.getId(),
                    "Updated user: " + user.getEmail(),
                    SessionManager.getCurrentUsername() != null
                        ? SessionManager.getCurrentUsername() : "unknown");

        return ValidationResult.success("User updated successfully.");
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a user by id.
     * Only admins are permitted to delete — enforced here in the controller,
     * and also enforced in the UI by disabling the button for non-admins.
     *
     * @param id  database primary key of the user to delete
     * @return    ValidationResult (fail if caller is not admin)
     */
    public ValidationResult deleteUser(int id) {
        // Role check — double-gated (UI disables the button, controller enforces it)
        if (!SessionManager.isAdmin()) {
            return ValidationResult.fail(
                    "Permission denied. Only admins can delete users.");
        }

        // Grab the user first so we can log their email in the audit trail
        User user = db.getUserById(id);
        String email = user != null ? user.getEmail() : "id=" + id;

        boolean deleted = db.deleteUser(id);
        if (!deleted)
            return ValidationResult.fail("Delete failed. Please try again.");

        // Audit log
        db.logAudit("DELETE", "users", id,
                    "Deleted user: " + email,
                    SessionManager.getCurrentUsername());

        return ValidationResult.success("User deleted.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        return email != null
            && email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    // =========================================================================
    // Inner class: ValidationResult
    // =========================================================================

    public static class ValidationResult {

        private final boolean success;
        private final String  message;

        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String  getMessage() { return message; }
    }
}
