package com.attendx.model;

import java.time.LocalDateTime;

public class Attendance {

    public enum Status {
        PRESENT("Present", "status-present"),
        ABSENT("Absent", "status-absent"),
        LATE("Late", "status-late"),
        LEAVE("Leave", "status-leave"),
        EXCUSED("Excused", "status-excused");

        private final String display;
        private final String cssClass;

        Status(String display, String cssClass) {
            this.display = display;
            this.cssClass = cssClass;
        }

        public String getDisplay() { return display; }
        public String getCssClass() { return cssClass; }

        public static Status fromString(String val) {
            if (val == null) return ABSENT;
            for (Status s : values()) {
                if (s.name().equalsIgnoreCase(val) || s.display.equalsIgnoreCase(val)) {
                    return s;
                }
            }
            return ABSENT;
        }
    }

    private int id;
    private int classSessionId;
    private int studentId;
    private String studentName;
    private String registrationNumber;
    private Status status = Status.ABSENT;
    private String remarks;
    private Integer markedBy;
    private LocalDateTime markedAt;
    private LocalDateTime updatedAt;

    public Attendance() {}

    public Attendance(int id, int classSessionId, int studentId, Status status, String remarks, Integer markedBy) {
        this.id = id;
        this.classSessionId = classSessionId;
        this.studentId = studentId;
        this.status = status != null ? status : Status.ABSENT;
        this.remarks = remarks;
        this.markedBy = markedBy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClassSessionId() { return classSessionId; }
    public void setClassSessionId(int classSessionId) { this.classSessionId = classSessionId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public void setStatusString(String statusStr) { this.status = Status.fromString(statusStr); }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Integer getMarkedBy() { return markedBy; }
    public void setMarkedBy(Integer markedBy) { this.markedBy = markedBy; }

    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return studentName + " [" + registrationNumber + "]: " + status.getDisplay();
    }
}
