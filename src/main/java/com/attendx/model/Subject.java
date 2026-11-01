package com.attendx.model;

import java.time.LocalDateTime;

public class Subject {
    private int id;
    private String code;
    private String name;
    private int courseId;
    private String courseName;
    private int semesterId;
    private String semesterLabel;
    private int credits;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Subject() {}

    public Subject(int id, String code, String name, int courseId, int semesterId, int credits, boolean isActive) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.courseId = courseId;
        this.semesterId = semesterId;
        this.credits = credits;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getSemesterId() { return semesterId; }
    public void setSemesterId(int semesterId) { this.semesterId = semesterId; }

    public String getSemesterLabel() { return semesterLabel; }
    public void setSemesterLabel(String semesterLabel) { this.semesterLabel = semesterLabel; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
