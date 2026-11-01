package com.attendx.service;

import com.attendx.config.AppConfig;
import com.attendx.model.AuditLog;
import com.attendx.model.DashboardStats;
import com.attendx.model.Student;
import com.attendx.repository.AttendanceRepository;
import com.attendx.repository.DepartmentRepository;
import com.attendx.repository.FacultyRepository;
import com.attendx.repository.StudentRepository;

import java.util.List;
import java.util.Map;

public class DashboardService {
    private final StudentRepository studentRepo = new StudentRepository();
    private final FacultyRepository facultyRepo = new FacultyRepository();
    private final DepartmentRepository deptRepo = new DepartmentRepository();
    private final AttendanceRepository attendanceRepo = new AttendanceRepository();
    private final AuditService auditService = new AuditService();

    public DashboardStats getDashboardStats() {
        int students = studentRepo.countActive();
        int faculty = facultyRepo.countActive();
        int departments = deptRepo.count();
        int presentToday = attendanceRepo.countPresentToday();
        int absentToday = attendanceRepo.countAbsentToday();
        double overallPercentage = attendanceRepo.getOverallAttendancePercentage();
        int shortageCount = attendanceRepo.getShortageStudents(AppConfig.MIN_ATTENDANCE_PERCENT).size();

        return new DashboardStats(students, faculty, departments, presentToday, absentToday, overallPercentage, shortageCount);
    }

    public List<Map<String, Object>> getWeeklyAttendance() {
        return attendanceRepo.getWeeklyAttendance();
    }

    public List<Student> getShortageStudents() {
        return attendanceRepo.getShortageStudents(AppConfig.MIN_ATTENDANCE_PERCENT);
    }

    public List<AuditLog> getRecentActivities(int limit) {
        return auditService.getRecentLogs(limit);
    }
}
