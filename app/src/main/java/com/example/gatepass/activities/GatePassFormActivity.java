package com.example.gatepass.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.gatepass.validation.BlacklistManager;
import com.example.gatepass.utils.Constants;
import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.utils.HashUtils;
import com.example.gatepass.R;
import com.example.gatepass.scanner.StudentLookupManager;
import com.example.gatepass.validation.ValidationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Locale;

// handles both the OCR flow (Aadhaar already scanned in MainActivity) and manual entry
public class GatePassFormActivity extends BaseActivity {

    private TextInputEditText etVisitorName, etVisitorId, etEntryTime, etStudentReg, etStudentName;
    private TextInputLayout tilVisitorId, tilStudentReg, tilStudentName;
    private View llStudentFields;
    private android.widget.RadioGroup rgVisitStudent;
    private MaterialButton btnSubmit;
    private View pbLoading;
    private TextView tvBlacklistWarning;

    private StudentLookupManager studentLookupManager;
    private BlacklistManager blacklistManager;

    private boolean isStudentVerified = false;
    private boolean isOcrEntry = false;
    private String visitorIdHash = "";

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gate_pass_form);

        studentLookupManager = new StudentLookupManager(repository);
        blacklistManager = new BlacklistManager(repository);

        initializeUI();
        catchIntentData();
        applyOcrUiAdjustments();
        setupListeners();
    }

    private void initializeUI() {
        ImageView btnBack = findViewById(R.id.btnBack);
        etVisitorName = findViewById(R.id.etVisitorName);
        etVisitorId = findViewById(R.id.etVisitorId);
        etEntryTime = findViewById(R.id.etEntryTime);
        etStudentReg = findViewById(R.id.etStudentReg);
        etStudentName = findViewById(R.id.etStudentName);
        tilVisitorId = findViewById(R.id.tilVisitorId);
        tilStudentReg = findViewById(R.id.tilStudentReg);
        tilStudentName = findViewById(R.id.tilStudentName);
        llStudentFields = findViewById(R.id.llStudentFields);
        rgVisitStudent = findViewById(R.id.rgVisitStudent);
        btnSubmit = findViewById(R.id.btnSubmit);
        pbLoading = findViewById(R.id.pbLoading);
        tvBlacklistWarning = findViewById(R.id.tvBlacklistWarning);

        btnBack.setOnClickListener(v -> finish());

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        etEntryTime.setText(currentTime);
    }

    private void catchIntentData() {
        String visitorName = getIntent().getStringExtra(Constants.EXTRA_VISITOR_NAME);
        String rawVisitorId = getIntent().getStringExtra(Constants.EXTRA_VISITOR_ID);

        if (rawVisitorId != null && !rawVisitorId.isEmpty()) {
            visitorIdHash = HashUtils.hashAadhaar(rawVisitorId);
            isOcrEntry = true;
        }

        if (visitorName != null && !visitorName.isEmpty()) {
            etVisitorName.setText(visitorName);
            checkBlacklist(visitorName);
        }
    }

    // OCR entries already have the Aadhaar, so hide the field and don't let
    // leftover validation state block the submit button. no-op for manual entry.
    private void applyOcrUiAdjustments() {
        if (isOcrEntry) {
            tilVisitorId.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
        }
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> saveGateLog());

        rgVisitStudent.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbYes) {
                llStudentFields.setVisibility(View.VISIBLE);
                isStudentVerified = false;
            } else {
                llStudentFields.setVisibility(View.GONE);
                isStudentVerified = true;
                tilStudentReg.setError(null);
                tilStudentName.setError(null);
            }
        });

        isStudentVerified = true;

        etStudentReg.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (rgVisitStudent.getCheckedRadioButtonId() == R.id.rbYes) {
                    isStudentVerified = false;
                    tilStudentReg.setHelperText(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {
                String reg = s.toString().trim();
                if (reg.length() >= 3) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                    debounceRunnable = () -> {
                        performStudentLookup(reg, false);
                        debounceRunnable = null;
                    };
                    debounceHandler.postDelayed(debounceRunnable, 300);
                }
            }
        });

        etStudentName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (rgVisitStudent.getCheckedRadioButtonId() == R.id.rbYes) {
                    isStudentVerified = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                if (rgVisitStudent.getCheckedRadioButtonId() == R.id.rbYes && name.length() >= 4 && etStudentReg.getText().toString().isEmpty()) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                    debounceRunnable = () -> {
                        performStudentLookup(name, true);
                        debounceRunnable = null;
                    };
                    debounceHandler.postDelayed(debounceRunnable, 500);
                }
            }
        });

        etVisitorName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) checkBlacklist(etVisitorName.getText().toString().trim());
        });

        // Only wire up Aadhaar validation for manual entry — OCR field is hidden.
        if (!isOcrEntry) {
            setupAadhaarValidation();
        }
    }

    private void setupAadhaarValidation() {
        etVisitorId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilVisitorId.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {
                String id = s.toString().trim();
                if (id.length() == 12) {
                    if (ValidationHelper.isValidAadhaar(id)) {
                        tilVisitorId.setHelperText("Aadhaar Verified");
                        tilVisitorId.setError(null);
                        btnSubmit.setEnabled(true);
                    } else {
                        tilVisitorId.setError("Invalid Aadhaar (Verhoeff Check Failed)");
                        btnSubmit.setEnabled(false);
                    }
                } else if (id.length() > 0) {
                    tilVisitorId.setError("Enter 12 digits");
                    btnSubmit.setEnabled(false);
                }
            }
        });
    }

    private void performStudentLookup(String query, boolean byName) {
        StudentLookupManager.StudentLookupListener listener = new StudentLookupManager.StudentLookupListener() {
            @Override
            public void onStudentFound(String name, String regNo) {
                if (byName) {
                    etStudentReg.setText(regNo);
                    tilStudentName.setHelperText("Student Verified: " + regNo);
                    tilStudentName.setError(null);
                } else {
                    etStudentName.setText(name);
                    tilStudentReg.setHelperText("Student Verified: " + name);
                    tilStudentReg.setError(null);
                }
                // setText() above triggers the other field's TextWatcher, which resets
                // isStudentVerified to false - so this has to be set AFTER, not before.
                isStudentVerified = true;
            }

            @Override
            public void onStudentNotFound(String error) {
                isStudentVerified = false;
                (byName ? tilStudentName : tilStudentReg).setError(error);
            }
        };

        if (byName) {
            studentLookupManager.lookupByName(query, listener);
        } else {
            studentLookupManager.lookupByReg(query, listener);
        }
    }

    private void checkBlacklist(String name) {
        blacklistManager.checkBlacklist(name, new BlacklistManager.BlacklistCheckListener() {
            @Override
            public void onBlacklisted(String name) {
                tvBlacklistWarning.setVisibility(View.VISIBLE);
                tvBlacklistWarning.setText("WARNING: This individual is on the security restricted list!");
                tvBlacklistWarning.setTextColor(Color.RED);
            }

            @Override
            public void onNotBlacklisted() {
                tvBlacklistWarning.setVisibility(View.GONE);
            }
        });
    }

    private void saveGateLog() {
        // OCR flow: hash already set in catchIntentData(), skip field validation entirely.
        // Manual flow: read and validate the Aadhaar field before proceeding.
        if (!isOcrEntry) {
            String visitorId = etVisitorId.getText().toString().trim();
            if (!ValidationHelper.isValidAadhaar(visitorId)) {
                tilVisitorId.setError("Valid 12-digit Aadhaar required");
                return;
            }
            visitorIdHash = HashUtils.hashAadhaar(visitorId);
        }

        boolean isVisitingStudent = rgVisitStudent.getCheckedRadioButtonId() == R.id.rbYes;

        if (isVisitingStudent && !isStudentVerified) {
            Toast.makeText(this, "Please provide valid student details!", Toast.LENGTH_LONG).show();
            return;
        }

        String vName = etVisitorName.getText().toString().trim();
        String eTime = etEntryTime.getText().toString().trim();
        String regNo = etStudentReg.getText().toString().trim();
        String sName = etStudentName.getText().toString().trim();

        if (vName.isEmpty()) {
            etVisitorName.setError("Visitor name is required");
            return;
        }

        setLoading(true);
        blacklistManager.checkBlacklist(vName, new BlacklistManager.BlacklistCheckListener() {
            @Override
            public void onBlacklisted(String name) {
                setLoading(false);
                showBlacklistBlockDialog(vName);
            }

            @Override
            public void onNotBlacklisted() {
                proceedWithRegistration(sName, regNo, vName, eTime);
            }
        });
    }

    private void showBlacklistBlockDialog(String name) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ SECURITY BLOCK")
                .setMessage("REGISTRATION DENIED\n\nIndividual '" + name + "' is on the security restricted list.")
                .setPositiveButton("ACKNOWLEDGE", null)
                .setCancelable(false)
                .show();
    }

    private void proceedWithRegistration(String sName, String regNo, String vName, String eTime) {
        boolean isVisitingStudent = rgVisitStudent.getCheckedRadioButtonId() == R.id.rbYes;

        Map<String, Object> visitorData = new HashMap<>();
        visitorData.put(FirestoreConstants.FIELD_HAS_STUDENT_VISIT, isVisitingStudent);
        visitorData.put(FirestoreConstants.FIELD_STUDENT_NAME, isVisitingStudent ? sName : "N/A");
        visitorData.put(FirestoreConstants.FIELD_STUDENT_REG, isVisitingStudent ? regNo : "N/A");
        visitorData.put(FirestoreConstants.FIELD_VISITOR_NAME, vName);
        visitorData.put(FirestoreConstants.FIELD_VISITOR_ID_HASH, visitorIdHash);
        visitorData.put(FirestoreConstants.FIELD_VISITOR_NAME_LOWER, vName.toLowerCase());
        visitorData.put(FirestoreConstants.FIELD_ENTRY_TIME, Timestamp.now());
        visitorData.put(FirestoreConstants.FIELD_TIMESTAMP, System.currentTimeMillis());
        visitorData.put(FirestoreConstants.FIELD_STATUS, FirestoreConstants.STATUS_PENDING);

        repository.saveVisitor(visitorData).addOnSuccessListener(documentReference -> {
            setLoading(false);
            Toast.makeText(this, "Gate Pass Saved Successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, DisplayQrActivity.class);
            intent.putExtra(Constants.EXTRA_STUDENT_NAME, sName);
            intent.putExtra(Constants.EXTRA_STUDENT_REG, regNo);
            intent.putExtra(Constants.EXTRA_ENTRY_TIME, eTime);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentReference.getId());
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setLoading(boolean isLoading) {
        btnSubmit.setEnabled(!isLoading);
        btnSubmit.setText(isLoading ? "" : "GENERATE PASS");
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}