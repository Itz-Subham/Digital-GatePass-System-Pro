package com.example.gatepass.firebase;

public class FirestoreConstants {

    // Collections
    public static final String COL_USERS = "users";
    public static final String COL_VISITORS = "visitors";
    public static final String COL_STUDENTS = "students";
    public static final String COL_BLACKLIST = "blacklist";

    // Common Field Names
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAME_LOWER = "nameLower";
    public static final String FIELD_ID = "id";

    // User Fields
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_PASSWORD = "password";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_ACTIVE = "active";

    // Visitor Fields
    public static final String FIELD_VISITOR_NAME = "visitorName";
    public static final String FIELD_VISITOR_NAME_LOWER = "visitorNameLower";
    public static final String FIELD_VISITOR_ID_HASH = "visitorIdHash";
    public static final String FIELD_STUDENT_REG = "studentReg";
    public static final String FIELD_STUDENT_NAME = "studentName";
    public static final String FIELD_HAS_STUDENT_VISIT = "hasStudentVisit";
    public static final String FIELD_ENTRY_TIME = "entryTime";
    public static final String FIELD_EXIT_TIMESTAMP = "exitTimestamp";
    public static final String FIELD_EXIT_TIME = "exitTime";

    // Student Fields
    public static final String FIELD_REG_NO = "regNo";

    // Role Values
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_SECURITY = "security";

    // Status Values
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_LEFT = "Left";
}