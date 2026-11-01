package com.attendx.service;

import com.attendx.model.Attendance;
import com.attendx.model.ClassSession;
import com.attendx.model.Student;
import com.attendx.repository.AttendanceRepository;
import com.attendx.repository.ClassSessionRepository;
import com.attendx.repository.StudentRepository;
import com.attendx.security.SessionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AttendanceService {
    private final ClassSessionRepository sessionRepo = new ClassSessionRepository();
    private final AttendanceRepository attendanceRepo = new AttendanceRepository();
    private final StudentRepository studentRepo = new StudentRepository();
    private final AuditService auditService = new AuditService();

    public ClassSession getOrCreateSession(int subjectId, int facultyId, LocalDate date, String topic) {
        Optional<ClassSession> existing = sessionRepo.findBySubjectAndDate(subjectId, date);
        if (existing.isPresent()) {
            return existing.get();
        }

        ClassSession newSession = new ClassSession();
        newSession.setSubjectId(subjectId);
        newSession.setFacultyId(facultyId);
        newSession.setSessionDate(date);
        newSession.setTopic(topic != null && !topic.trim().isEmpty() ? topic : "Regular Lecture");
        newSession.setStatus("COMPLETED");

        int id = sessionRepo.saveAndGetId(newSession);
        newSession.setId(id);

        auditService.log("CREATE_CLASS_SESSION", "CLASS_SESSION", id, null, "Subject: " + subjectId + " Date: " + date);
        return newSession;
    }

    public List<Student> getEnrolledStudents(int departmentId, int courseId, int semesterId) {
        return studentRepo.findByGroup(departmentId, courseId, semesterId);
    }

    public List<Attendance> getSessionAttendance(int sessionId) {
        return attendanceRepo.findBySession(sessionId);
    }

    public boolean submitBulkAttendance(int sessionId, Map<Integer, Attendance.Status> studentStatusMap, Map<Integer, String> remarksMap) {
        Integer markedByUserId = null;
        if (SessionManager.getInstance().isLoggedIn()) {
            markedByUserId = SessionManager.getInstance().getCurrentUser().get().getId();
        }

        List<Attendance> list = new ArrayList<>();
        for (Map.Entry<Integer, Attendance.Status> entry : studentStatusMap.entrySet()) {
            int studentId = entry.getKey();
            Attendance.Status status = entry.getValue();
            String remarks = remarksMap != null ? remarksMap.get(studentId) : null;

            Attendance att = new Attendance();
            att.setClassSessionId(sessionId);
            att.setStudentId(studentId);
            att.setStatus(status);
            att.setRemarks(remarks);
            att.setMarkedBy(markedByUserId);

            list.add(att);
        }

        boolean saved = attendanceRepo.saveAll(list);
        if (saved) {
            auditService.log("SUBMIT_ATTENDANCE", "ATTENDANCE", sessionId, null, "Marked attendance for " + list.size() + " students");
        }
        return saved;
    }

    public boolean updateSingleAttendance(int sessionId, int studentId, Attendance.Status newStatus, String remarks) {
        Integer markedBy = null;
        if (SessionManager.getInstance().isLoggedIn()) {
            markedBy = SessionManager.getInstance().getCurrentUser().get().getId();
        }

        Attendance att = new Attendance();
        att.setClassSessionId(sessionId);
        att.setStudentId(studentId);
        att.setStatus(newStatus);
        att.setRemarks(remarks);
        att.setMarkedBy(markedBy);

        boolean updated = attendanceRepo.saveOrUpdate(att);
        if (updated) {
            auditService.log("UPDATE_ATTENDANCE", "ATTENDANCE", sessionId, null, "Student " + studentId + " changed to " + newStatus);
        }
        return updated;
    }
}
