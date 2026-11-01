package com.attendx.model;

public class Semester {
    private int id;
    private int number;
    private String label;
    private boolean isActive;

    public Semester() {}

    public Semester(int id, int number, String label, boolean isActive) {
        this.id = id;
        this.number = number;
        this.label = label;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return label;
    }
}
