import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class AttendanceController {
    private AttendanceModel model;
    private AttendanceView view;

    public AttendanceController(AttendanceModel model, AttendanceView view) {
        this.model = model;
        this.view = view;

        refreshStudentDropdown();
        refreshDailyLogs();
        refreshMonthlyReport();

        // 1. Add Student
        view.addStudentBtn.addActionListener(e -> {
            String roll = view.rollField.getText().trim();
            String name = view.nameField.getText().trim();
            if (roll.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(view, "Please enter both Roll No and Name!");
                return;
            }
            if (model.addStudent(roll, name)) {
                JOptionPane.showMessageDialog(view, "Student added successfully!");
                view.rollField.setText("");
                view.nameField.setText("");
                refreshStudentDropdown();
                refreshMonthlyReport();
            } else {
                JOptionPane.showMessageDialog(view, "Error: Roll No already exists!");
            }
        });

        // 2. Delete Selected Student (from Dropdown)
        view.deleteStudentBtn.addActionListener(e -> {
            String selected = (String) view.studentDropdown.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(view, "No student selected in dropdown!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                    view,
                    "Delete student (" + selected + ") and all their attendance records?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                int studentId = Integer.parseInt(selected.split(" \\| ")[0]);
                if (model.deleteStudent(studentId)) {
                    JOptionPane.showMessageDialog(view, "Student deleted!");
                    refreshStudentDropdown();
                    refreshDailyLogs();
                    refreshMonthlyReport();
                }
            }
        });

        // 3. Save Attendance
        view.markBtn.addActionListener(e -> {
            String selected = (String) view.studentDropdown.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(view, "No student selected!");
                return;
            }
            int studentId = Integer.parseInt(selected.split(" \\| ")[0]);
            String date = view.dateField.getText().trim();
            String status = view.presentRadio.isSelected() ? "Present" : "Absent";

            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(view, "Invalid Date! Use YYYY-MM-DD format.");
                return;
            }

            if (model.markAttendance(studentId, date, status)) {
                refreshDailyLogs();
                refreshMonthlyReport();
            } else {
                JOptionPane.showMessageDialog(view, "Failed to log attendance.");
            }
        });

        // 4. Delete Selected Attendance Row from Table
        view.deleteLogBtn.addActionListener(e -> {
            int selectedRow = view.dailyTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(view, "Please click on a row in the table to select it first!");
                return;
            }
            String rollNo = view.dailyTableModel.getValueAt(selectedRow, 0).toString();
            String date = view.dailyTableModel.getValueAt(selectedRow, 2).toString();

            if (model.deleteAttendanceLog(rollNo, date)) {
                refreshDailyLogs();
                refreshMonthlyReport();
                JOptionPane.showMessageDialog(view, "Attendance log removed!");
            }
        });

        // 5. Load Date Logs & Monthly Report & Export CSV
        view.viewDateBtn.addActionListener(e -> refreshDailyLogs());
        view.generateReportBtn.addActionListener(e -> refreshMonthlyReport());
        view.exportCsvBtn.addActionListener(e -> exportReportToCSV());
    }

    private void refreshStudentDropdown() {
        view.studentDropdown.removeAllItems();
        List<String> students = model.getAllStudents();
        for (String s : students) {
            view.studentDropdown.addItem(s);
        }
    }

    private void refreshDailyLogs() {
        view.dailyTableModel.setRowCount(0);
        String date = view.dateField.getText().trim();
        List<String[]> logs = model.getDailyLogs(date);
        for (String[] row : logs) {
            view.dailyTableModel.addRow(row);
        }
    }

    private void refreshMonthlyReport() {
        view.reportTableModel.setRowCount(0);
        String month = view.monthField.getText().trim();
        List<String[]> summary = model.getMonthlySummary(month);
        for (String[] row : summary) {
            view.reportTableModel.addRow(row);
        }
    }

    private void exportReportToCSV() {
        String month = view.monthField.getText().trim();
        String fileName = "Attendance_Report_" + month + ".csv";
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Roll No,Name,Total Classes,Present,Absent,Percentage,Status\n");
            for (int i = 0; i < view.reportTableModel.getRowCount(); i++) {
                for (int j = 0; j < view.reportTableModel.getColumnCount(); j++) {
                    writer.write(view.reportTableModel.getValueAt(i, j).toString() + (j < 6 ? "," : ""));
                }
                writer.write("\n");
            }
            JOptionPane.showMessageDialog(view, "Report exported to " + fileName);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(view, "Export failed: " + ex.getMessage());
        }
    }
}
