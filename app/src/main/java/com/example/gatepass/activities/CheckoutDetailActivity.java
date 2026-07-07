package com.example.gatepass.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gatepass.utils.Constants;
import com.example.gatepass.utils.DateTimeUtils;
import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.R;
import com.example.gatepass.models.Visitor;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class CheckoutDetailActivity extends BaseActivity {

    private TextView tvStudentName, tvStudentReg, tvVisitorName, tvEntryTime;
    private String documentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_checkout_detail);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);

        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentReg = findViewById(R.id.tvStudentReg);
        tvVisitorName = findViewById(R.id.tvVisitorName);
        tvEntryTime = findViewById(R.id.tvEntryTime);
        MaterialButton btnConfirmCheckout = findViewById(R.id.btnConfirmCheckout);
        MaterialButton btnCancel = findViewById(R.id.btnCancel);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        loadVisitorDetails();

        btnConfirmCheckout.setOnClickListener(v -> performCheckout());
    }

    private void loadVisitorDetails() {
        if (documentId == null) return;

        repository.getVisitorReference(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Visitor visitor = documentSnapshot.toObject(Visitor.class);
                        if (visitor != null) {
                            tvStudentName.setText(visitor.studentName);
                            tvStudentReg.setText("Reg No: " + visitor.studentReg);
                            tvVisitorName.setText(visitor.visitorName);
                            tvEntryTime.setText("Entry: " + visitor.getFormattedEntryTime());
                        }
                    } else {
                        Toast.makeText(this, "Visitor not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void performCheckout() {
        if (documentId == null) return;
        
        final DocumentReference docRef = repository.getVisitorReference(documentId);

        repository.performCheckoutTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(docRef);
            
            String status = snapshot.getString(FirestoreConstants.FIELD_STATUS);
            if (FirestoreConstants.STATUS_LEFT.equalsIgnoreCase(status)) {
                throw new FirebaseFirestoreException("Visitor already checked out", 
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put(FirestoreConstants.FIELD_STATUS, FirestoreConstants.STATUS_LEFT);
            updates.put(FirestoreConstants.FIELD_EXIT_TIME, DateTimeUtils.getCurrentTime());
            updates.put(FirestoreConstants.FIELD_EXIT_TIMESTAMP, System.currentTimeMillis());
            
            transaction.update(docRef, updates);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Checkout Successful", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException && 
                ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.ABORTED) {
                Toast.makeText(this, "Error: This pass has already been checked out!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Checkout Failed: Network or Server Error", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }
}