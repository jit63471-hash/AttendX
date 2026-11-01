package com.attendx.controller;

import com.attendx.model.*;
import com.attendx.service.AttendanceService;
import com.attendx.service.DepartmentService;
import com.attendx.util.AlertUtil;
import com.attendx.util.FxUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceController {

    @FXML private ComboBox<String> deptCombo;
    @FXML private ComboBox<String> courseCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private DatePicker sessionDatePicker;
    @FXML private TextField topicField;
    @FXML private Button loadStudentsBtn;

    @FXML private Label sessionSummaryLabel;
    @FXML private Button markAllPresentBtn;
    @FXML private Button markAllAbsentBtn;
    @FXML private Button submitBtn;
    @FXML private FlowPane cardsContainer;

    private final DepartmentService deptService = new DepartmentService();
    private final AttendanceService attendanceService = new AttendanceService();

    private final Map<String, Integer> deptMap = new HashMap<>();
    private final Map<String, Integer> courseMap = new HashMap<>();
    private final Map<String, Integer> semMap = new HashMap<>();
    private final Map<String, Integer> subjectMap = new HashMap<>();

    private final Map<Integer, Attendance.Status> studentStatusMap = new HashMap<>();
    private final Map<Integer, VBox> cardMap = new HashMap<>();
    private final Map<Integer, ToggleGroup> toggleGroupMap = new HashMap<>();

    private ClassSession currentSession;

    @FXML
    public void initialize() {
        sessionDatePicker.setValue(LocalDate.now());

        // Load departments
        FxUtil.runAsync(() -> deptService.getAllDepartments(),
                depts -> {
                    deptMap.clear();
                    deptCombo.getItems().clear();
                    for (Department d : depts) {
                        deptMap.put(d.getName(), d.getId());
                        deptCombo.getItems().add(d.getName());
                    }
                },
                null
        );

        // When Department changes, load Courses
        deptCombo.setOnAction(e -> {
            String selectedDept = deptCombo.getValue();
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
                },
                null
        );

        // When Course or Semester changes, load Subjects
        courseCombo.setOnAction(e -> reloadSubjects());
        semesterCombo.setOnAction(e -> reloadSubjects());
    }

    private void reloadSubjects() {
        String course = courseCombo.getValue();
        String sem = semesterCombo.getValue();
        if (course != null && sem != null && courseMap.containsKey(course) && semMap.containsKey(sem)) {
            int courseId = courseMap.get(course);
            int semId = semMap.get(sem);
            FxUtil.runAsync(() -> deptService.getSubjectsByCourseAndSemester(courseId, semId),
                    subjects -> {
                        subjectMap.clear();
                        subjectCombo.getItems().clear();
                        for (Subject s : subjects) {
                            subjectMap.put(s.getName() + " (" + s.getCode() + ")", s.getId());
                            subjectCombo.getItems().add(s.getName() + " (" + s.getCode() + ")");
                        }
                    },
                    null
            );
        }
    }

    @FXML
    public void handleLoadStudents() {
        String dept = deptCombo.getValue();
        String course = courseCombo.getValue();
        String sem = semesterCombo.getValue();
        String subject = subjectCombo.getValue();
        LocalDate date = sessionDatePicker.getValue();

        if (dept == null || course == null || sem == null || subject == null || date == null) {
            AlertUtil.showWarning("Incomplete Selection", "Please select Department, Course, Semester, Subject, and Date.");
            return;
        }

        int deptId = deptMap.get(dept);
        int courseId = courseMap.get(course);
        int semId = semMap.get(sem);
        int subjectId = subjectMap.get(subject);
        String topic = topicField.getText();

        loadStudentsBtn.setDisable(true);
        loadStudentsBtn.setText("Loading...");

        FxUtil.runAsync(() -> {
            // Find or create session
            ClassSession session = attendanceService.getOrCreateSession(subjectId, 1, date, topic);
            List<Student> students = attendanceService.getEnrolledStudents(deptId, courseId, semId);
            List<Attendance> existingAttendance = attendanceService.getSessionAttendance(session.getId());
            return new Object[]{session, students, existingAttendance};
        },
        result -> {
            loadStudentsBtn.setDisable(false);
            loadStudentsBtn.setText("Load Students");

            currentSession = (ClassSession) result[0];
            @SuppressWarnings("unchecked")
            List<Student> students = (List<Student>) result[1];
            @SuppressWarnings("unchecked")
            List<Attendance> existing = (List<Attendance>) result[2];

            renderStudentCards(students, existing);
        },
        err -> {
            loadStudentsBtn.setDisable(false);
            loadStudentsBtn.setText("Load Students");
            AlertUtil.showError("Loading Error", "Failed to load session roster: " + err.getMessage());
        });
    }

    private void renderStudentCards(List<Student> students, List<Attendance> existing) {
        cardsContainer.getChildren().clear();
        studentStatusMap.clear();
        cardMap.clear();
        toggleGroupMap.clear();

        if (students.isEmpty()) {
            sessionSummaryLabel.setText("No students enrolled in this group.");
            markAllPresentBtn.setDisable(true);
            markAllAbsentBtn.setDisable(true);
            submitBtn.setDisable(true);
            return;
        }

        // Map existing attendance for fast lookup
        Map<Integer, Attendance.Status> existingMap = new HashMap<>();
        for (Attendance a : existing) {
            existingMap.put(a.getStudentId(), a.getStatus());
        }

        boolean alreadyMarked = !existing.isEmpty();
        sessionSummaryLabel.setText(students.size() + " Students loaded. " +
                (alreadyMarked ? "⚠️ Existing attendance records loaded for editing." : "Ready to mark attendance."));

        for (Student s : students) {
            Attendance.Status initialStatus = existingMap.getOrDefault(s.getId(), Attendance.Status.PRESENT);
            studentStatusMap.put(s.getId(), initialStatus);

            VBox card = createStudentCard(s, initialStatus);
            cardMap.put(s.getId(), card);
            cardsContainer.getChildren().add(card);
        }

        markAllPresentBtn.setDisable(false);
        markAllAbsentBtn.setDisable(false);
        submitBtn.setDisable(false);
    }

    private VBox createStudentCard(Student s, Attendance.Status status) {
        VBox card = new VBox(8);
        card.getStyleClass().add("att-card");
        updateCardColor(card, status);

        Label regNo = new Label(s.getRegistrationNumber());
        regNo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6C757D;");

        Label name = new Label(s.getFullName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #212529;");

        HBox statusBox = new HBox(4);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup group = new ToggleGroup();
        toggleGroupMap.put(s.getId(), group);

        for (Attendance.Status st : Attendance.Status.values()) {
            ToggleButton tb = new ToggleButton(st.getDisplay().substring(0, 1));
            tb.setTooltip(new Tooltip(st.getDisplay()));
            tb.setToggleGroup(group);
            tb.setSelected(st == status);
            tb.setStyle("-fx-font-size: 10px; -fx-padding: 3 6;");

            tb.setOnAction(e -> {
                if (tb.isSelected()) {
                    studentStatusMap.put(s.getId(), st);
                    updateCardColor(card, st);
                }
            });

            statusBox.getChildren().add(tb);
        }

        card.getChildren().addAll(regNo, name, statusBox);
        return card;
    }

    private void updateCardColor(VBox card, Attendance.Status status) {
        card.getStyleClass().removeAll("present", "absent", "late");
        if (status == Attendance.Status.PRESENT) {
            card.getStyleClass().add("present");
        } else if (status == Attendance.Status.ABSENT) {
            card.getStyleClass().add("absent");
        } else if (status == Attendance.Status.LATE) {
            card.getStyleClass().add("late");
        }
    }

    @FXML
    public void handleMarkAllPresent() {
        for (Map.Entry<Integer, ToggleGroup> entry : toggleGroupMap.entrySet()) {
            int studentId = entry.getKey();
            studentStatusMap.put(studentId, Attendance.Status.PRESENT);
            ToggleGroup tg = entry.getValue();
            if (!tg.getToggles().isEmpty()) {
                tg.getToggles().get(0).setSelected(true);
            }
            VBox card = cardMap.get(studentId);
            if (card != null) updateCardColor(card, Attendance.Status.PRESENT);
        }
    }

    @FXML
    public void handleMarkAllAbsent() {
        for (Map.Entry<Integer, ToggleGroup> entry : toggleGroupMap.entrySet()) {
            int studentId = entry.getKey();
            studentStatusMap.put(studentId, Attendance.Status.ABSENT);
            ToggleGroup tg = entry.getValue();
            if (tg.getToggles().size() > 1) {
                tg.getToggles().get(1).setSelected(true);
            }
            VBox card = cardMap.get(studentId);
            if (card != null) updateCardColor(card, Attendance.Status.ABSENT);
        }
    }

    @FXML
    public void handleSubmitAttendance() {
        if (currentSession == null || studentStatusMap.isEmpty()) {
            AlertUtil.showWarning("No Attendance Data", "Please load students first.");
            return;
        }

        submitBtn.setDisable(true);
        submitBtn.setText("Saving...");

        FxUtil.runAsync(() -> attendanceService.submitBulkAttendance(currentSession.getId(), studentStatusMap, null),
                success -> {
                    submitBtn.setDisable(false);
                    submitBtn.setText("💾 Submit Attendance");
                    AlertUtil.showSuccess("Success", "Attendance for " + studentStatusMap.size() + " students saved successfully!");
                },
                err -> {
                    submitBtn.setDisable(false);
                    submitBtn.setText("💾 Submit Attendance");
                    AlertUtil.showError("Submission Failed", "Error saving attendance: " + err.getMessage());
                }
        );
    }
}
