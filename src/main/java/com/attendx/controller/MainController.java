package com.attendx.controller;

import com.attendx.model.User;
import com.attendx.security.SessionManager;
import com.attendx.service.AuthService;
import com.attendx.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class MainController {

    @FXML private Button navDashboard;
    @FXML private Button navStudents;
    @FXML private Button navFaculty;
    @FXML private Button navAttendance;
    @FXML private Button navReports;

    @FXML private Label userNameLabel;
    @FXML private Label userRoleBadge;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label currentDateLabel;
    @FXML private ToggleButton themeToggle;
    @FXML private StackPane contentArea;

    private final AuthService authService = new AuthService();
    private boolean isDarkTheme = false;

    @FXML
    public void initialize() {
        // Set user info
        Optional<User> userOpt = SessionManager.getInstance().getCurrentUser();
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userNameLabel.setText(user.getFullName());
            userRoleBadge.setText(user.getRoleName().toUpperCase());

            if ("faculty".equalsIgnoreCase(user.getRoleName())) {
                userRoleBadge.getStyleClass().clear();
                userRoleBadge.getStyleClass().add("badge-faculty");
                // Faculty members see relevant views
                navFaculty.setVisible(false);
                navFaculty.setManaged(false);
            }
        }

        // Display current date
        LocalDate now = LocalDate.now();
        currentDateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));

        // Load dashboard by default
        handleNavDashboard();
    }

    @FXML
    public void handleNavDashboard() {
        setActiveNav(navDashboard);
        setPageHeader("Dashboard Overview", "Real-time institutional attendance analytics & trends");
        loadView("/fxml/dashboard.fxml");
    }

    @FXML
    public void handleNavStudents() {
        setActiveNav(navStudents);
        setPageHeader("Student Management", "Manage student profiles, enrollments, and academic records");
        loadView("/fxml/students.fxml");
    }

    @FXML
    public void handleNavFaculty() {
        if (!SessionManager.getInstance().isAdmin()) {
            AlertUtil.showWarning("Access Restricted", "Faculty management is restricted to Administrators.");
            return;
        }
        setActiveNav(navFaculty);
        setPageHeader("Faculty Directory", "Manage teaching staff, departments, and course assignments");
        loadView("/fxml/faculty.fxml");
    }

    @FXML
    public void handleNavAttendance() {
        setActiveNav(navAttendance);
        setPageHeader("Mark Attendance", "Bulk session attendance recording with duplicate prevention");
        loadView("/fxml/attendance.fxml");
    }

    @FXML
    public void handleNavReports() {
        setActiveNav(navReports);
        setPageHeader("Reports & Analytics", "Institutional attendance summaries, shortage alerts, and exports");
        loadView("/fxml/reports.fxml");
    }

    @FXML
    public void handleToggleTheme() {
        Scene scene = contentArea.getScene();
        if (scene == null) return;

        isDarkTheme = !isDarkTheme;
        scene.getStylesheets().removeIf(url -> url.contains("theme-light.css") || url.contains("theme-dark.css"));

        if (isDarkTheme) {
            scene.getStylesheets().add(getClass().getResource("/css/theme-dark.css").toExternalForm());
            themeToggle.setText("☀️ Light Mode");
        } else {
            scene.getStylesheets().add(getClass().getResource("/css/theme-light.css").toExternalForm());
            themeToggle.setText("🌙 Dark Mode");
        }
    }

    @FXML
    public void handleLogout() {
        boolean confirmed = AlertUtil.showConfirmation("Sign Out", "Are you sure you want to sign out of AttendX?");
        if (confirmed) {
            authService.logout();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) contentArea.getScene().getWindow();
                Scene scene = new Scene(root, 1200, 750);
                scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/css/theme-light.css").toExternalForm());
                stage.setScene(scene);
                stage.centerOnScreen();
            } catch (IOException e) {
                AlertUtil.showError("Logout Error", "Failed to load login screen: " + e.getMessage());
            }
        }
    }

    private void setActiveNav(Button activeBtn) {
        Button[] buttons = {navDashboard, navStudents, navFaculty, navAttendance, navReports};
        for (Button btn : buttons) {
            btn.getStyleClass().remove("active");
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
            activeBtn.getStyleClass().add("active");
        }
    }

    private void setPageHeader(String title, String subtitle) {
        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Could not load view " + fxmlPath + ": " + e.getMessage());
        }
    }
}
