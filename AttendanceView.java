import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;

public class AttendanceView extends JFrame {
    // Tab 1 Components
    public JTextField rollField = new JTextField(10);
    public JTextField nameField = new JTextField(14);
    public JButton addStudentBtn = new JButton("Add Student");
    public JButton deleteStudentBtn = new JButton("Delete Selected Student");

    public JComboBox<String> studentDropdown = new JComboBox<>();
    public JTextField dateField = new JTextField(LocalDate.now().toString(), 9);
    public JRadioButton presentRadio = new JRadioButton("Present", true);
    public JRadioButton absentRadio = new JRadioButton("Absent");
    public JButton markBtn = new JButton("Save Attendance");
    public JButton viewDateBtn = new JButton("Load Date Logs");
    public JButton deleteLogBtn = new JButton("Delete Selected Log Row");

    public DefaultTableModel dailyTableModel;
    public JTable dailyTable;

    // Tab 2 Components
    public JTextField monthField = new JTextField(YearMonth.now().toString(), 8);
    public JButton generateReportBtn = new JButton("Generate Monthly Report");
    public JButton exportCsvBtn = new JButton("Export Report to CSV");
    public DefaultTableModel reportTableModel;
    public JTable reportTable;

    public AttendanceView() {
        setTitle("Attendance Management System");
        setSize(980, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Daily Attendance & Students", createDailyPanel());
        tabs.addTab("2. Monthly Summary Report", createReportPanel());

        add(tabs);
    }

    private JPanel createDailyPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel topContainer = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel studentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        studentPanel.setBorder(BorderFactory.createTitledBorder("Register / Manage Students"));
        studentPanel.add(new JLabel("Roll No:"));
        studentPanel.add(rollField);
        studentPanel.add(new JLabel("Name:"));
        studentPanel.add(nameField);
        studentPanel.add(addStudentBtn);
        studentPanel.add(deleteStudentBtn);

        JPanel markPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        markPanel.setBorder(BorderFactory.createTitledBorder("Log Daily Attendance / Absenteeism"));
        markPanel.add(new JLabel("Student:"));
        markPanel.add(studentDropdown);
        markPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        markPanel.add(dateField);

        ButtonGroup statusGroup = new ButtonGroup();
        statusGroup.add(presentRadio);
        statusGroup.add(absentRadio);
        markPanel.add(presentRadio);
        markPanel.add(absentRadio);
        markPanel.add(markBtn);
        markPanel.add(viewDateBtn);

        topContainer.add(studentPanel);
        topContainer.add(markPanel);
        panel.add(topContainer, BorderLayout.NORTH);

        String[] cols = {"Roll No", "Student Name", "Date", "Attendance Status"};
        dailyTableModel = new DefaultTableModel(cols, 0);
        dailyTable = new JTable(dailyTableModel);
        panel.add(new JScrollPane(dailyTable), BorderLayout.CENTER);

        // Bottom bar for deleting a highlighted attendance entry
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(deleteLogBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createTitledBorder("Monthly Summary & Shortage Check"));
        topPanel.add(new JLabel("Month (YYYY-MM):"));
        topPanel.add(monthField);
        topPanel.add(generateReportBtn);
        topPanel.add(exportCsvBtn);

        panel.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Roll No", "Name", "Total Classes", "Present", "Absent", "Percentage", "Status (75% Rule)"};
        reportTableModel = new DefaultTableModel(cols, 0);
        reportTable = new JTable(reportTableModel);
        panel.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        return panel;
    }
}