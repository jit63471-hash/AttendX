package com.attendx.controller;

import com.attendx.model.Faculty;
import com.attendx.service.FacultyService;
import com.attendx.util.AlertUtil;
import com.attendx.util.FxUtil;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FacultyController {

    @FXML private TextField searchField;
    @FXML private Label facultyCountLabel;

    @FXML private TableView<Faculty> facultyTable;
    @FXML private TableColumn<Faculty, String> colEmpId;
    @FXML private TableColumn<Faculty, String> colName;
    @FXML private TableColumn<Faculty, String> colEmail;
    @FXML private TableColumn<Faculty, String> colPhone;
    @FXML private TableColumn<Faculty, String> colDept;
    @FXML private TableColumn<Faculty, String> colDesignation;
    @FXML private TableColumn<Faculty, String> colStatus;
    @FXML private TableColumn<Faculty, Void> colActions;

    private final FacultyService facultyService = new FacultyService();
    private final ObservableList<Faculty> masterData = FXCollections.observableArrayList();
    private FilteredList<Faculty> filteredData;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        loadFaculty();
    }

    private void setupTableColumns() {
        colEmpId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colDesignation.setCellValueFactory(new PropertyValueFactory<>("designation"));

        // Status Badge
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Faculty f = getTableRow().getItem();
                    Label badge = new Label(f.isActive() ? "Active" : "Inactive");
                    badge.getStyleClass().add(f.isActive() ? "status-present" : "status-absent");
                    setGraphic(badge);
                }
            }
        });

        // Action Buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button toggleBtn = new Button("Deactivate");
            private final HBox pane = new HBox(6, editBtn, toggleBtn);

            {
                pane.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("secondary-btn");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                toggleBtn.getStyleClass().add("secondary-btn");
                toggleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");

                editBtn.setOnAction(e -> {
                    Faculty f = getTableRow().getItem();
                    if (f != null) openFacultyDialog(f);
                });

                toggleBtn.setOnAction(e -> {
                    Faculty f = getTableRow().getItem();
                    if (f != null) {
                        if (f.isActive()) {
                            boolean ok = AlertUtil.showConfirmation("Deactivate Faculty", "Deactivate " + f.getFullName() + "?");
                            if (ok) {
                                facultyService.deactivateFaculty(f.getId());
                                loadFaculty();
                            }
                        } else {
                            facultyService.activateFaculty(f.getId());
                            loadFaculty();
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
                    Faculty f = getTableRow().getItem();
                    toggleBtn.setText(f.isActive() ? "Deactivate" : "Activate");
                    setGraphic(pane);
                }
            }
        });
    }

    private void setupFilters() {
        filteredData = new FilteredList<>(masterData, p -> true);
        facultyTable.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal != null ? newVal.toLowerCase().trim() : "";
            filteredData.setPredicate(f -> {
                if (query.isEmpty()) return true;
                return (f.getEmployeeId() != null && f.getEmployeeId().toLowerCase().contains(query)) ||
                        (f.getFullName() != null && f.getFullName().toLowerCase().contains(query)) ||
                        (f.getEmail() != null && f.getEmail().toLowerCase().contains(query));
            });
            facultyCountLabel.setText("Showing " + filteredData.size() + " faculty members");
        });
    }

    public void loadFaculty() {
        FxUtil.runAsync(() -> facultyService.getAllFaculty(),
                list -> {
                    masterData.setAll(list);
                    facultyCountLabel.setText("Showing " + masterData.size() + " faculty members");
                },
                err -> AlertUtil.showError("Loading Error", "Failed to retrieve faculty: " + err.getMessage())
        );
    }

    @FXML
    public void handleOpenAddDialog() {
        openFacultyDialog(null);
    }

    private void openFacultyDialog(Faculty faculty) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/faculty_form.fxml"));
            Parent root = loader.load();

            FacultyFormController controller = loader.getController();
            controller.setFaculty(faculty, this::loadFaculty);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(faculty == null ? "Register Faculty Member" : "Edit Faculty Profile");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(facultyTable.getScene().getWindow());

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (Exception e) {
            AlertUtil.showError("Dialog Error", "Could not open faculty form: " + e.getMessage());
        }
    }
}
