package com.attendx;

import com.attendx.config.AppConfig;
import com.attendx.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    @Override
    public void init() throws Exception {
        log.info("Starting {} v{} - {}", AppConfig.APP_NAME, AppConfig.APP_VERSION, AppConfig.APP_TAGLINE);
        // Pre-warm database connection pool
        DatabaseConfig.getInstance();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 750);
            scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/theme-light.css").toExternalForm());

            primaryStage.setTitle(AppConfig.APP_NAME + " – " + AppConfig.APP_TAGLINE);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

            log.info("{} GUI launched successfully", AppConfig.APP_NAME);
        } catch (Exception e) {
            log.error("Fatal error during application startup", e);
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        log.info("Shutting down {}...", AppConfig.APP_NAME);
        DatabaseConfig.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
