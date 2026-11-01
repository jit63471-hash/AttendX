package com.attendx.controller;

import com.attendx.service.ReportService;
import com.attendx.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportController {

    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private VBox dateBox;
    @FXML private DatePicker singleDatePicker;
    @FXML private VBox monthBox;
    @FXML private ComboBox<String> monthCombo;
    @FXML private ComboBox<Integer> yearCombo;
    @FXML private Button generateBtn;

    @FXML private Label reportStatusLabel;
    @FXML private Button exportExcelBtn;
    @FXML private Button exportPdfBtn;
    @FXML private Button exportCsvBtn;
    @FXML private TableView<String[]> reportTable;

    private final ReportService reportService = new ReportService();
    private String[] currentHeaders = new String[0];
    private final List<String[]> currentData = new ArrayList<>();

    @FXML
    public void initialize() {
        reportTypeCombo.getItems().addAll(
                "Monthly Summary & 75% Rule",
                "Daily Attendance Log",
                "Shortage Deficit Report (<75%)"
        );
        reportTypeCombo.setValue("Monthly Summary & 75% Rule");

        singleDatePicker.setValue(LocalDate.now());

        // Populate months
        monthCombo.getItems().addAll("01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr", "05 - May", "06 - Jun",
                "07 - Jul", "08 - Aug", "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec");
        monthCombo.setValue(String.format("%02d - %s", LocalDate.now().getMonthValue(), LocalDate.now().getMonth().name().substring(0, 3)));

        // Populate years
        int curYear = LocalDate.now().getYear();
        for (int y = curYear - 2; y <= curYear + 1; y++) {
            yearCombo.getItems().add(y);
        }
        yearCombo.setValue(curYear);

        // Visibility toggles based on selected report type
        reportTypeCombo.setOnAction(e -> updateFilterVisibility());
        updateFilterVisibility();
    }

    private void updateFilterVisibility() {
        String type = reportTypeCombo.getValue();
        if ("Daily Attendance Log".equals(type)) {
            dateBox.setVisible(true);
            dateBox.setManaged(true);
            monthBox.setVisible(false);
            monthBox.setManaged(false);
        } else if ("Monthly Summary & 75% Rule".equals(type)) {
            dateBox.setVisible(false);
            dateBox.setManaged(false);
            monthBox.setVisible(true);
            monthBox.setManaged(true);
        } else {
            // Shortage report needs no specific date range
            dateBox.setVisible(false);
            dateBox.setManaged(false);
            monthBox.setVisible(false);
            monthBox.setManaged(false);
        }
    }

    @FXML
    public void handleGenerateReport() {
        String type = reportTypeCombo.getValue();
        generateBtn.setDisable(true);
        generateBtn.setText("Generating...");

        FxUtil.runAsync(() -> {
            if ("Daily Attendance Log".equals(type)) {
                LocalDate date = singleDatePicker.getValue() != null ? singleDatePicker.getValue() : LocalDate.now();
                String[] headers = {"Reg No", "Student Name", "Subject", "Status", "Remarks"};
                List<String[]> rows = reportService.getDailyReport(date);
                return new Object[]{headers, rows, "Daily Attendance Report for " + date};
            } else if ("Shortage Deficit Report (<75%)".equals(type)) {
                String[] headers = {"Reg No", "Student Name", "Department", "Email", "Phone", "Total Classes", "Attended", "Percentage", "Deficit Status"};
                List<String[]> rows = reportService.getShortageReport();
                return new Object[]{headers, rows, "Attendance Shortage Deficit Report (< 75%)"};
            } else {
                int month = Integer.parseInt(monthCombo.getValue().substring(0, 2));
                int year = yearCombo.getValue();
                String[] headers = {"Reg No", "Student Name", "Dept", "Total", "Present", "Absent", "Late", "Percentage", "75% Rule Status"};
                List<String[]> rows = reportService.getMonthlySummaryReport(year, month);
                return new Object[]{headers, rows, "Monthly Summary Report for " + String.format("%04d-%02d", year, month)};
            }
        },
        result -> {
            generateBtn.setDisable(false);
            generateBtn.setText("Generate Report");

            currentHeaders = (String[]) result[0];
            @SuppressWarnings("unchecked")
            List<String[]> rows = (List<String[]>) result[1];
            String reportTitle = (String) result[2];

            currentData.clear();
            currentData.addAll(rows);

            renderTable(currentHeaders, currentData);
            reportStatusLabel.setText(reportTitle + " (" + rows.size() + " records)");

            boolean hasData = !rows.isEmpty();
            exportExcelBtn.setDisable(!hasData);
            exportPdfBtn.setDisable(!hasData);
            exportCsvBtn.setDisable(!hasData);
        },
        err -> {
            generateBtn.setDisable(false);
            generateBtn.setText("Generate Report");
            AlertUtil.showError("Report Error", "Failed to generate report: " + err.getMessage());
        });
    }

    private void renderTable(String[] headers, List<String[]> data) {
        reportTable.getColumns().clear();

        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(cellData -> {
                String[] row = cellData.getValue();
                String val = (colIndex < row.length && row[colIndex] != null) ? row[colIndex] : "";
                return new SimpleStringProperty(val);
            });

            // Highlight 75% rule shortage in red
            if (headers[i].contains("Status") || headers[i].contains("Rule")) {
                col.setCellFactory(column -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            if (item.contains("SHORTAGE") || item.contains("DEFICIT")) {
                                setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                            } else if (item.contains("ELIGIBLE") || item.contains("OK")) {
                                setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                            } else {
                                setStyle("");
                            }
                        }
                    }
                });
            }

            reportTable.getColumns().add(col);
        }

        ObservableList<String[]> items = FXCollections.observableArrayList(data);
        reportTable.setItems(items);
    }

    @FXML
    public void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Report to Excel");
        fc.setInitialFileName("AttendX_Report_" + System.currentTimeMillis() + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        File file = fc.showSaveDialog(reportTable.getScene().getWindow());

        if (file != null) {
            FxUtil.runAsyncAction(
                    () -> ExcelExporter.exportReport(file, "Attendance Report", currentHeaders, currentData),
                    () -> AlertUtil.showSuccess("Export Complete", "Report successfully exported to " + file.getName()),
                    err -> AlertUtil.showError("Export Failed", "Error exporting to Excel: " + err.getMessage())
            );
        }
    }

    @FXML
    public void handleExportPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Report to PDF");
        fc.setInitialFileName("AttendX_Report_" + System.currentTimeMillis() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));
        File file = fc.showSaveDialog(reportTable.getScene().getWindow());

        if (file != null) {
            FxUtil.runAsyncAction(
                    () -> PdfExporter.exportAttendanceReport(file, reportStatusLabel.getText(), currentHeaders, currentData),
                    () -> AlertUtil.showSuccess("Export Complete", "PDF report exported to " + file.getName()),
                    err -> AlertUtil.showError("Export Failed", "Error exporting to PDF: " + err.getMessage())
            );
        }
    }

    @FXML
    public void handleExportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Report to CSV");
        fc.setInitialFileName("AttendX_Report_" + System.currentTimeMillis() + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Document (*.csv)", "*.csv"));
        File file = fc.showSaveDialog(reportTable.getScene().getWindow());

        if (file != null) {
            FxUtil.runAsyncAction(
                    () -> CsvExporter.exportToFile(file, currentHeaders, currentData),
                    () -> AlertUtil.showSuccess("Export Complete", "CSV report exported to " + file.getName()),
                    err -> AlertUtil.showError("Export Failed", "Error exporting to CSV: " + err.getMessage())
            );
        }
    }
}
