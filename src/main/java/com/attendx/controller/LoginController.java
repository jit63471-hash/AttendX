package com.attendx.controller;

import com.attendx.exception.AuthException;
import com.attendx.model.User;
import com.attendx.service.AuthService;
import com.attendx.util.FxUtil;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {

    @FXML private VBox cardBox;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private CheckBox showPasswordCheck;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        // Sync password field with text field
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordTextField.isVisible()) {
                passwordTextField.setText(newVal);
            }
        });
        passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passwordTextField.isVisible()) {
                passwordField.setText(newVal);
            }
        });

        // Enter key to login
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        passwordTextField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleToggleShowPassword() {
        if (showPasswordCheck.isSelected()) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
            passwordTextField.requestFocus();
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordTextField.setVisible(false);
            passwordField.requestFocus();
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = showPasswordCheck.isSelected() ? passwordTextField.getText() : passwordField.getText();

        errorLabel.setVisible(false);
        loginBtn.setDisable(true);
        loginBtn.setText("Authenticating...");

        FxUtil.runAsync(() -> authService.login(username, password),
                user -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Sign In");
                    openMainDashboard(user);
                },
                error -> {
                    loginBtn.setDisable(false);
                    loginBtn.setText("Sign In");
                    showError(error instanceof AuthException ? error.getMessage() : "Authentication failed. " + error.getMessage());
                    shakeCard();
                }
        );
    }

    private void openMainDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginBtn.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/theme-light.css").toExternalForm());

            stage.setTitle("AttendX – Smart Attendance Management System");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            showError("Failed to load application workspace: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void shakeCard() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(cardBox.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(50), new KeyValue(cardBox.translateXProperty(), -10)),
                new KeyFrame(Duration.millis(100), new KeyValue(cardBox.translateXProperty(), 10)),
                new KeyFrame(Duration.millis(150), new KeyValue(cardBox.translateXProperty(), -8)),
                new KeyFrame(Duration.millis(200), new KeyValue(cardBox.translateXProperty(), 8)),
                new KeyFrame(Duration.millis(250), new KeyValue(cardBox.translateXProperty(), -4)),
                new KeyFrame(Duration.millis(300), new KeyValue(cardBox.translateXProperty(), 0))
        );
        timeline.play();
    }
}
