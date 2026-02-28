package com.lubesoft;

import com.lubesoft.db.DatabaseManager;
import com.lubesoft.db.DatabaseInitializer;
import com.lubesoft.service.LicenseService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Initialize database
        try {
            DatabaseManager.getInstance().getConnection().close();
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
        DatabaseInitializer.initialize();

        // Check license
        LicenseService licenseService = LicenseService.getInstance();
        boolean activated = licenseService.isActivated();
        boolean trialExpired = licenseService.isTrialExpired();

        if (!activated && trialExpired) {
            loadScene("/com/lubesoft/fxml/activation.fxml", "LubeSoft - Activate License", 600, 400);
        } else {
            loadScene("/com/lubesoft/fxml/login.fxml", "LubeSoft - Login", 480, 360);
        }

        stage.setTitle("LubeSoft POS");
        stage.show();
    }

    public static void loadScene(String fxmlPath, String title, double width, double height) throws IOException {
        URL resource = App.class.getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("FXML not found: " + fxmlPath);
        }
        FXMLLoader loader = new FXMLLoader(resource);
        Scene scene = new Scene(loader.load(), width, height);
        URL css = App.class.getResource("/com/lubesoft/css/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        primaryStage.setScene(scene);
        primaryStage.setTitle(title);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
