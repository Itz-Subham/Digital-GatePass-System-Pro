package com.example.gatepass.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public static String getCurrentTime() {
        return TIME_FORMAT.format(new Date());
    }

    public static String formatHeaderDate(Date date) {
        if (date == null) return "";
        return DATE_TIME_FORMAT.format(date);
    }
}
