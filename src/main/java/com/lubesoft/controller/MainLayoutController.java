package com.lubesoft.controller;

import com.lubesoft.App;
import com.lubesoft.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;

public class MainLayoutController {

    @FXML private BorderPane rootPane;
    @FXML private Label userLabel;
    @FXML private VBox sidebar;

    // Sidebar nav buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnPOS;
    @FXML private Button btnInventory;
    @FXML private Button btnCustomers;
    @FXML private Button btnReports;
    @FXML private Button btnEmployees;

    @FXML
    public void initialize() {
        var user = AuthService.getCurrentUser();
        if (user != null) {
            userLabel.setText(user.getUsername() + " (" + user.getRole() + ")");
        }

        // Hide buttons based on role
        if (!AuthService.hasPermission("INVENTORY")) {
            btnInventory.setVisible(false);
            btnInventory.setManaged(false);
        }
        if (!AuthService.hasPermission("REPORTS")) {
            btnReports.setVisible(false);
            btnReports.setManaged(false);
        }
        if (!AuthService.hasPermission("EMPLOYEES_VIEW")) {
            btnEmployees.setVisible(false);
            btnEmployees.setManaged(false);
        }

        // Default to dashboard
        loadView("dashboard");
    }

    @FXML private void showDashboard() { loadView("dashboard"); }
    @FXML private void showPOS() { loadView("pos"); }
    @FXML private void showInventory() { loadView("inventory"); }
    @FXML private void showCustomers() { loadView("customers"); }
    @FXML private void showReports() { loadView("reports"); }
    @FXML private void showEmployees() { loadView("employees"); }

    @FXML
    private void handleLogout() {
        AuthService.getInstance().logout();
        try {
            App.loadScene("/com/lubesoft/fxml/login.fxml", "LubeSoft - Login", 480, 360);
            App.getPrimaryStage().setMaximized(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String viewName) {
        try {
            URL resource = getClass().getResource("/com/lubesoft/fxml/" + viewName + ".fxml");
            if (resource == null) {
                System.err.println("FXML not found: " + viewName);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            rootPane.setCenter(view);
            highlightActiveButton(viewName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void highlightActiveButton(String viewName) {
        for (Button btn : new Button[]{btnDashboard, btnPOS, btnInventory, btnCustomers, btnReports, btnEmployees}) {
            btn.getStyleClass().remove("sidebar-active");
        }
        switch (viewName) {
            case "dashboard" -> btnDashboard.getStyleClass().add("sidebar-active");
            case "pos"       -> btnPOS.getStyleClass().add("sidebar-active");
            case "inventory" -> btnInventory.getStyleClass().add("sidebar-active");
            case "customers" -> btnCustomers.getStyleClass().add("sidebar-active");
            case "reports"   -> btnReports.getStyleClass().add("sidebar-active");
            case "employees" -> btnEmployees.getStyleClass().add("sidebar-active");
        }
    }
}
