# JavaFX Registration App

A fully functional **JavaFX desktop application** built with the **MVC pattern**,
demonstrating every major UI component, SQLite persistence, BCrypt security,
role-based access control, and an audit log.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI Framework | JavaFX 22 + FXML |
| Language | Java 17 |
| Architecture | MVC (Model-View-Controller) |
| Database | SQLite (via sqlite-jdbc) |
| Password Security | BCrypt (jbcrypt) |
| Build Tool | Maven |

---

## Application Flow

```
┌─────────────┐     login      ┌───────────────┐    Edit button   ┌──────────────┐
│  Login      │ ─────────────► │  User List    │ ───────────────► │  Edit User   │
│  Screen     │                │  Screen       │                   │  (Modal)     │
└─────────────┘                └───────────────┘                   └──────────────┘
       │                              │
       │ Register here                │ + New Registration / ← Back
       ▼                              ▼
┌─────────────────────────────────────────────┐
│           Registration Form                 │
└─────────────────────────────────────────────┘
```

---

## Screens

### 1. Login Screen

The app opens on the login screen — nothing is accessible without valid credentials.

- **Admin Login** tab — authenticates against the `admin_users` table (username + BCrypt password)
- **User Login** tab — authenticates against the `users` table (email + BCrypt password)
- Default admin credentials are shown in the hint box
- "Register here" navigates to the registration form without requiring login

![Login Screen](docs/screenshots/01-login.png)

> **Default admin:** username `admin` / password `admin123`

---

### 2. Registration Form — Personal Information

A scrollable form packed with every major JavaFX UI component.

**Components visible:**
- `TextField` — First Name, Last Name, Email, Phone
- `DatePicker` — Date of Birth
- `ComboBox` — Country dropdown
- `PasswordField` — Password with live strength indicator
- `PasswordField` — Confirm Password
- `TitledPane` — section card wrapper
- `GridPane` — two-column form layout

![Registration Form - Top](docs/screenshots/02-registration-top.png)

**Gender section** uses `RadioButton` + `ToggleGroup` (mutually exclusive).
**Hobbies section** uses independent `CheckBox` controls.

---

### 3. Registration Form — Profile & Preferences

The bottom half of the scrollable registration form.

**Components visible:**
- `Slider` — Years of experience with live label update
- `TextArea` — Bio / About Me (multi-line, wrappable)
- `CheckBox` — Subscribe to newsletter
- `CheckBox` — Terms & Conditions (required)
- `Hyperlink` — View Terms & Conditions link
- `Button` — Clear Form (secondary) + Submit Registration (primary)
- `Separator` — visual divider
- `ScrollPane` — wraps the entire form

![Registration Form - Bottom](docs/screenshots/03-registration-bottom.png)

**Password rules enforced:**
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 number
- Both password fields must match
- Plain-text password is **never stored** — BCrypt hash only

---

### 4. User List

Displays all registered users in a `TableView`. Navigated to automatically
after a successful login or registration.

**Features visible:**
- `TableView` with 11 columns: ID, First Name, Last Name, Email, Phone, Gender, Country, Newsletter, Role, Registered At, Actions
- `ToolBar` — Search field + user count + Refresh + New Registration buttons
- `TextField` — live search filters all rows instantly (no button press)
- **Actions column** — Edit (green) + Delete (red) buttons per row, built in Java code
- **Session banner** — top right shows logged-in username + role badge + Logout button
- Role badge: `ADMIN` (yellow) / `USER` (grey)
- Delete button is **disabled** for non-admin users

![User List](docs/screenshots/04-user-list.png)

> Logged in as `admin` — both Edit and Delete buttons are active.
> Regular users see Delete greyed out.

---

### 5. Edit User — Basic Information Tab

Opens as a **modal dialog** when Edit is clicked on any row.
Pre-populated with the selected user's existing data.

**Tab 1 — Basic Information:**
- `TabPane` with 2 tabs
- `TextField` — First Name, Last Name, Email, Phone
- `ComboBox` — Country (pre-selected)
- `RadioButton` + `ToggleGroup` — Gender (pre-selected)
- `CheckBox` — Hobbies (pre-ticked from saved data)
- `GridPane` — two-column layout
- `Button` — Cancel + Save Changes

![Edit User - Basic Info](docs/screenshots/05-edit-basic.png)

---

### 6. Edit User — Preferences & Bio Tab

The second tab of the edit modal.

**Tab 2 — Preferences & Bio:**
- `ProgressBar` — profile completeness (updates live as checkboxes are ticked)
- `CheckBox` — Subscribe to newsletter (pre-ticked)
- `CheckBox` — Terms & Conditions accepted (pre-ticked)
- `Accordion` — collapsible Bio / About Me section
- `Separator` — visual divider

![Edit User - Preferences](docs/screenshots/06-edit-preferences.png)

> Profile completeness shows **40%** — calculated from how many optional
> fields are filled in.

---

## JavaFX Components Used

| Component | Screen |
|-----------|--------|
| `TextField` | Registration, Edit User |
| `PasswordField` | Login, Registration |
| `DatePicker` | Registration |
| `ComboBox` | Registration, Edit User |
| `RadioButton` + `ToggleGroup` | Registration (gender), Login (mode toggle), Edit User |
| `ToggleButton` | Login (Admin/User switcher) |
| `CheckBox` | Registration (hobbies, newsletter, terms), Edit User |
| `TextArea` | Registration (bio) |
| `Slider` | Registration (experience) |
| `TableView` + `TableColumn` | User List |
| `Button` | All screens |
| `Hyperlink` | Registration (Terms link) |
| `Label` | All screens |
| `Separator` | Registration, Edit User |
| `ToolBar` | User List |
| `TitledPane` | Registration (section cards) |
| `TabPane` + `Tab` | Edit User |
| `Accordion` | Edit User (Bio section) |
| `ProgressBar` | Edit User (profile completeness) |
| `Tooltip` | Edit User (disabled Delete hint) |
| `BorderPane` | Root layout — all screens |
| `VBox` | Vertical stacking — all screens |
| `HBox` | Horizontal stacking — all screens |
| `GridPane` | Two-column form grid |
| `FlowPane` | Wrapping hobby checkboxes |
| `ScrollPane` | Scrollable registration form |
| `Alert` | Confirmation dialogs, Terms popup |

---

## MVC Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  VIEW  (FXML + ViewController)                                   │
│  login.fxml         ↔  LoginViewController                       │
│  registration.fxml  ↔  RegistrationViewController               │
│  userlist.fxml      ↔  UserListViewController                    │
│  edituser.fxml      ↔  EditUserViewController                    │
│                              │  calls only                       │
├──────────────────────────────▼───────────────────────────────────┤
│  SECURITY                                                        │
│  AuthController   — BCrypt login, password hashing + validation  │
│  SessionManager   — who is logged in, role, isAdmin()            │
│                              │  used by                          │
├──────────────────────────────▼───────────────────────────────────┤
│  CONTROLLER                                                      │
│  UserController   — registerUser / updateUser / deleteUser       │
│                     validation + audit log on every write        │
│                              │  calls only                       │
├──────────────────────────────▼───────────────────────────────────┤
│  MODEL                                                           │
│  User.java          — data entity (POJO)                         │
│  DatabaseHelper     — SQLite CRUD, Singleton, 3 tables           │
└──────────────────────────────────────────────────────────────────┘
```

**Key rule:** Views never call `DatabaseHelper` directly.
All data flows through `UserController` or `AuthController`.

---

## Security Features

| Feature | Implementation |
|---------|---------------|
| Password hashing | BCrypt, cost factor 12 |
| Password strength | 8+ chars, 1 uppercase, 1 digit — live UI feedback |
| Login gate | App starts on login screen — no bypass |
| Session management | `SessionManager` static holder — cleared on logout |
| Role-based access | Delete gated to `admin` (UI disabled + controller check) |
| Audit trail | Every INSERT / UPDATE / DELETE logged to `audit_log` table |
| SQL injection prevention | All queries use `PreparedStatement` |
| Generic error messages | "Invalid username or password" — no field-level hints |

---

## Database Tables

SQLite file `users.db` is created automatically on first launch.

| Table | Purpose |
|-------|---------|
| `users` | Registered user accounts (includes `password_hash`, `role`) |
| `admin_users` | Admin credentials — seeded with `admin` / `admin123` |
| `audit_log` | Append-only record of every insert / update / delete |

---

## How to Run

### Prerequisites
- JDK 17 or 21 — [adoptium.net](https://adoptium.net)
- Maven 3.8+ — [maven.apache.org](https://maven.apache.org)

### Maven settings (one-time)
Create `C:\Users\<you>\.m2\settings.xml`:
```xml
<settings>
  <pluginGroups>
    <pluginGroup>org.openjfx</pluginGroup>
  </pluginGroups>
</settings>
```

### Run
```cmd
cd javafx-registration
del users.db
mvn clean compile
mvn javafx:run
```

> Delete `users.db` only on first run after a fresh clone so the schema
> is created correctly.

---

## Project Structure

```
javafx-registration/
├── pom.xml
├── HOW_TO_RUN.md
├── JAVAFX_INTERVIEW_GUIDE.md
├── docs/screenshots/
│   ├── 01-login.png
│   ├── 02-registration-top.png
│   ├── 03-registration-bottom.png
│   ├── 04-user-list.png
│   ├── 05-edit-basic.png
│   └── 06-edit-preferences.png
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/learn/registration/
    │       ├── App.java
    │       ├── model/User.java
    │       ├── database/DatabaseHelper.java
    │       ├── security/AuthController.java
    │       ├── security/SessionManager.java
    │       ├── controller/UserController.java
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

*Built as a JavaFX learning reference — covers MVC architecture, every major
UI component, BCrypt security, and SQLite persistence in a single runnable project.*
