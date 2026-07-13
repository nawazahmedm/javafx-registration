package com.learn.registration.view;

import com.learn.registration.controller.UserController;
import com.learn.registration.controller.UserController.ValidationResult;
import com.learn.registration.model.User;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * VIEW CONTROLLER for edituser.fxml
 *
 * Lifecycle:
 *   1. FXMLLoader creates this controller and injects all @FXML fields
 *   2. initialize() runs — sets up ToggleGroup, ComboBox, listeners
 *   3. UserListViewController calls setUser(user) to pre-populate the form
 *   4. User clicks "Save Changes" → onSave() validates + updates via UserController
 *   5. Window closes → UserListViewController refreshes its table
 */
public class EditUserViewController implements Initializable {

    // ── Injected FXML fields ──────────────────────────────────────────────────

    @FXML private Label  idLabel;
    @FXML private Label  editSubtitle;

    // Tab 1 — Basic Info
    @FXML private TextField        firstNameField;
    @FXML private TextField        lastNameField;
    @FXML private TextField        emailField;
    @FXML private TextField        phoneField;
    @FXML private ComboBox<String> countryCombo;

    // Gender radio buttons
    @FXML private RadioButton radioMale;
    @FXML private RadioButton radioFemale;
    @FXML private RadioButton radioOther;

    // Hobby checkboxes
    @FXML private CheckBox cbReading;
    @FXML private CheckBox cbGaming;
    @FXML private CheckBox cbCooking;
    @FXML private CheckBox cbTraveling;
    @FXML private CheckBox cbMusic;
    @FXML private CheckBox cbSports;
    @FXML private CheckBox cbArt;
    @FXML private CheckBox cbCoding;

    // Tab 2 — Preferences & Bio
    @FXML private CheckBox     cbNewsletter;
    @FXML private CheckBox     cbTerms;
    @FXML private TextArea     bioArea;
    @FXML private ProgressBar  profileProgress;
    @FXML private Label        progressLabel;

    // Feedback
    @FXML private Label statusLabel;

    // ── Non-FXML fields ───────────────────────────────────────────────────────

    private final UserController userController = new UserController();
    private ToggleGroup genderGroup;
    private User currentUser;   // the user being edited

    // ── Initializable ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupGenderToggleGroup();
        setupCountryComboBox();
        setupProfileProgressListeners();
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

    /**
     * Wires checkboxes so the ProgressBar updates to reflect how many
     * optional fields are filled in — a purely visual completeness indicator.
     */
    private void setupProfileProgressListeners() {
        List<CheckBox> boxes = List.of(
            cbReading, cbGaming, cbCooking, cbTraveling,
            cbMusic, cbSports, cbArt, cbCoding, cbNewsletter, cbTerms
        );
        for (CheckBox cb : boxes) {
            cb.selectedProperty().addListener((obs, o, n) -> updateProgress());
        }
    }

    private void updateProgress() {
        List<CheckBox> all = List.of(
            cbReading, cbGaming, cbCooking, cbTraveling,
            cbMusic, cbSports, cbArt, cbCoding, cbNewsletter, cbTerms
        );
        long checked = all.stream().filter(CheckBox::isSelected).count();
        double progress = (double) checked / all.size();
        profileProgress.setProgress(progress);
        progressLabel.setText((int) (progress * 100) + "%");
    }

    // ── Public API: called by UserListViewController ──────────────────────────

    /**
     * Pre-populates every form field with the selected user's data.
     * Must be called AFTER FXMLLoader.load() so all @FXML fields are injected.
     *
     * @param user  the User to edit
     */
    public void setUser(User user) {
        this.currentUser = user;

        // Header
        idLabel.setText(String.valueOf(user.getId()));
        editSubtitle.setText("Updating user #" + user.getId()
                + " — " + user.getFullName());

        // Basic fields
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone() != null ? user.getPhone() : "");
        countryCombo.setValue(user.getCountry());

        // Gender radio
        switch (user.getGender() != null ? user.getGender() : "") {
            case "Male"   -> radioMale.setSelected(true);
            case "Female" -> radioFemale.setSelected(true);
            case "Other"  -> radioOther.setSelected(true);
        }

        // Hobbies — split the CSV and tick matching checkboxes
        if (user.getHobbies() != null && !user.getHobbies().isBlank()) {
            List<String> hobbies = Arrays.asList(user.getHobbies().split(",\\s*"));
            cbReading.setSelected(hobbies.contains("Reading"));
            cbGaming.setSelected(hobbies.contains("Gaming"));
            cbCooking.setSelected(hobbies.contains("Cooking"));
            cbTraveling.setSelected(hobbies.contains("Traveling"));
            cbMusic.setSelected(hobbies.contains("Music"));
            cbSports.setSelected(hobbies.contains("Sports"));
            cbArt.setSelected(hobbies.contains("Art"));
            cbCoding.setSelected(hobbies.contains("Coding"));
        }

        // Preferences
        cbNewsletter.setSelected(user.isNewsletter());
        cbTerms.setSelected(user.isTermsAccepted());

        // Bio
        bioArea.setText(user.getBio() != null ? user.getBio() : "");

        // Sync the progress bar to the current state
        updateProgress();
    }

    // ── FXML event handlers ───────────────────────────────────────────────────

    /** Called when "Save Changes" is clicked. */
    @FXML
    private void onSave() {
        hideStatus();

        if (currentUser == null) {
            showError("No user loaded. Cannot save.");
            return;
        }

        // Read gender
        String gender = "";
        Toggle selected = genderGroup.getSelectedToggle();
        if (selected instanceof RadioButton rb) {
            gender = rb.getText();
        }

        // Build updated User — keep the original id, registeredAt, passwordHash, and role
        User updated = new User(
            currentUser.getId(),
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
            currentUser.getRegisteredAt(),
            currentUser.getPasswordHash(),  // never changed here
            currentUser.getRole()           // preserve existing role
        );

        ValidationResult result = userController.updateUser(updated);

        if (result.isSuccess()) {
            showSuccess(result.getMessage());
            // Close the dialog after a moment — user sees success message
            closeWindow();
        } else {
            showError(result.getMessage());
        }
    }

    /** Called when "Cancel" is clicked — closes without saving. */
    @FXML
    private void onCancel() {
        closeWindow();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String collectHobbies() {
        List<String> selected = new ArrayList<>();
        if (cbReading.isSelected())   selected.add("Reading");
        if (cbGaming.isSelected())    selected.add("Gaming");
        if (cbCooking.isSelected())   selected.add("Cooking");
        if (cbTraveling.isSelected()) selected.add("Traveling");
        if (cbMusic.isSelected())     selected.add("Music");
        if (cbSports.isSelected())    selected.add("Sports");
        if (cbArt.isSelected())       selected.add("Art");
        if (cbCoding.isSelected())    selected.add("Coding");
        return String.join(", ", selected);
    }

    private void closeWindow() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
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

    private String trim(String s) {
        return s != null ? s.trim() : "";
    }
}
