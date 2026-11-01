package com.attendx.controller;

import com.attendx.model.Department;
import com.attendx.model.Student;
import com.attendx.service.DepartmentService;
import com.attendx.service.StudentService;
import com.attendx.util.AlertUtil;
import com.attendx.util.ExcelExporter;
import com.attendx.util.FxUtil;
import com.attendx.util.PdfExporter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StudentController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label studentCountLabel;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, String> colRegNo;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, String> colPhone;
    @FXML private TableColumn<Student, String> colDept;
    @FXML private TableColumn<Student, String> colCourse;
    @FXML private TableColumn<Student, String> colSemester;
    @FXML private TableColumn<Student, String> colSection;
    @FXML private TableColumn<Student, String> colStatus;
    @FXML private TableColumn<Student, Void> colActions;

    private final StudentService studentService = new StudentService();
    private final DepartmentService deptService = new DepartmentService();

    private final ObservableList<Student> masterData = FXCollections.observableArrayList();
    private FilteredList<Student> filteredData;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        loadStudents();
    }

    private void setupTableColumns() {
        colRegNo.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colSemester.setCellValueFactory(new PropertyValueFactory<>("semesterLabel"));
        colSection.setCellValueFactory(new PropertyValueFactory<>("section"));

        // Status Badge
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Student s = getTableRow().getItem();
                    Label badge = new Label(s.isActive() ? "Active" : "Archived");
                    badge.getStyleClass().add(s.isActive() ? "status-present" : "status-absent");
                    setGraphic(badge);
                }
            }
        });

        // Action Buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button archiveBtn = new Button("Archive");
            private final HBox pane = new HBox(6, editBtn, archiveBtn);

            {
                pane.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("secondary-btn");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                archiveBtn.getStyleClass().add("secondary-btn");
                archiveBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");

                editBtn.setOnAction(e -> {
                    Student s = getTableRow().getItem();
                    if (s != null) openStudentDialog(s);
                });

                archiveBtn.setOnAction(e -> {
                    Student s = getTableRow().getItem();
                    if (s != null) {
                        if (s.isActive()) {
                            boolean ok = AlertUtil.showConfirmation("Archive Student", "Archive student " + s.getFullName() + "?");
                            if (ok) {
                                studentService.archiveStudent(s.getId());
                                loadStudents();
                            }
                        } else {
                            studentService.restoreStudent(s.getId());
                            loadStudents();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Student s = getTableRow().getItem();
                    archiveBtn.setText(s.isActive() ? "Archive" : "Restore");
                    setGraphic(pane);
                }
            }
        });
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("All Statuses", "Active Only", "Archived Only");
        statusFilter.setValue("Active Only");

        // Load departments into filter
        FxUtil.runAsync(() -> deptService.getAllDepartments(),
                depts -> {
                    departmentFilter.getItems().add("All Departments");
                    for (Department d : depts) {
                        departmentFilter.getItems().add(d.getName());
                    }
                    departmentFilter.setValue("All Departments");
                },
                null
        );

        filteredData = new FilteredList<>(masterData, p -> true);
        studentTable.setItems(filteredData);

        // Bind filter events
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        departmentFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void applyFilter() {
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String dept = departmentFilter.getValue();
        String status = statusFilter.getValue();

        filteredData.setPredicate(s -> {
            // Status check
            if ("Active Only".equals(status) && !s.isActive()) return false;
            if ("Archived Only".equals(status) && s.isActive()) return false;

            // Dept check
            if (dept != null && !"All Departments".equals(dept)) {
                if (s.getDepartmentName() == null || !s.getDepartmentName().equalsIgnoreCase(dept)) {
                    return false;
                }
            }

            // Search text check
            if (!searchText.isEmpty()) {
                boolean matchesRegNo = s.getRegistrationNumber() != null && s.getRegistrationNumber().toLowerCase().contains(searchText);
                boolean matchesName = s.getFullName() != null && s.getFullName().toLowerCase().contains(searchText);
                boolean matchesEmail = s.getEmail() != null && s.getEmail().toLowerCase().contains(searchText);
                return matchesRegNo || matchesName || matchesEmail;
            }

            return true;
        });

        studentCountLabel.setText("Showing " + filteredData.size() + " students");
    }

    public void loadStudents() {
        loadingIndicator.setVisible(true);
        FxUtil.runAsync(() -> studentService.getAllStudents(),
                students -> {
                    masterData.setAll(students);
                    loadingIndicator.setVisible(false);
                    applyFilter();
                },
                err -> {
                    loadingIndicator.setVisible(false);
                    AlertUtil.showError("Loading Error", "Failed to retrieve student records: " + err.getMessage());
                }
        );
    }

    @FXML
    public void handleOpenAddDialog() {
        openStudentDialog(null);
    }

    private void openStudentDialog(Student student) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student_form.fxml"));
            Parent root = loader.load();

            StudentFormController controller = loader.getController();
            controller.setStudent(student, this::loadStudents);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(student == null ? "Register New Student" : "Edit Student Profile");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(studentTable.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (Exception e) {
            AlertUtil.showError("Dialog Error", "Could not open student form: " + e.getMessage());
        }
    }

    @FXML
    public void handleImportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Students from Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xlsx, *.xls)", "*.xlsx", "*.xls"));
        File file = fileChooser.showOpenDialog(studentTable.getScene().getWindow());

        if (file != null) {
            FxUtil.runAsync(() -> studentService.importFromExcel(file, 1, 1, 1, "2025-2026"),
                    count -> {
                        AlertUtil.showSuccess("Import Successful", "Successfully imported " + count + " students.");
                        loadStudents();
                    },
                    err -> AlertUtil.showError("Import Failed", "Excel import error: " + err.getMessage())
            );
        }
    }

    @FXML
    public void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Students to Excel");
        fileChooser.setInitialFileName("AttendX_Students_" + System.currentTimeMillis() + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        File file = fileChooser.showSaveDialog(studentTable.getScene().getWindow());

        if (file != null) {
            FxUtil.runAsyncAction(
                    () -> ExcelExporter.exportStudents(file, new ArrayList<>(filteredData)),
                    () -> AlertUtil.showSuccess("Export Complete", "Student directory exported to " + file.getName()),
                    err -> AlertUtil.showError("Export Failed", "Error exporting to Excel: " + err.getMessage())
            );
        }
    }

    @FXML
    public void handleExportPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Student Directory to PDF");
        fileChooser.setInitialFileName("AttendX_Students_" + System.currentTimeMillis() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));
        File file = fileChooser.showSaveDialog(studentTable.getScene().getWindow());

        if (file != null) {
            String[] headers = {"Reg No", "Full Name", "Email", "Department", "Course", "Semester", "Section", "Status"};
            List<String[]> rows = new ArrayList<>();
            for (Student s : filteredData) {
                rows.add(new String[]{
                        s.getRegistrationNumber(),
                        s.getFullName(),
                        s.getEmail(),
                        s.getDepartmentName() != null ? s.getDepartmentName() : "",
                        s.getCourseName() != null ? s.getCourseName() : "",
                        s.getSemesterLabel() != null ? s.getSemesterLabel() : "",
                        s.getSection() != null ? s.getSection() : "A",
                        s.isActive() ? "Active" : "Archived"
                });
            }

            FxUtil.runAsyncAction(
                    () -> PdfExporter.exportAttendanceReport(file, "Student Directory & Enrollment Roster", headers, rows),
                    () -> AlertUtil.showSuccess("Export Complete", "Student roster exported to " + file.getName()),
                    err -> AlertUtil.showError("Export Failed", "Error exporting to PDF: " + err.getMessage())
            );
        }
    }
}
