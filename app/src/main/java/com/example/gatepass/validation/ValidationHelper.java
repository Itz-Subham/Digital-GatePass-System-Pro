package com.example.gatepass.validation;

import android.text.TextUtils;

import com.example.gatepass.utils.Constants;

public class ValidationHelper {

    public static boolean isValidAadhaar(String aadhaar) {
        return !TextUtils.isEmpty(aadhaar) && aadhaar.length() == 12 && Verhoeff.validateVerhoeff(aadhaar);
    }

    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && Constants.NAME_PATTERN.matcher(name).matches();
    }

    public static boolean containsBlacklistedText(String text) {
        return !TextUtils.isEmpty(text) && Constants.BLACKLIST_REGEX.matcher(text).find();
    }
}
