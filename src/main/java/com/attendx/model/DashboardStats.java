package com.attendx.model;

public class DashboardStats {
    private int totalStudents;
    private int totalFaculty;
    private int totalDepartments;
    private int presentToday;
    private int absentToday;
    private double overallAttendancePercent;
    private int shortageCount;

    public DashboardStats() {}

    public DashboardStats(int totalStudents, int totalFaculty, int totalDepartments, int presentToday, int absentToday, double overallAttendancePercent, int shortageCount) {
        this.totalStudents = totalStudents;
        this.totalFaculty = totalFaculty;
        this.totalDepartments = totalDepartments;
        this.presentToday = presentToday;
        this.absentToday = absentToday;
        this.overallAttendancePercent = overallAttendancePercent;
        this.shortageCount = shortageCount;
    }

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

    public int getTotalFaculty() { return totalFaculty; }
    public void setTotalFaculty(int totalFaculty) { this.totalFaculty = totalFaculty; }

    public int getTotalDepartments() { return totalDepartments; }
    public void setTotalDepartments(int totalDepartments) { this.totalDepartments = totalDepartments; }

    public int getPresentToday() { return presentToday; }
    public void setPresentToday(int presentToday) { this.presentToday = presentToday; }

    public int getAbsentToday() { return absentToday; }
    public void setAbsentToday(int absentToday) { this.absentToday = absentToday; }

    public double getOverallAttendancePercent() { return overallAttendancePercent; }
    public void setOverallAttendancePercent(double overallAttendancePercent) { this.overallAttendancePercent = overallAttendancePercent; }

    public int getShortageCount() { return shortageCount; }
    public void setShortageCount(int shortageCount) { this.shortageCount = shortageCount; }
}
