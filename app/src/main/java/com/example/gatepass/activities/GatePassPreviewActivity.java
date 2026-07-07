package com.example.gatepass.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gatepass.utils.Constants;
import com.example.gatepass.utils.DateTimeUtils;
import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.utils.HashUtils;
import com.example.gatepass.R;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

public class GatePassPreviewActivity extends BaseActivity {

    private TextView previewStudentName, previewStudentReg, previewVisitorName, previewEntryTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_gate_pass_preview);

        previewStudentName = findViewById(R.id.previewStudentName);
        previewStudentReg = findViewById(R.id.previewStudentReg);
        previewVisitorName = findViewById(R.id.previewVisitorName);
        previewEntryTime = findViewById(R.id.previewEntryTime);
        
        MaterialButton btnDone = findViewById(R.id.btnDone);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        String studentName = getIntent().getStringExtra(Constants.EXTRA_STUDENT_NAME);
        String studentReg = getIntent().getStringExtra(Constants.EXTRA_STUDENT_REG);
        String visitorName = getIntent().getStringExtra(Constants.EXTRA_VISITOR_NAME);
        String visitorId = getIntent().getStringExtra(Constants.EXTRA_VISITOR_ID);
        String existingDocId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        boolean hasStudentVisit = getIntent().getBooleanExtra(FirestoreConstants.FIELD_HAS_STUDENT_VISIT, false);
        String entryTime = getIntent().getStringExtra(Constants.EXTRA_ENTRY_TIME);
        if (entryTime == null) entryTime = DateTimeUtils.getCurrentTime();

        previewStudentName.setText(studentName);
        previewStudentReg.setText("Reg No: " + studentReg);
        previewVisitorName.setText(visitorName);
        previewEntryTime.setText("Entry Time: " + entryTime);

        // this screen is opened either to view an already-saved pass (existingDocId present,
        // e.g. from dashboard search) or to confirm issuing a brand new one (no docId yet)
        String finalEntryTime = entryTime;
        if (existingDocId != null && !existingDocId.isEmpty()) {
            btnDone.setText("View Pass");
            btnDone.setOnClickListener(v -> openExistingPass(existingDocId, visitorName, studentName, studentReg, finalEntryTime));
        } else {
            btnDone.setOnClickListener(v -> saveToFirestore(studentName, studentReg, visitorName, visitorId, finalEntryTime, hasStudentVisit));
        }
    }

    private void openExistingPass(String docId, String visitorName, String studentName, String studentReg, String entryTime) {
        Intent intent = new Intent(this, DisplayQrActivity.class);
        intent.putExtra(Constants.EXTRA_VISITOR_NAME, visitorName);
        intent.putExtra(Constants.EXTRA_STUDENT_NAME, studentName);
        intent.putExtra(Constants.EXTRA_STUDENT_REG, studentReg);
        intent.putExtra(Constants.EXTRA_ENTRY_TIME, entryTime);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, docId);
        startActivity(intent);
        finish();
    }

    private void saveToFirestore(String studentName, String studentReg, String visitorName, String visitorId, String entryTime, boolean hasStudentVisit) {
        Map<String, Object> visitor = new HashMap<>();
        visitor.put(FirestoreConstants.FIELD_HAS_STUDENT_VISIT, hasStudentVisit);
        visitor.put(FirestoreConstants.FIELD_STUDENT_NAME, studentName);
        visitor.put(FirestoreConstants.FIELD_STUDENT_REG, studentReg);
        visitor.put(FirestoreConstants.FIELD_VISITOR_NAME, visitorName);
        visitor.put(FirestoreConstants.FIELD_VISITOR_NAME_LOWER, visitorName.toLowerCase());
        visitor.put(FirestoreConstants.FIELD_VISITOR_ID_HASH, HashUtils.hashAadhaar(visitorId));
        visitor.put(FirestoreConstants.FIELD_ENTRY_TIME, entryTime);
        visitor.put(FirestoreConstants.FIELD_TIMESTAMP, System.currentTimeMillis());
        visitor.put(FirestoreConstants.FIELD_STATUS, FirestoreConstants.STATUS_PENDING);

        repository.saveVisitor(visitor)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Gate Pass Issued", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, DisplayQrActivity.class);
                    intent.putExtra(Constants.EXTRA_VISITOR_NAME, visitorName);
                    intent.putExtra(Constants.EXTRA_STUDENT_NAME, studentName);
                    intent.putExtra(Constants.EXTRA_STUDENT_REG, studentReg);
                    intent.putExtra(Constants.EXTRA_ENTRY_TIME, entryTime);
                    intent.putExtra(Constants.EXTRA_DOCUMENT_ID, documentReference.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}