package com.lubesoft.controller;

import com.lubesoft.App;
import com.lubesoft.service.AuthService;
import com.lubesoft.service.LicenseService;
import com.lubesoft.util.AlertUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label trialLabel;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);

        // Show trial status
        LicenseService ls = LicenseService.getInstance();
        if (!ls.isActivated()) {
            long daysLeft = ls.getTrialDaysRemaining();
            trialLabel.setText("Trial: " + daysLeft + " day(s) remaining");
            trialLabel.setVisible(true);
        } else {
            trialLabel.setVisible(false);
        }

        // Allow pressing Enter in password field
        passwordField.setOnAction(this::handleLogin);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        // Run authentication in background thread
        new Thread(() -> {
            AuthService authService = AuthService.getInstance();
            var user = authService.login(username, password);

            Platform.runLater(() -> {
                loginButton.setDisable(false);
                if (user != null) {
                    try {
                        App.loadScene("/com/lubesoft/fxml/main_layout.fxml", "LubeSoft POS", 1280, 800);
                        App.getPrimaryStage().setMaximized(true);
                    } catch (Exception e) {
                        showError("Failed to load main screen: " + e.getMessage());
                    }
                } else {
                    showError("Invalid username or password.");
                    passwordField.clear();
                }
            });
        }).start();
    }

    @FXML
    private void handleActivate(ActionEvent event) {
        try {
            App.loadScene("/com/lubesoft/fxml/activation.fxml", "LubeSoft - Activate License", 600, 400);
        } catch (Exception e) {
            showError("Could not open activation screen.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
