package com.attendx.controller;

import com.attendx.exception.ValidationException;
import com.attendx.model.Department;
import com.attendx.model.Faculty;
import com.attendx.service.DepartmentService;
import com.attendx.service.FacultyService;
import com.attendx.util.FxUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class FacultyFormController {

    @FXML private Label formTitleLabel;
    @FXML private TextField empIdField;
    @FXML private TextField designationField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> departmentCombo;
    @FXML private VBox passwordBox;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button saveBtn;

    private final FacultyService facultyService = new FacultyService();
    private final DepartmentService deptService = new DepartmentService();
    private final Map<String, Integer> deptMap = new HashMap<>();

    private Faculty facultyToEdit;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        FxUtil.runAsync(() -> deptService.getAllDepartments(),
                depts -> {
                    deptMap.clear();
                    departmentCombo.getItems().clear();
                    for (Department d : depts) {
                        deptMap.put(d.getName(), d.getId());
                        departmentCombo.getItems().add(d.getName());
                    }
                    if (facultyToEdit != null && facultyToEdit.getDepartmentName() != null) {
                        departmentCombo.setValue(facultyToEdit.getDepartmentName());
                    }
                },
                null
        );
    }

    public void setFaculty(Faculty faculty, Runnable callback) {
        this.facultyToEdit = faculty;
        this.onSaveCallback = callback;

        if (faculty != null) {
            formTitleLabel.setText("Edit Faculty Profile");
            empIdField.setText(faculty.getEmployeeId());
            empIdField.setDisable(true);
            designationField.setText(faculty.getDesignation());
            firstNameField.setText(faculty.getFirstName());
            lastNameField.setText(faculty.getLastName());
            emailField.setText(faculty.getEmail());
            phoneField.setText(faculty.getPhone());
            if (faculty.getDepartmentName() != null) {
                departmentCombo.setValue(faculty.getDepartmentName());
            }
            passwordBox.setVisible(false);
            passwordBox.setManaged(false);
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setVisible(false);

        String empId = empIdField.getText();
        String designation = designationField.getText();
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String deptName = departmentCombo.getValue();
        int deptId = deptName != null && deptMap.containsKey(deptName) ? deptMap.get(deptName) : 0;
        String password = passwordField.getText();

        Faculty f = facultyToEdit != null ? facultyToEdit : new Faculty();
        f.setEmployeeId(empId);
        f.setDesignation(designation);
        f.setFirstName(firstName);
        f.setLastName(lastName);
        f.setEmail(email);
        f.setPhone(phone);
        f.setDepartmentId(deptId);

        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");

        FxUtil.runAsync(() -> {
            if (facultyToEdit == null) {
                return facultyService.createFaculty(f, password);
            } else {
                return facultyService.updateFaculty(f);
            }
        },
        success -> {
            saveBtn.setDisable(false);
            if (onSaveCallback != null) onSaveCallback.run();
            closeDialog();
        },
        err -> {
            saveBtn.setDisable(false);
            saveBtn.setText("Save Faculty Member");
            errorLabel.setText(err instanceof ValidationException ? err.getMessage() : "Error saving faculty: " + err.getMessage());
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
