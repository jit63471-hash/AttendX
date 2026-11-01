package com.attendx.controller;

import com.attendx.model.AuditLog;
import com.attendx.model.DashboardStats;
import com.attendx.model.Student;
import com.attendx.service.DashboardService;
import com.attendx.util.FxUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private Label totalStudentsLabel;
    @FXML private Label totalFacultyLabel;
    @FXML private Label presentTodayLabel;
    @FXML private Label absentTodayLabel;
    @FXML private Label overallRateLabel;
    @FXML private Label shortageBadgeLabel;

    @FXML private BarChart<String, Number> weeklyBarChart;
    @FXML private PieChart distributionPieChart;

    @FXML private ListView<String> shortageListView;
    @FXML private ListView<String> activityListView;

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    @FXML
    public void handleRefresh() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        // Load stats cards
        FxUtil.runAsync(() -> dashboardService.getDashboardStats(),
                this::updateStatsCards,
                err -> System.err.println("Error loading stats: " + err.getMessage())
        );

        // Load weekly attendance chart
        FxUtil.runAsync(() -> dashboardService.getWeeklyAttendance(),
                this::updateWeeklyChart,
                err -> System.err.println("Error loading weekly chart: " + err.getMessage())
        );

        // Load shortage alerts
        FxUtil.runAsync(() -> dashboardService.getShortageStudents(),
                this::updateShortageList,
                err -> System.err.println("Error loading shortage list: " + err.getMessage())
        );

        // Load recent activity logs
        FxUtil.runAsync(() -> dashboardService.getRecentActivities(10),
                this::updateActivityList,
                err -> System.err.println("Error loading audit activities: " + err.getMessage())
        );
    }

    private void updateStatsCards(DashboardStats stats) {
        totalStudentsLabel.setText(String.valueOf(stats.getTotalStudents()));
        totalFacultyLabel.setText(String.valueOf(stats.getTotalFaculty()));
        presentTodayLabel.setText(String.valueOf(stats.getPresentToday()));
        absentTodayLabel.setText(String.valueOf(stats.getAbsentToday()));
        overallRateLabel.setText(String.format("%.1f%%", stats.getOverallAttendancePercent()));
        shortageBadgeLabel.setText(stats.getShortageCount() + " Shortage Alerts (<75%)");

        // Update distribution Pie Chart
        int present = stats.getPresentToday();
        int absent = stats.getAbsentToday();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (present == 0 && absent == 0) {
            pieData.add(new PieChart.Data("No Data Today", 1));
        } else {
            if (present > 0) pieData.add(new PieChart.Data("Present (" + present + ")", present));
            if (absent > 0) pieData.add(new PieChart.Data("Absent (" + absent + ")", absent));
        }
        distributionPieChart.setData(pieData);
    }

    private void updateWeeklyChart(List<Map<String, Object>> weeklyData) {
        weeklyBarChart.getData().clear();

        XYChart.Series<String, Number> presentSeries = new XYChart.Series<>();
        presentSeries.setName("Present");

        XYChart.Series<String, Number> absentSeries = new XYChart.Series<>();
        absentSeries.setName("Absent");

        for (Map<String, Object> day : weeklyData) {
            String dayLabel = (String) day.get("day");
            int present = (int) day.get("present");
            int absent = (int) day.get("absent");

            presentSeries.getData().add(new XYChart.Data<>(dayLabel, present));
            absentSeries.getData().add(new XYChart.Data<>(dayLabel, absent));
        }

        weeklyBarChart.getData().addAll(presentSeries, absentSeries);
    }

    private void updateShortageList(List<Student> shortageStudents) {
        ObservableList<String> items = FXCollections.observableArrayList();
        if (shortageStudents.isEmpty()) {
            items.add("✓ All active students are maintaining 75%+ attendance!");
        } else {
            for (Student s : shortageStudents) {
                items.add("⚠️  " + s.getRegistrationNumber() + " - " + s.getFullName() + " (" + s.getEmail() + ")");
            }
        }
        shortageListView.setItems(items);
    }

    private void updateActivityList(List<AuditLog> auditLogs) {
        ObservableList<String> items = FXCollections.observableArrayList();
        if (auditLogs.isEmpty()) {
            items.add("No recent activities recorded.");
        } else {
            for (AuditLog log : auditLogs) {
                items.add("• " + log.getAction() + " on " + log.getEntityType() + " by " + log.getUsername() + " - " + (log.getNewValues() != null ? log.getNewValues() : ""));
            }
        }
        activityListView.setItems(items);
    }
}
