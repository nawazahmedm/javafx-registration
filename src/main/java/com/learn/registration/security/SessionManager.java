package com.learn.registration.security;

/**
 * SESSION MANAGER — a simple application-level session store.
 *
 * Holds the currently logged-in user's details for the lifetime of the app.
 * All controllers read from here to know:
 *   - who is logged in (getUsername)
 *   - what they are allowed to do (getRole, isAdmin)
 *
 * Design: static singleton — there is only ever one user logged in at a time
 * in a desktop app, so a static holder is the simplest correct approach.
 *
 * Call SessionManager.login() after successful authentication.
 * Call SessionManager.logout() when the user logs out or the app exits.
 */
public class SessionManager {

    private static String  currentUsername = null;
    private static String  currentRole     = null;
    private static boolean loggedIn        = false;

    // Private constructor — no instances, only static methods
    private SessionManager() {}

    /**
     * Stores the authenticated user in the session.
     *
     * @param username  the username that was verified
     * @param role      "admin" or "user"
     */
    public static void login(String username, String role) {
        currentUsername = username;
        currentRole     = role;
        loggedIn        = true;
        System.out.println("[Session] Logged in: " + username + " (" + role + ")");
    }

    /**
     * Clears the session — must be called on logout so the next person
     * who opens the window cannot reuse a previous session.
     */
    public static void logout() {
        System.out.println("[Session] Logged out: " + currentUsername);
        currentUsername = null;
        currentRole     = null;
        loggedIn        = false;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static boolean isLoggedIn()      { return loggedIn; }
    public static String  getCurrentUsername() { return currentUsername; }
    public static String  getCurrentRole()  { return currentRole; }

    /**
     * Returns true if the logged-in user has the "admin" role.
     * Use this in view controllers to enable/disable privileged actions.
     */
    public static boolean isAdmin() {
        return "admin".equalsIgnoreCase(currentRole);
    }
}
