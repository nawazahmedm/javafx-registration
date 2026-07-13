package com.learn.registration.view;

import com.learn.registration.App;
import com.learn.registration.controller.UserController;
import com.learn.registration.controller.UserController.ValidationResult;
import com.learn.registration.model.User;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * VIEW CONTROLLER for registration.fxml
 *
 * Security additions:
 *   - Two PasswordFields: passwordField + confirmPasswordField
 *   - Live password-strength indicator updates as the user types
 *   - Plain-text passwords are passed to UserController.registerUser()
 *     which hashes them via BCrypt before any DB write
 *   - This class never sees or stores the hash itself
 */
public class RegistrationViewController implements Initializable {

    // ── Personal info ─────────────────────────────────────────────────────────
    @FXML private TextField        firstNameField;
    @FXML private TextField        lastNameField;
    @FXML private TextField        emailField;
    @FXML private TextField        phoneField;
    @FXML private DatePicker       dobPicker;
    @FXML private ComboBox<String> countryCombo;

    // ── Password fields ───────────────────────────────────────────────────────
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         pwStrengthLabel;   // live strength hint

    // ── Gender ────────────────────────────────────────────────────────────────
    @FXML private RadioButton radioMale;
    @FXML private RadioButton radioFemale;
    @FXML private RadioButton radioOther;

    // ── Hobbies ───────────────────────────────────────────────────────────────
    @FXML private CheckBox cbReading;
    @FXML private CheckBox cbGaming;
    @FXML private CheckBox cbCooking;
    @FXML private CheckBox cbTraveling;
    @FXML private CheckBox cbMusic;
    @FXML private CheckBox cbSports;
    @FXML private CheckBox cbArt;
    @FXML private CheckBox cbCoding;

    // ── Profile ───────────────────────────────────────────────────────────────
    @FXML private Slider   experienceSlider;
    @FXML private Label    experienceLabel;
    @FXML private TextArea bioArea;

    // ── Preferences ───────────────────────────────────────────────────────────
    @FXML private CheckBox cbNewsletter;
    @FXML private CheckBox cbTerms;

    // ── Feedback ──────────────────────────────────────────────────────────────
    @FXML private Label statusLabel;

    // ── Non-FXML ──────────────────────────────────────────────────────────────
    private final UserController userController = new UserController();
    private ToggleGroup genderGroup;

    // ── Initializable ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupGenderToggleGroup();
        setupCountryComboBox();
        setupExperienceSlider();
        setupPasswordStrengthIndicator();
        hideStatus();
    }

    // ── Setup helpers ─────────────────────────────────────────────────────────

    private void setupGenderToggleGroup() {
        genderGroup = new ToggleGroup();
        radioMale.setToggleGroup(genderGroup);
        radioFemale.setToggleGroup(genderGroup);
        radioOther.setToggleGroup(genderGroup);
    }

    private void setupCountryComboBox() {
        countryCombo.setItems(FXCollections.observableArrayList(
            "United States", "United Kingdom", "Canada", "Australia",
            "Germany", "France", "India", "Japan", "Brazil", "South Africa",
            "Pakistan", "Nigeria", "Mexico", "Italy", "Spain",
            "Netherlands", "Sweden", "Norway", "China", "South Korea"
        ));
    }

    private void setupExperienceSlider() {
        if (experienceSlider != null && experienceLabel != null) {
            experienceLabel.setText("0 yrs");
            experienceSlider.valueProperty().addListener((obs, o, n) ->
                experienceLabel.setText(n.intValue() + " yrs"));
        }
    }

    /**
     * Wires a listener to the password field so a strength hint updates live.
     *
     * Rules (same as AuthController.validatePasswordStrength):
     *   Weak   — fewer than 8 characters
     *   Medium — 8+ chars but missing a digit OR uppercase
     *   Strong — 8+ chars, has digit AND uppercase
     */
    private void setupPasswordStrengthIndicator() {
        if (passwordField == null || pwStrengthLabel == null) return;

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                pwStrengthLabel.setText("");
                return;
            }
            if (newVal.length() < 8) {
                pwStrengthLabel.setText("Weak — too short");
                setStrengthStyle("weak");
            } else if (!newVal.matches(".*[0-9].*") || !newVal.matches(".*[A-Z].*")) {
                pwStrengthLabel.setText("Medium — add a number and uppercase letter");
                setStrengthStyle("medium");
            } else {
                pwStrengthLabel.setText("Strong ✓");
                setStrengthStyle("strong");
            }
        });
    }

    private void setStrengthStyle(String level) {
        pwStrengthLabel.getStyleClass().removeAll(
            "pw-strength-weak", "pw-strength-medium", "pw-strength-strong");
        pwStrengthLabel.getStyleClass().add("pw-strength-" + level);
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void onSubmit() {
        hideStatus();

        // Collect gender
        String gender = "";
        Toggle selected = genderGroup.getSelectedToggle();
        if (selected instanceof RadioButton rb) gender = rb.getText();

        // Build User — passwordHash is empty here; controller will hash it
        User user = new User(
            trim(firstNameField.getText()),
            trim(lastNameField.getText()),
            trim(emailField.getText()),
            trim(phoneField.getText()),
            gender,
            countryCombo.getValue() != null ? countryCombo.getValue() : "",
            bioArea.getText() != null ? bioArea.getText().trim() : "",
            cbNewsletter.isSelected(),
            cbTerms.isSelected(),
            collectHobbies(),
            "",       // passwordHash — will be set by UserController
            "user"    // role — always "user" for self-registration
        );

        // Pass plain-text passwords to controller — it validates + hashes
        String plain   = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        ValidationResult result = userController.registerUser(user, plain, confirm);

        if (result.isSuccess()) {
            showSuccess(result.getMessage());
            clearForm();
            navigateToUserList();
        } else {
            showError(result.getMessage());
        }
    }

    @FXML
    private void onClear() {
        clearForm();
        hideStatus();
    }

    @FXML
    private void onViewUsers() {
        navigateToUserList();
    }

    @FXML
    private void onViewTerms() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Terms & Conditions");
        alert.setHeaderText("Terms & Conditions");
        alert.setContentText(
            "By registering, you agree to our Terms of Service and Privacy Policy.\n\n"
            + "Your data is stored locally in a SQLite database and will not "
            + "be shared with any third party.\n\n"
            + "Passwords are hashed with BCrypt — they cannot be recovered.\n\n"
            + "This is a learning demo application.");
        alert.showAndWait();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToUserList() {
        try {
            URL fxml = getClass().getResource(
                    "/com/learn/registration/view/userlist.fxml");
            FXMLLoader loader = new FXMLLoader(fxml);
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.setTitle(App.APP_TITLE + " — User List");
            stage.setScene(scene);
            stage.setWidth(980);
            stage.setHeight(660);
            stage.setResizable(true);
        } catch (IOException e) {
            showError("Could not open the user list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String collectHobbies() {
        List<String> sel = new ArrayList<>();
        if (cbReading.isSelected())   sel.add("Reading");
        if (cbGaming.isSelected())    sel.add("Gaming");
        if (cbCooking.isSelected())   sel.add("Cooking");
        if (cbTraveling.isSelected()) sel.add("Traveling");
        if (cbMusic.isSelected())     sel.add("Music");
        if (cbSports.isSelected())    sel.add("Sports");
        if (cbArt.isSelected())       sel.add("Art");
        if (cbCoding.isSelected())    sel.add("Coding");
        return String.join(", ", sel);
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        pwStrengthLabel.setText("");
        dobPicker.setValue(null);
        countryCombo.getSelectionModel().clearSelection();
        genderGroup.selectToggle(null);
        bioArea.clear();
        for (CheckBox cb : List.of(cbReading, cbGaming, cbCooking, cbTraveling,
                                    cbMusic, cbSports, cbArt, cbCoding))
            cb.setSelected(false);
        cbNewsletter.setSelected(false);
        cbTerms.setSelected(false);
        if (experienceSlider != null) experienceSlider.setValue(0);
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-success");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
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

    private String trim(String s) { return s != null ? s.trim() : ""; }
}
