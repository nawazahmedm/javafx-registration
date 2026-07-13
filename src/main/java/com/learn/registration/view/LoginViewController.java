package com.learn.registration.view;

import com.learn.registration.App;
import com.learn.registration.security.AuthController;
import com.learn.registration.security.AuthController.AuthResult;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * VIEW CONTROLLER for login.fxml
 *
 * Two modes toggled by the user:
 *   Admin mode  — authenticates against admin_users table (username + password)
 *   User mode   — authenticates against users table (email + password)
 *
 * On success → navigates to the User List screen.
 * "Register here" link → navigates to the Registration form (no login needed
 *  to register a new account — this mirrors most real-world apps).
 */
public class LoginViewController implements Initializable {

    @FXML private ToggleButton  adminToggle;
    @FXML private ToggleButton  userToggle;
    @FXML private Label         usernameLabel;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         statusLabel;
    @FXML private Button        loginBtn;

    private final AuthController authController = new AuthController();
    private ToggleGroup modeGroup;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Wire both buttons into a ToggleGroup so they are mutually exclusive.
        // Without a ToggleGroup each button manages its own state independently
        // and both can be selected at the same time.
        modeGroup = new ToggleGroup();
        adminToggle.setToggleGroup(modeGroup);
        userToggle.setToggleGroup(modeGroup);
        adminToggle.setSelected(true);   // default: Admin mode

        // React to whichever button the user clicks
        modeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            // Prevent deselecting both by re-selecting the previous one
            if (newToggle == null) {
                modeGroup.selectToggle(oldToggle);
                return;
            }
            syncToggleUI();
            clearFields();
            hideStatus();
        });

        syncToggleUI();
        hideStatus();

        // Enter on username jumps focus to password field
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    /** Returns true when Admin tab is active, false when User tab is active. */
    private boolean isAdminMode() {
        return modeGroup.getSelectedToggle() == adminToggle;
    }

    /** Updates labels and button text to reflect the current mode. */
    private void syncToggleUI() {
        if (isAdminMode()) {
            usernameLabel.setText("Username");
            usernameField.setPromptText("Enter admin username");
            loginBtn.setText("Login as Admin");
        } else {
            usernameLabel.setText("Email Address");
            usernameField.setPromptText("Enter your email");
            loginBtn.setText("Login as User");
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @FXML
    private void onLogin() {
        hideStatus();

        String username = usernameField.getText();
        String password = passwordField.getText();

        AuthResult result = isAdminMode()
            ? authController.loginAdmin(username, password)
            : authController.loginUser(username, password);

        if (result.isSuccess()) {
            showSuccess(result.getMessage());
            navigateTo("/com/learn/registration/view/userlist.fxml",
                       App.APP_TITLE + " — User List", 940, 660);
        } else {
            showError(result.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onGoToRegister() {
        navigateTo("/com/learn/registration/view/registration.fxml",
                   App.APP_TITLE + " — Registration", 780, 720);
    }

    private void navigateTo(String fxmlPath, String title, double w, double h) {
        try {
            URL fxml = getClass().getResource(fxmlPath);
            Scene scene = new Scene(new FXMLLoader(fxml).load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setWidth(w);
            stage.setHeight(h);
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Navigation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
    }

    private void showSuccess(String message) {
        statusLabel.setText("✓  " + message);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-success");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showError(String message) {
        statusLabel.setText("✗  " + message);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-error");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
}
