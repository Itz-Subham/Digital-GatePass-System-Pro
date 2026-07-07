package com.example.gatepass.utils;

import java.util.regex.Pattern;

public class Constants {

    // Web App URLs (Synced with official portal)
    public static final String BASE_QR_URL = "https://gate-pass-generator-6f165.web.app/?id=";

    // Intent Extras
    public static final String EXTRA_DOCUMENT_ID = "DOCUMENT_ID";
    public static final String EXTRA_STUDENT_NAME = "STUDENT_NAME";
    public static final String EXTRA_STUDENT_REG = "STUDENT_REG";
    public static final String EXTRA_VISITOR_NAME = "VISITOR_NAME";
    public static final String EXTRA_ENTRY_TIME = "ENTRY_TIME";
    public static final String EXTRA_VISITOR_ID = "VISITOR_ID";

    // Regex Patterns
    public static final Pattern AADHAAR_PATTERN = Pattern.compile("\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}");
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z\\s]{1,38}[a-zA-Z]$");
    public static final Pattern BLACKLIST_REGEX = Pattern.compile(
            "(?i).*(gov|govement|gavement|india|indla|unique|unlque|authority|uidai|aadhaar|aadhar|dob|" +
                    "male|female|year|birth|address|father|husband|wife|enrolment|vid|help|www|http|phone|mobile).*"
    );

    // OCR Constants
    public static final double SIMILARITY_THRESHOLD = 0.70;
    public static final int REQUIRED_CONSENSUS = 2;
    public static final float BLUR_THRESHOLD = 4.0f;
}