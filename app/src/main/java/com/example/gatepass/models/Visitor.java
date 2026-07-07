package com.example.gatepass.models;

import com.example.gatepass.utils.DateTimeUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Visitor {
    public String visitorName;
    public String visitorNameLower;
    public String visitorIdHash;
    public String studentReg;
    public String studentName;
    public boolean hasStudentVisit;
    
    @PropertyName("entryTime")
    public Object entryTime; // Handles both String and Timestamp from Firestore
    
    public String status;
    public long timestamp;
    public long exitTimestamp;
    public String exitTime;

    public Visitor() {
        // Required empty constructor for Firestore
    }

    public Visitor(String visitorName, String visitorIdHash, String studentReg, String studentName, Object entryTime, String status, long timestamp) {
        this.visitorName = visitorName;
        this.visitorIdHash = visitorIdHash;
        this.studentReg = studentReg;
        this.studentName = studentName;
        this.entryTime = entryTime;
        this.status = status;
        this.timestamp = timestamp;
    }

    @PropertyName("visitorName")
    public String getVisitorName() { return visitorName; }
    
    @PropertyName("visitorName")
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    @PropertyName("visitorIdHash")
    public String getVisitorIdHash() { return visitorIdHash; }
    
    @PropertyName("visitorIdHash")
    public void setVisitorIdHash(String visitorIdHash) { this.visitorIdHash = visitorIdHash; }

    public String getStudentReg() { return studentReg; }
    public String getStudentName() { return studentName; }
    public boolean isHasStudentVisit() { return hasStudentVisit; }
    
    @PropertyName("entryTime")
    public Object getRawEntryTime() { return entryTime; }
    
    @PropertyName("entryTime")
    public void setRawEntryTime(Object entryTime) { this.entryTime = entryTime; }

    public String getFormattedEntryTime() {
        if (entryTime instanceof Timestamp) {
            return DateTimeUtils.formatHeaderDate(((Timestamp) entryTime).toDate());
        }
        return entryTime != null ? entryTime.toString() : ""; 
    }
    
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public String getExitTime() { return exitTime; }
}