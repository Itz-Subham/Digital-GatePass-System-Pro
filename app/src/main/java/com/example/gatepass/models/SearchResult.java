package com.example.gatepass.models;

public class SearchResult {
    public String id, name, regNo, status, studentName, entryTime;
    public boolean hasStudentVisit;

    public SearchResult(String id, String name, String regNo, String status, String studentName, String entryTime, boolean hasStudentVisit) {
        this.id = id;
        this.name = name;
        this.regNo = regNo;
        this.status = status;
        this.studentName = studentName;
        this.entryTime = entryTime;
        this.hasStudentVisit = hasStudentVisit;
    }
}