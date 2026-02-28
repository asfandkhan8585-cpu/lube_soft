package com.lubesoft.controller;

import com.lubesoft.dao.TimeEntryDAO;
import com.lubesoft.dao.UserDAO;
import com.lubesoft.model.TimeEntry;
import com.lubesoft.model.User;
import com.lubesoft.service.AuthService;
import com.lubesoft.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EmployeeController {

    // Time clock section
    @FXML private Label clockStatusLabel;
    @FXML private Button clockInButton;
    @FXML private Button clockOutButton;

    // Time entries table
    @FXML private TableView<TimeEntry> timeEntriesTable;
    @FXML private TableColumn<TimeEntry, String> colClockIn;
    @FXML private TableColumn<TimeEntry, String> colClockOut;

    // Employee management (ADMIN only)
    @FXML private TableView<User> employeesTable;
    @FXML private TableColumn<User, String> colEmpUsername;
    @FXML private TableColumn<User, String> colEmpRole;
    @FXML private TableColumn<User, Boolean> colEmpActive;

    // Employee form
    @FXML private TextField empUsernameField;
    @FXML private PasswordField empPasswordField;
    @FXML private ComboBox<String> empRoleCombo;
    @FXML private CheckBox empActiveCheck;

    private final TimeEntryDAO timeEntryDAO = new TimeEntryDAO();
    private final UserDAO userDAO = new UserDAO();
    private TimeEntry activeEntry;
    private User selectedEmployee;

    @FXML
    public void initialize() {
        setupTables();
        loadCurrentUserClock();
        loadTimeEntries();

        if (AuthService.hasPermission("EMPLOYEES_MANAGE")) {
            loadEmployees();
        } else if (employeesTable != null) {
            employeesTable.setVisible(false);
        }

        if (empRoleCombo != null) {
            empRoleCombo.setItems(FXCollections.observableArrayList("ADMIN", "MANAGER", "TECHNICIAN"));
        }
    }

    private void setupTables() {
        if (colClockIn != null) colClockIn.setCellValueFactory(new PropertyValueFactory<>("clockIn"));
        if (colClockOut != null) colClockOut.setCellValueFactory(new PropertyValueFactory<>("clockOut"));

        if (colEmpUsername != null) colEmpUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colEmpRole != null) colEmpRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        if (employeesTable != null) {
            employeesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) populateEmployeeForm(newVal);
            });
        }
    }

    private void loadCurrentUserClock() {
        var user = AuthService.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                Optional<TimeEntry> activeOpt = timeEntryDAO.findActiveEntry(user.getId());
                Platform.runLater(() -> {
                    if (activeOpt.isPresent()) {
                        activeEntry = activeOpt.get();
                        clockStatusLabel.setText("Clocked in since: " + activeEntry.getClockIn());
                        if (clockInButton != null) clockInButton.setDisable(true);
                        if (clockOutButton != null) clockOutButton.setDisable(false);
                    } else {
                        clockStatusLabel.setText("Not clocked in");
                        if (clockInButton != null) clockInButton.setDisable(false);
                        if (clockOutButton != null) clockOutButton.setDisable(true);
                    }
                });
            } catch (SQLException e) {
                // ignore
            }
        }).start();
    }

    private void loadTimeEntries() {
        var user = AuthService.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                List<TimeEntry> entries = timeEntryDAO.findByUser(user.getId());
                Platform.runLater(() -> {
                    if (timeEntriesTable != null)
                        timeEntriesTable.setItems(FXCollections.observableArrayList(entries));
                });
            } catch (SQLException e) {
                // ignore
            }
        }).start();
    }

    @FXML
    private void handleClockIn() {
        var user = AuthService.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                TimeEntry entry = timeEntryDAO.clockIn(user.getId(), "");
                Platform.runLater(() -> {
                    activeEntry = entry;
                    clockStatusLabel.setText("Clocked in since: " + entry.getClockIn());
                    if (clockInButton != null) clockInButton.setDisable(true);
                    if (clockOutButton != null) clockOutButton.setDisable(false);
                    loadTimeEntries();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleClockOut() {
        if (activeEntry == null) return;

        new Thread(() -> {
            try {
                timeEntryDAO.clockOut(activeEntry.getId());
                Platform.runLater(() -> {
                    activeEntry = null;
                    clockStatusLabel.setText("Not clocked in");
                    if (clockInButton != null) clockInButton.setDisable(false);
                    if (clockOutButton != null) clockOutButton.setDisable(true);
                    loadTimeEntries();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> AlertUtil.showError("Error", e.getMessage()));
            }
        }).start();
    }

    private void loadEmployees() {
        new Thread(() -> {
            try {
                List<User> users = userDAO.findAll();
                Platform.runLater(() -> {
                    if (employeesTable != null)
                        employeesTable.setItems(FXCollections.observableArrayList(users));
                });
            } catch (SQLException e) {
                // ignore
            }
        }).start();
    }

    @FXML
    private void handleSaveEmployee() {
        if (!AuthService.hasPermission("EMPLOYEES_MANAGE")) {
            AlertUtil.showError("Permission Denied", "You do not have permission to manage employees.");
            return;
        }
        try {
            User u = selectedEmployee != null ? selectedEmployee : new User();
            u.setUsername(empUsernameField.getText().trim());
            if (empPasswordField != null && !empPasswordField.getText().isEmpty()) {
                u.setPasswordHash(BCrypt.hashpw(empPasswordField.getText(), BCrypt.gensalt()));
            }
            u.setRole(empRoleCombo != null ? empRoleCombo.getValue() : "TECHNICIAN");
            u.setActive(empActiveCheck == null || empActiveCheck.isSelected());

            userDAO.save(u);
            AlertUtil.showInfo("Saved", "Employee saved.");
            clearEmployeeForm();
            loadEmployees();
        } catch (SQLException e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleNewEmployee() {
        selectedEmployee = null;
        clearEmployeeForm();
    }

    private void populateEmployeeForm(User u) {
        selectedEmployee = u;
        if (empUsernameField != null) empUsernameField.setText(u.getUsername());
        if (empPasswordField != null) empPasswordField.clear();
        if (empRoleCombo != null) empRoleCombo.setValue(u.getRole());
        if (empActiveCheck != null) empActiveCheck.setSelected(u.isActive());
    }

    private void clearEmployeeForm() {
        if (empUsernameField != null) empUsernameField.clear();
        if (empPasswordField != null) empPasswordField.clear();
        if (empRoleCombo != null) empRoleCombo.setValue("TECHNICIAN");
        if (empActiveCheck != null) empActiveCheck.setSelected(true);
    }
}
