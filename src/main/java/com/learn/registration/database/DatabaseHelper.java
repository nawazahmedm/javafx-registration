package com.learn.registration.database;

import com.learn.registration.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DATABASE LAYER — handles all SQLite operations.
 *
 * Tables managed:
 *   users       — registered user accounts (with password_hash + role)
 *   admin_users — separate admin credential store (email + bcrypt hash + role)
 *   audit_log   — immutable record of every insert / update / delete action
 *
 * Security notes:
 *   - Passwords are NEVER stored here as plain text.
 *     BCrypt hashing is done in AuthController before calling insertUser().
 *   - All queries use PreparedStatement — no string concatenation in SQL.
 *   - audit_log rows are INSERT-only; nothing in this class ever deletes them.
 */
public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:users.db";

    // ── DDL ───────────────────────────────────────────────────────────────────

    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                first_name      TEXT    NOT NULL,
                last_name       TEXT    NOT NULL,
                email           TEXT    NOT NULL UNIQUE,
                phone           TEXT,
                gender          TEXT,
                country         TEXT,
                bio             TEXT,
                newsletter      INTEGER NOT NULL DEFAULT 0,
                terms_accepted  INTEGER NOT NULL DEFAULT 0,
                hobbies         TEXT,
                registered_at   TEXT    NOT NULL,
                password_hash   TEXT    NOT NULL,
                role            TEXT    NOT NULL DEFAULT 'user'
            );
            """;

    /**
     * Separate table for app administrators.
     * Admins log in here; regular users are stored in the users table.
     * On first launch a default admin (admin / admin123) is seeded automatically.
     */
    private static final String CREATE_ADMIN_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS admin_users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT    NOT NULL UNIQUE,
                password_hash TEXT    NOT NULL,
                role          TEXT    NOT NULL DEFAULT 'admin',
                created_at    TEXT    NOT NULL
            );
            """;

    /**
     * Immutable audit trail — one row per create / update / delete event.
     * performed_by stores the username of whoever was logged in.
     */
    private static final String CREATE_AUDIT_LOG_TABLE = """
            CREATE TABLE IF NOT EXISTS audit_log (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                action       TEXT NOT NULL,
                target_table TEXT NOT NULL,
                target_id    INTEGER,
                description  TEXT,
                performed_by TEXT NOT NULL,
                performed_at TEXT NOT NULL
            );
            """;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static DatabaseHelper instance;

    private DatabaseHelper() {
        initDatabase();
    }

    public static DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(CREATE_USERS_TABLE);
            stmt.execute(CREATE_ADMIN_USERS_TABLE);
            stmt.execute(CREATE_AUDIT_LOG_TABLE);

            // Add password_hash / role columns to existing DB if upgrading
            ensureColumn(conn, "users", "password_hash", "TEXT NOT NULL DEFAULT ''");
            ensureColumn(conn, "users", "role",          "TEXT NOT NULL DEFAULT 'user'");

            seedDefaultAdmin(conn);

            System.out.println("[DB] Database initialised. All tables ready.");

        } catch (SQLException e) {
            System.err.println("[DB] Initialisation failed: " + e.getMessage());
            throw new RuntimeException("Could not initialise the database.", e);
        }
    }

    /**
     * Adds a column to a table only if it doesn't already exist.
     * Needed when the app is upgraded on a database that was created before
     * the security columns were introduced.
     */
    private void ensureColumn(Connection conn, String table,
                               String column, String definition) {
        try {
            ResultSet rs = conn.getMetaData().getColumns(null, null, table, column);
            if (!rs.next()) {
                conn.createStatement()
                    .execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                System.out.println("[DB] Added column " + column + " to " + table);
            }
        } catch (SQLException e) {
            System.err.println("[DB] ensureColumn failed for " + column + ": " + e.getMessage());
        }
    }

    /**
     * Creates the default admin account on first run.
     * Credentials:  username=admin  password=admin123
     * The password is BCrypt-hashed before storage.
     *
     * IMPORTANT: Change the default password after first login in production.
     */
    private void seedDefaultAdmin(Connection conn) throws SQLException {
        String check = "SELECT COUNT(*) FROM admin_users WHERE username = 'admin'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Hash "admin123" with BCrypt — cost factor 12
                String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
                        "admin123", org.mindrot.jbcrypt.BCrypt.gensalt(12));
                String insert = "INSERT INTO admin_users (username, password_hash, role, created_at) "
                              + "VALUES (?, ?, 'admin', ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, "admin");
                    ps.setString(2, hash);
                    ps.setString(3, now());
                    ps.executeUpdate();
                    System.out.println("[DB] Default admin seeded. username=admin password=admin123");
                }
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ── USERS : CREATE ────────────────────────────────────────────────────────

    /**
     * Inserts a new user. The passwordHash field must already be a BCrypt hash
     * — plain-text passwords must NEVER reach this method.
     */
    public boolean insertUser(User user) {
        String sql = """
                INSERT INTO users
                    (first_name, last_name, email, phone, gender, country,
                     bio, newsletter, terms_accepted, hobbies, registered_at,
                     password_hash, role)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1,  user.getFirstName());
            ps.setString(2,  user.getLastName());
            ps.setString(3,  user.getEmail());
            ps.setString(4,  user.getPhone());
            ps.setString(5,  user.getGender());
            ps.setString(6,  user.getCountry());
            ps.setString(7,  user.getBio());
            ps.setInt(8,     user.isNewsletter()    ? 1 : 0);
            ps.setInt(9,     user.isTermsAccepted() ? 1 : 0);
            ps.setString(10, user.getHobbies());
            ps.setString(11, now());
            ps.setString(12, user.getPasswordHash());
            ps.setString(13, user.getRole() != null ? user.getRole() : "user");

            int rows = ps.executeUpdate();
            System.out.println("[DB] insertUser → " + rows + " row(s).");
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DB] insertUser failed: " + e.getMessage());
            return false;
        }
    }

    // ── USERS : READ ALL ──────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users ORDER BY id DESC";
        List<User> users = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) users.add(mapUserRow(rs));

        } catch (SQLException e) {
            System.err.println("[DB] getAllUsers failed: " + e.getMessage());
        }

        return users;
    }

    // ── USERS : READ ONE ──────────────────────────────────────────────────────

    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUserRow(rs);

        } catch (SQLException e) {
            System.err.println("[DB] getUserById failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Looks up a user by email address — used during login to verify credentials.
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUserRow(rs);

        } catch (SQLException e) {
            System.err.println("[DB] getUserByEmail failed: " + e.getMessage());
        }
        return null;
    }

    // ── USERS : UPDATE ────────────────────────────────────────────────────────

    public boolean updateUser(User user) {
        String sql = """
                UPDATE users SET
                    first_name     = ?,
                    last_name      = ?,
                    email          = ?,
                    phone          = ?,
                    gender         = ?,
                    country        = ?,
                    bio            = ?,
                    newsletter     = ?,
                    terms_accepted = ?,
                    hobbies        = ?,
                    role           = ?
                WHERE id = ?
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1,  user.getFirstName());
            ps.setString(2,  user.getLastName());
            ps.setString(3,  user.getEmail());
            ps.setString(4,  user.getPhone());
            ps.setString(5,  user.getGender());
            ps.setString(6,  user.getCountry());
            ps.setString(7,  user.getBio());
            ps.setInt(8,     user.isNewsletter()    ? 1 : 0);
            ps.setInt(9,     user.isTermsAccepted() ? 1 : 0);
            ps.setString(10, user.getHobbies());
            ps.setString(11, user.getRole() != null ? user.getRole() : "user");
            ps.setInt(12,    user.getId());

            int rows = ps.executeUpdate();
            System.out.println("[DB] updateUser id=" + user.getId() + " → " + rows + " row(s).");
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DB] updateUser failed: " + e.getMessage());
            return false;
        }
    }

    // ── USERS : DELETE ────────────────────────────────────────────────────────

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println("[DB] deleteUser id=" + id + " → " + rows + " row(s).");
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DB] deleteUser failed: " + e.getMessage());
            return false;
        }
    }

    // ── ADMIN USERS ───────────────────────────────────────────────────────────

    /**
     * Fetches an admin record by username.
     * Returns null if not found — caller must handle the null case.
     */
    public AdminUser getAdminByUsername(String username) {
        String sql = "SELECT * FROM admin_users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new AdminUser(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("role")
                );
            }

        } catch (SQLException e) {
            System.err.println("[DB] getAdminByUsername failed: " + e.getMessage());
        }
        return null;
    }

    // ── AUDIT LOG ─────────────────────────────────────────────────────────────

    /**
     * Writes one audit entry. Call this after every INSERT / UPDATE / DELETE.
     *
     * @param action       "INSERT" | "UPDATE" | "DELETE"
     * @param targetTable  table affected, e.g. "users"
     * @param targetId     primary key of the affected row
     * @param description  human-readable summary, e.g. "Registered john@example.com"
     * @param performedBy  username of the logged-in user who triggered the action
     */
    public void logAudit(String action, String targetTable, int targetId,
                         String description, String performedBy) {
        String sql = """
                INSERT INTO audit_log
                    (action, target_table, target_id, description, performed_by, performed_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, action);
            ps.setString(2, targetTable);
            ps.setInt(3,    targetId);
            ps.setString(4, description);
            ps.setString(5, performedBy);
            ps.setString(6, now());
            ps.executeUpdate();

        } catch (SQLException e) {
            // Audit failure is logged to console but must not crash the app
            System.err.println("[DB] logAudit failed: " + e.getMessage());
        }
    }

    /**
     * Returns the full audit log ordered by most-recent first.
     * Useful for an admin "audit trail" screen (future feature).
     */
    public List<AuditEntry> getAuditLog() {
        String sql = "SELECT * FROM audit_log ORDER BY id DESC";
        List<AuditEntry> entries = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                entries.add(new AuditEntry(
                    rs.getInt("id"),
                    rs.getString("action"),
                    rs.getString("target_table"),
                    rs.getInt("target_id"),
                    rs.getString("description"),
                    rs.getString("performed_by"),
                    rs.getString("performed_at")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[DB] getAuditLog failed: " + e.getMessage());
        }

        return entries;
    }

    // ── Row mappers ───────────────────────────────────────────────────────────

    private User mapUserRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("gender"),
            rs.getString("country"),
            rs.getString("bio"),
            rs.getInt("newsletter")     == 1,
            rs.getInt("terms_accepted") == 1,
            rs.getString("hobbies"),
            rs.getString("registered_at"),
            rs.getString("password_hash"),
            rs.getString("role")
        );
    }

    private String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // =========================================================================
    // Inner value objects — kept here so the database layer is self-contained
    // =========================================================================

    /** Lightweight representation of an admin_users row. */
    public static class AdminUser {
        public final int    id;
        public final String username;
        public final String passwordHash;
        public final String role;

        public AdminUser(int id, String username, String passwordHash, String role) {
            this.id           = id;
            this.username     = username;
            this.passwordHash = passwordHash;
            this.role         = role;
        }
    }

    /** Lightweight representation of an audit_log row. */
    public static class AuditEntry {
        public final int    id;
        public final String action;
        public final String targetTable;
        public final int    targetId;
        public final String description;
        public final String performedBy;
        public final String performedAt;

        public AuditEntry(int id, String action, String targetTable, int targetId,
                          String description, String performedBy, String performedAt) {
            this.id          = id;
            this.action      = action;
            this.targetTable = targetTable;
            this.targetId    = targetId;
            this.description = description;
            this.performedBy = performedBy;
            this.performedAt = performedAt;
        }
    }
}
