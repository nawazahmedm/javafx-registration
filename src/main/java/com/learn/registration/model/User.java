package com.learn.registration.model;

/**
 * MODEL — represents a User entity.
 *
 * Security additions:
 *   - passwordHash : BCrypt hash of the user's password (never plain text)
 *   - role         : "admin" | "user" — controls what actions are allowed in the UI
 */
public class User {

    private int    id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String gender;
    private String country;
    private String bio;
    private boolean newsletter;
    private boolean termsAccepted;
    private String hobbies;
    private String registeredAt;

    // ── Security fields ───────────────────────────────────────────────────────
    private String passwordHash;   // BCrypt hash — never the plain-text password
    private String role;           // "admin" | "user"

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    /** Full constructor — used when loading from DB. */
    public User(int id, String firstName, String lastName, String email,
                String phone, String gender, String country, String bio,
                boolean newsletter, boolean termsAccepted, String hobbies,
                String registeredAt, String passwordHash, String role) {
        this.id            = id;
        this.firstName     = firstName;
        this.lastName      = lastName;
        this.email         = email;
        this.phone         = phone;
        this.gender        = gender;
        this.country       = country;
        this.bio           = bio;
        this.newsletter    = newsletter;
        this.termsAccepted = termsAccepted;
        this.hobbies       = hobbies;
        this.registeredAt  = registeredAt;
        this.passwordHash  = passwordHash;
        this.role          = role;
    }

    /** Constructor for new registrations — id and registeredAt assigned by DB. */
    public User(String firstName, String lastName, String email, String phone,
                String gender, String country, String bio,
                boolean newsletter, boolean termsAccepted, String hobbies,
                String passwordHash, String role) {
        this.firstName     = firstName;
        this.lastName      = lastName;
        this.email         = email;
        this.phone         = phone;
        this.gender        = gender;
        this.country       = country;
        this.bio           = bio;
        this.newsletter    = newsletter;
        this.termsAccepted = termsAccepted;
        this.hobbies       = hobbies;
        this.passwordHash  = passwordHash;
        this.role          = role;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int     getId()           { return id; }
    public void    setId(int id)     { this.id = id; }

    public String  getFirstName()    { return firstName; }
    public void    setFirstName(String v) { this.firstName = v; }

    public String  getLastName()     { return lastName; }
    public void    setLastName(String v)  { this.lastName = v; }

    public String  getEmail()        { return email; }
    public void    setEmail(String v)     { this.email = v; }

    public String  getPhone()        { return phone; }
    public void    setPhone(String v)     { this.phone = v; }

    public String  getGender()       { return gender; }
    public void    setGender(String v)    { this.gender = v; }

    public String  getCountry()      { return country; }
    public void    setCountry(String v)   { this.country = v; }

    public String  getBio()          { return bio; }
    public void    setBio(String v)       { this.bio = v; }

    public boolean isNewsletter()    { return newsletter; }
    public void    setNewsletter(boolean v)    { this.newsletter = v; }

    public boolean isTermsAccepted() { return termsAccepted; }
    public void    setTermsAccepted(boolean v) { this.termsAccepted = v; }

    public String  getHobbies()      { return hobbies; }
    public void    setHobbies(String v)   { this.hobbies = v; }

    public String  getRegisteredAt() { return registeredAt; }
    public void    setRegisteredAt(String v) { this.registeredAt = v; }

    // Security getters/setters
    public String  getPasswordHash() { return passwordHash; }
    public void    setPasswordHash(String v) { this.passwordHash = v; }

    public String  getRole()         { return role; }
    public void    setRole(String v)      { this.role = v; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getFullName() { return firstName + " " + lastName; }

    /** Convenience check used in the UI to gate admin-only actions. */
    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }

    @Override
    public String toString() {
        return "User{id=" + id + ", name=" + getFullName()
                + ", email=" + email + ", role=" + role + "}";
    }
}
