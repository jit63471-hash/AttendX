package com.attendx.model;

import java.time.LocalDateTime;

public class Course {
    private int id;
    private String code;
    private String name;
    private int departmentId;
    private String departmentName;
    private int durationSemesters;
    private boolean isActive;
    private LocalDateTime createdAt;

    public Course() {}

    public Course(int id, String code, String name, int departmentId, int durationSemesters, boolean isActive) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.departmentId = departmentId;
        this.durationSemesters = durationSemesters;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getDurationSemesters() { return durationSemesters; }
    public void setDurationSemesters(int durationSemesters) { this.durationSemesters = durationSemesters; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
