package com.attendx.controller;

import com.attendx.exception.ValidationException;
import com.attendx.model.Course;
import com.attendx.model.Department;
import com.attendx.model.Semester;
import com.attendx.model.Student;
import com.attendx.service.DepartmentService;
import com.attendx.service.StudentService;
import com.attendx.util.DateUtil;
import com.attendx.util.FxUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class StudentFormController {

    @FXML private Label formTitleLabel;
    @FXML private TextField regNoField;
    @FXML private TextField academicYearField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> departmentCombo;
    @FXML private ComboBox<String> courseCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private TextField sectionField;
    @FXML private Label errorLabel;
    @FXML private Button saveBtn;

    private final StudentService studentService = new StudentService();
    private final DepartmentService deptService = new DepartmentService();

    private final Map<String, Integer> deptMap = new HashMap<>();
    private final Map<String, Integer> courseMap = new HashMap<>();
    private final Map<String, Integer> semMap = new HashMap<>();

    private Student studentToEdit;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        academicYearField.setText(DateUtil.getCurrentAcademicYear());

        // Load departments
        FxUtil.runAsync(() -> deptService.getAllDepartments(),
                depts -> {
                    deptMap.clear();
                    departmentCombo.getItems().clear();
                    for (Department d : depts) {
                        deptMap.put(d.getName(), d.getId());
                        departmentCombo.getItems().add(d.getName());
                    }
                    if (studentToEdit != null && studentToEdit.getDepartmentName() != null) {
                        departmentCombo.setValue(studentToEdit.getDepartmentName());
                    }
                },
                null
        );

        // When Department changes, load its Courses
        departmentCombo.setOnAction(e -> {
            String selectedDept = departmentCombo.getValue();
            if (selectedDept != null && deptMap.containsKey(selectedDept)) {
                int deptId = deptMap.get(selectedDept);
                FxUtil.runAsync(() -> deptService.getCoursesByDepartment(deptId),
                        courses -> {
                            courseMap.clear();
                            courseCombo.getItems().clear();
                            for (Course c : courses) {
                                courseMap.put(c.getName(), c.getId());
                                courseCombo.getItems().add(c.getName());
                            }
                            if (studentToEdit != null && studentToEdit.getCourseName() != null) {
                                courseCombo.setValue(studentToEdit.getCourseName());
                            }
                        },
                        null
                );
            }
        });

        // Load semesters
        FxUtil.runAsync(() -> deptService.getAllSemesters(),
                semesters -> {
                    semMap.clear();
                    semesterCombo.getItems().clear();
                    for (Semester s : semesters) {
                        semMap.put(s.getLabel(), s.getId());
                        semesterCombo.getItems().add(s.getLabel());
                    }
                    if (studentToEdit != null && studentToEdit.getSemesterLabel() != null) {
                        semesterCombo.setValue(studentToEdit.getSemesterLabel());
                    }
                },
                null
        );
    }

    public void setStudent(Student student, Runnable callback) {
        this.studentToEdit = student;
        this.onSaveCallback = callback;

        if (student != null) {
            formTitleLabel.setText("Edit Student Profile");
            regNoField.setText(student.getRegistrationNumber());
            regNoField.setDisable(true); // Don't change PK registration number in edit
            firstNameField.setText(student.getFirstName());
            lastNameField.setText(student.getLastName());
            emailField.setText(student.getEmail());
            phoneField.setText(student.getPhone());
            sectionField.setText(student.getSection());
            if (student.getAcademicYear() != null) {
                academicYearField.setText(student.getAcademicYear());
            }
            if (student.getDepartmentName() != null) {
                departmentCombo.setValue(student.getDepartmentName());
            }
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setVisible(false);

        String regNo = regNoField.getText();
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String section = sectionField.getText();
        String academicYear = academicYearField.getText();

        String deptName = departmentCombo.getValue();
        String courseName = courseCombo.getValue();
        String semName = semesterCombo.getValue();

        int deptId = deptName != null && deptMap.containsKey(deptName) ? deptMap.get(deptName) : 0;
        int courseId = courseName != null && courseMap.containsKey(courseName) ? courseMap.get(courseName) : 0;
        int semId = semName != null && semMap.containsKey(semName) ? semMap.get(semName) : 0;

        Student s = studentToEdit != null ? studentToEdit : new Student();
        s.setRegistrationNumber(regNo);
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setEmail(email);
        s.setPhone(phone);
        s.setSection(section != null && !section.isEmpty() ? section : "A");
        s.setAcademicYear(academicYear);
        s.setDepartmentId(deptId);
        s.setCourseId(courseId);
        s.setSemesterId(semId);

        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");

        FxUtil.runAsync(() -> {
            if (studentToEdit == null) {
                return studentService.createStudent(s);
            } else {
                return studentService.updateStudent(s);
            }
        },
        success -> {
            saveBtn.setDisable(false);
            if (onSaveCallback != null) onSaveCallback.run();
            closeDialog();
        },
        err -> {
            saveBtn.setDisable(false);
            saveBtn.setText("Save Student");
            errorLabel.setText(err instanceof ValidationException ? err.getMessage() : "Save failed: " + err.getMessage());
            errorLabel.setVisible(true);
        });
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }
}
