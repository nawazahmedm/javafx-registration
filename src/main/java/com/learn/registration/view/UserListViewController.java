package com.learn.registration.view;

import com.learn.registration.App;
import com.learn.registration.controller.UserController;
import com.learn.registration.controller.UserController.ValidationResult;
import com.learn.registration.model.User;
import com.learn.registration.security.AuthController;
import com.learn.registration.security.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * VIEW CONTROLLER for userlist.fxml
 *
 * Security additions:
 *   - Session banner shows who is logged in and their role badge
 *   - Delete button is DISABLED for non-admin users (role-based access control)
 *   - deleteUser() calls UserController which double-checks the role server-side
 *   - Logout button clears the session and returns to login screen
 */
public class UserListViewController implements Initializable {

    // ── FXML fields ───────────────────────────────────────────────────────────

    @FXML private TableView<User>            userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colFirstName;
    @FXML private TableColumn<User, String>  colLastName;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colPhone;
    @FXML private TableColumn<User, String>  colGender;
    @FXML private TableColumn<User, String>  colCountry;
    @FXML private TableColumn<User, String>  colNewsletter;
    @FXML private TableColumn<User, String>  colDate;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, Void>    colActions;

    @FXML private TextField searchField;
    @FXML private Label     countLabel;
    @FXML private Label     statusLabel;

    // Session banner (shows logged-in user + role)
    @FXML private Label sessionUserLabel;
    @FXML private Label sessionRoleLabel;

    // ── Non-FXML ──────────────────────────────────────────────────────────────

    private final UserController userController = new UserController();
    private final AuthController authController = new AuthController();
    private ObservableList<User> masterData;

    // ── Initializable ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSessionBanner();
        setupColumns();
        setupActionsColumn();
        loadUsers();
        setupSearch();
        hideStatus();
    }

    // ── Session banner ────────────────────────────────────────────────────────

    /**
     * Populates the top-right session banner with the current user's name
     * and a colour-coded role badge (green = admin, grey = user).
     */
    private void setupSessionBanner() {
        if (sessionUserLabel != null) {
            sessionUserLabel.setText("Logged in as: "
                    + SessionManager.getCurrentUsername());
        }
        if (sessionRoleLabel != null) {
            String role = SessionManager.getCurrentRole();
            sessionRoleLabel.setText(role != null ? role.toUpperCase() : "");
            sessionRoleLabel.getStyleClass().add(
                    SessionManager.isAdmin() ? "role-badge-admin" : "role-badge-user");
        }
    }

    // ── Column setup ──────────────────────────────────────────────────────────

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colCountry.setCellValueFactory(new PropertyValueFactory<>("country"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("registeredAt"));

        colNewsletter.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isNewsletter() ? "✓" : "✗"));

        // Role column — colour-coded cell
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(role.toUpperCase());
                    setStyle("admin".equalsIgnoreCase(role)
                        ? "-fx-text-fill: #1a73e8; -fx-font-weight: bold;"
                        : "-fx-text-fill: #555555;");
                }
            }
        });
    }

    /**
     * Builds the Actions column in Java.
     *
     * Role-based access control:
     *   - Edit   button → visible to ALL logged-in users
     *   - Delete button → visible ONLY to admins; disabled + styled differently
     *     for regular users so they can see the button but cannot use it
     */
    private void setupActionsColumn() {
        boolean isAdmin = SessionManager.isAdmin();

        colActions.setCellFactory(col -> new TableCell<>() {

            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-edit");
                box.setAlignment(Pos.CENTER);

                if (isAdmin) {
                    // Admin: delete is fully enabled
                    deleteBtn.getStyleClass().add("btn-delete");
                } else {
                    // Non-admin: delete is visually disabled
                    deleteBtn.getStyleClass().add("btn-delete-disabled");
                    deleteBtn.setDisable(true);
                    deleteBtn.setTooltip(
                        new Tooltip("Only admins can delete users"));
                }

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    openEditDialog(user);
                });

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    confirmAndDelete(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadUsers() {
        List<User> users = userController.getAllUsers();
        masterData = FXCollections.observableArrayList(users);
        updateCountLabel(masterData.size());
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        FilteredList<User> filtered = new FilteredList<>(masterData, u -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filtered.setPredicate(user -> {
                if (newVal == null || newVal.isBlank()) return true;
                String lower = newVal.toLowerCase();
                return user.getFirstName().toLowerCase().contains(lower)
                    || user.getLastName().toLowerCase().contains(lower)
                    || user.getEmail().toLowerCase().contains(lower)
                    || (user.getCountry() != null
                        && user.getCountry().toLowerCase().contains(lower));
            });
            updateCountLabel(filtered.size());
        });

        SortedList<User> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sorted);
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    private void openEditDialog(User user) {
        try {
            URL fxml = getClass().getResource(
                    "/com/learn/registration/view/edituser.fxml");
            FXMLLoader loader = new FXMLLoader(fxml);
            Scene scene = new Scene(loader.load());

            EditUserViewController editCtrl = loader.getController();
            editCtrl.setUser(user);

            Stage dialog = new Stage();
            dialog.setTitle("Edit User — " + user.getFullName());
            dialog.setScene(scene);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(userTable.getScene().getWindow());
            dialog.setResizable(false);
            dialog.showAndWait();

            refreshTable();

        } catch (IOException e) {
            showError("Could not open edit dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void confirmAndDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete " + user.getFullName() + "?");
        confirm.setContentText(
            "This action cannot be undone.\nUser: " + user.getEmail());

        Optional<ButtonType> response = confirm.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {

            // UserController.deleteUser() now returns ValidationResult
            // and also performs the role check server-side
            ValidationResult result = userController.deleteUser(user.getId());

            if (result.isSuccess()) {
                masterData.remove(user);
                updateCountLabel(userTable.getItems().size());
                showSuccess("User \"" + user.getFullName() + "\" deleted.");
            } else {
                showError(result.getMessage());
            }
        }
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void onRefresh() {
        searchField.clear();
        refreshTable();
        showSuccess("User list refreshed.");
    }

    @FXML
    private void onNewRegistration() {
        navigateTo("/com/learn/registration/view/registration.fxml",
                   App.APP_TITLE + " — Registration", 780, 720);
    }

    @FXML
    private void onBack() {
        navigateTo("/com/learn/registration/view/registration.fxml",
                   App.APP_TITLE + " — Registration", 780, 720);
    }

    /**
     * Logs out the current user, clears the session, and returns to the
     * login screen. Called by the Logout button in userlist.fxml.
     */
    @FXML
    private void onLogout() {
        authController.logout();            // clears SessionManager
        navigateTo("/com/learn/registration/view/login.fxml",
                   App.APP_TITLE + " — Login", 420, 520);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateTo(String fxmlPath, String title, double w, double h) {
        try {
            URL fxml = getClass().getResource(fxmlPath);
            Scene scene = new Scene(new FXMLLoader(fxml).load());
            Stage stage = (Stage) userTable.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setWidth(w);
            stage.setHeight(h);
            stage.setResizable(!fxmlPath.contains("login"));
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Navigation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refreshTable() {
        List<User> users = userController.getAllUsers();
        masterData.setAll(users);
        updateCountLabel(userTable.getItems().size());
    }

    private void updateCountLabel(int count) {
        if (countLabel != null)
            countLabel.setText(count + " user" + (count == 1 ? "" : "s"));
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
