# JavaFX Registration App — How to Run

## Prerequisites

| Tool  | Version | Check command   |
|-------|---------|-----------------|
| JDK   | 17 or 21 | `java -version` |
| Maven | 3.8+    | `mvn -version`  |

JavaFX and all other dependencies are downloaded automatically by Maven.
You do **not** need to install JavaFX separately.

---

## First-Time Setup (run once)

### 1. Register the JavaFX plugin group in Maven settings

If `C:\Users\nawaz\.m2\settings.xml` does not exist, it has already been
created for you. It contains:

```xml
<settings>
  <pluginGroups>
    <pluginGroup>org.openjfx</pluginGroup>
  </pluginGroups>
</settings>
```

This allows `mvn javafx:run` to work as a short command.

### 2. Delete any old database file

If you ran the app before the security update, the old `users.db` has no
`password_hash`, `role`, `admin_users`, or `audit_log` columns.
Delete it so a fresh schema is created:

```cmd
cd C:\Users\nawaz\workspace\javafx-registration\javafx-registration
del users.db
```

---

## Run the App

```cmd
cd C:\Users\nawaz\workspace\javafx-registration\javafx-registration
mvn clean compile
mvn javafx:run
```

`mvn clean compile` downloads dependencies on the first run (~30 sec).
Every run after that is instant.

---

## Default Login Credentials

The app starts on the **Login screen**.
A default admin account is seeded automatically on first launch:

| Field    | Value      |
|----------|------------|
| Username | `admin`    |
| Password | `admin123` |

> **Change this password after first login in any real deployment.**

---

## App Flow

```
┌─────────────────────────────────────────────────────┐
│                   LOGIN SCREEN                      │
│  ┌──────────────┐  ┌──────────────┐                 │
│  │ Admin Login  │  │  User Login  │  ← toggle tabs  │
│  └──────────────┘  └──────────────┘                 │
│                                                     │
│  "Register here" → Registration Form (no login)     │
└──────────────────────────┬──────────────────────────┘
                           │ success
                           ▼
┌─────────────────────────────────────────────────────┐
│                   USER LIST                         │
│  Session banner: username + role badge + Logout     │
│  Search bar — filters live as you type              │
│                                                     │
│  TableView columns:                                 │
│    ID | First | Last | Email | Phone | Gender |     │
│    Country | Newsletter | Role | Registered | Actions│
│                                                     │
│  Actions column:                                    │
│    [Edit]   — available to all logged-in users      │
│    [Delete] — admin only (greyed out for users)     │
└───────────┬─────────────────┬───────────────────────┘
            │ Edit            │ + New Registration
            ▼                 ▼
┌───────────────────┐  ┌──────────────────────────────┐
│   EDIT USER       │  │     REGISTRATION FORM        │
│   (modal dialog)  │  │                              │
│   Tab 1: Basic    │  │  Personal Info               │
│   Tab 2: Prefs    │  │  Password + Confirm          │
│                   │  │  Gender (Radio buttons)      │
│   Save / Cancel   │  │  Hobbies (Checkboxes)        │
└───────────────────┘  │  Experience (Slider)         │
                       │  Bio (TextArea)              │
                       │  Newsletter + Terms          │
                       └──────────────────────────────┘
```

---

## Screen-by-Screen Guide

### Login Screen
- Toggle between **Admin Login** (username) and **User Login** (email)
- Enter credentials and press Enter or click **Login**
- Click **Register here** to go directly to the registration form

### Registration Form
- All fields marked `*` are required
- **Password rules:** minimum 8 characters, at least 1 uppercase letter,
  at least 1 number
- A live **strength indicator** updates as you type (Weak / Medium / Strong)
- Both password fields must match before submission
- Click **Submit Registration** — the password is BCrypt-hashed before
  being stored; the plain-text password never touches the database
- On success the app navigates automatically to the User List

### User List
- **Search box** filters all rows instantly (name, email, or country)
- **Refresh** reloads all users from the database
- **+ New Registration** opens the registration form
- **Edit** button opens the edit modal pre-populated with the user's data
- **Delete** button — admin only; shows a confirmation dialog first
- **Logout** button clears the session and returns to the Login screen

### Edit User (modal)
- **Tab 1 — Basic Information:** name, email, phone, country, gender, hobbies
- **Tab 2 — Preferences & Bio:** newsletter, terms, bio text, profile progress bar
- Click **Save Changes** to persist, or **Cancel** to discard

---

## Project Structure

```
javafx-registration/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/learn/registration/
    │       ├── App.java                    ← entry point → login screen
    │       ├── model/
    │       │   └── User.java               ← POJO: all user fields incl. passwordHash + role
    │       ├── database/
    │       │   └── DatabaseHelper.java     ← SQLite CRUD, 3 tables, audit log
    │       ├── security/
    │       │   ├── AuthController.java     ← BCrypt login/logout/hash/validation
    │       │   └── SessionManager.java     ← static session: username + role
    │       ├── controller/
    │       │   └── UserController.java     ← business logic, validation, audit writes
    │       └── view/
    │           ├── LoginViewController.java
    │           ├── RegistrationViewController.java
    │           ├── UserListViewController.java
    │           └── EditUserViewController.java
    └── resources/com/learn/registration/
        ├── css/style.css
        └── view/
            ├── login.fxml
            ├── registration.fxml
            ├── userlist.fxml
            └── edituser.fxml
```

---

## Database Tables

The app creates `users.db` automatically in the project folder.

| Table        | Purpose                                              |
|--------------|------------------------------------------------------|
| `users`      | Registered user accounts (includes password_hash, role) |
| `admin_users`| Admin credentials — seeded with admin/admin123       |
| `audit_log`  | Immutable record of every insert / update / delete   |

---

## MVC + Security Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  VIEW  (FXML + ViewController)                               │
│  login.fxml         ↔  LoginViewController                   │
│  registration.fxml  ↔  RegistrationViewController            │
│  userlist.fxml      ↔  UserListViewController                │
│  edituser.fxml      ↔  EditUserViewController                │
│                          │ calls                             │
├──────────────────────────▼───────────────────────────────────┤
│  SECURITY                                                    │
│  AuthController  — BCrypt verify, hash, password rules       │
│  SessionManager  — who is logged in, isAdmin()               │
│                          │ used by                           │
├──────────────────────────▼───────────────────────────────────┤
│  CONTROLLER                                                  │
│  UserController  — registerUser / updateUser / deleteUser    │
│                    (role-checks + audit log on every write)  │
│                          │ calls                             │
├──────────────────────────▼───────────────────────────────────┤
│  MODEL                                                       │
│  User.java         — data entity (POJO)                      │
│  DatabaseHelper    — SQLite CRUD, Singleton                  │
└──────────────────────────────────────────────────────────────┘
```

**Key rule:** Views never talk to `DatabaseHelper` directly.
All reads/writes flow through `UserController` or `AuthController`.

---

## Security Features Summary

| Feature                  | Implementation                                    |
|--------------------------|---------------------------------------------------|
| Password hashing         | BCrypt, cost factor 12 (jbcrypt 0.4)              |
| Password strength rules  | 8+ chars, 1 uppercase, 1 digit — live UI feedback |
| Login gate               | App opens on login screen — no bypass             |
| Session management       | `SessionManager` — cleared on logout              |
| Role-based access        | Delete gated to `admin` role (UI + controller)    |
| Audit trail              | Every INSERT/UPDATE/DELETE logged to `audit_log`  |
| SQL injection prevention | All queries use `PreparedStatement`               |
| Generic error messages   | Login errors don't reveal which field was wrong   |

---

## JavaFX Components Reference

| Component        | Where used                                    |
|------------------|-----------------------------------------------|
| `TextField`      | First name, last name, email, phone, search   |
| `PasswordField`  | Login password, registration password fields  |
| `DatePicker`     | Date of birth                                 |
| `ComboBox`       | Country selection                             |
| `RadioButton`    | Gender (Male / Female / Other)                |
| `ToggleGroup`    | Groups the gender radio buttons               |
| `ToggleButton`   | Admin / User login mode switcher              |
| `CheckBox`       | Hobbies, Newsletter, Terms & Conditions       |
| `TextArea`       | Bio / About Me                                |
| `Slider`         | Years of experience                           |
| `TableView`      | User list with sortable columns               |
| `TableColumn`    | Each column in the user table                 |
| `Button`         | Submit, Clear, Edit, Delete, Refresh, Logout  |
| `Hyperlink`      | View Terms & Conditions link                  |
| `Label`          | All text, status messages, role badge         |
| `Separator`      | Visual dividers                               |
| `ToolBar`        | Search bar + action buttons row               |
| `TitledPane`     | Section cards on registration + edit forms    |
| `TabPane`        | Two-tab layout in edit form                   |
| `Accordion`      | Collapsible Bio section in edit form          |
| `ProgressBar`    | Profile completeness indicator                |
| `Tooltip`        | Hint on disabled Delete button                |
| `BorderPane`     | Root layout for all screens                   |
| `VBox`           | Vertical stacking                             |
| `HBox`           | Horizontal stacking                           |
| `GridPane`       | Two-column form grid                          |
| `FlowPane`       | Wrapping hobby checkboxes                     |
| `ScrollPane`     | Scrollable registration form                  |
| `Alert`          | Confirmation and info dialogs                 |

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| `java -version` shows Java 8/11 | Install JDK 17+ from https://adoptium.net and set `JAVA_HOME` |
| `mvn` not recognised | Install Maven from https://maven.apache.org and add to PATH |
| `mvn javafx:run` → "No plugin found" | Check `C:\Users\nawaz\.m2\settings.xml` has the `pluginGroups` block shown above |
| App opens but login fails | Delete `users.db` and restart — the default admin is re-seeded |
| "Email already in use" on registration | Each email must be unique — use a different email address |
| Table shows no users after registration | Click **Refresh** or re-login; check the registration completed with no error |
| Compile error about constructor args | Delete `users.db` — old DB schema is incompatible with the updated model |
