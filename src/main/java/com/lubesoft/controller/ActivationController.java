package com.lubesoft.controller;

import com.lubesoft.App;
import com.lubesoft.service.LicenseService;
import com.lubesoft.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ActivationController {

    @FXML private Label machineIdLabel;
    @FXML private TextField licenseKeyField;
    @FXML private Label statusLabel;
    @FXML private Button activateButton;

    private final LicenseService licenseService = LicenseService.getInstance();

    @FXML
    public void initialize() {
        String machineId = licenseService.generateMachineId();
        machineIdLabel.setText(machineId);

        if (licenseService.isActivated()) {
            statusLabel.setText("Status: ACTIVATED");
            licenseKeyField.setDisable(true);
            activateButton.setDisable(true);
        } else {
            long daysLeft = licenseService.getTrialDaysRemaining();
            statusLabel.setText("Trial: " + daysLeft + " day(s) remaining");
        }
    }

    @FXML
    private void handleActivate() {
        String key = licenseKeyField.getText().trim();
        if (key.isEmpty()) {
            AlertUtil.showWarning("Empty Key", "Please enter a license key.");
            return;
        }

        boolean success = licenseService.activate(key);
        if (success) {
            AlertUtil.showInfo("Activated", "License activated successfully! Please log in.");
            try {
                App.loadScene("/com/lubesoft/fxml/login.fxml", "LubeSoft - Login", 480, 360);
            } catch (Exception e) {
                AlertUtil.showError("Error", "Could not navigate to login.");
            }
        } else {
            AlertUtil.showError("Activation Failed", "Invalid license key for this machine.");
        }
    }

    @FXML
    private void handleCopyMachineId() {
        String machineId = machineIdLabel.getText();
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(machineId);
        clipboard.setContent(content);
        AlertUtil.showInfo("Copied", "Machine ID copied to clipboard.");
    }

    @FXML
    private void handleSkip() {
        if (licenseService.isTrialExpired()) {
            AlertUtil.showWarning("Trial Expired", "Your trial has expired. Please activate a license.");
            return;
        }
        try {
            App.loadScene("/com/lubesoft/fxml/login.fxml", "LubeSoft - Login", 480, 360);
        } catch (Exception e) {
            AlertUtil.showError("Error", "Could not navigate to login.");
        }
    }
}
