package com.example.gatepass.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.utils.HashUtils;
import com.example.gatepass.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends BaseActivity {

    private TextInputEditText etOldPassword, etNewPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_change_password);

        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        MaterialButton btnUpdate = findViewById(R.id.btnUpdate);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> performPasswordUpdate());
    }

    private void performPasswordUpdate() {
        String oldPw = etOldPassword.getText().toString().trim();
        String newPw = etNewPassword.getText().toString().trim();
        String confirmPw = etConfirmPassword.getText().toString().trim();
        String username = sessionManager.getUsername();
        String role = sessionManager.getRole();

        if (oldPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPw.equals(confirmPw)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify old password using Repository
        repository.getLoginQuery(username, role).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                String storedPassword = doc.getString(FirestoreConstants.FIELD_PASSWORD);

                if (HashUtils.verifyPassword(oldPw, storedPassword)) {
                    updatePasswordInFirestore(doc.getId(), newPw);
                } else {
                    Toast.makeText(this, "Old password incorrect", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updatePasswordInFirestore(String docId, String newPw) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirestoreConstants.FIELD_PASSWORD, HashUtils.hashString(newPw));

        repository.updateUser(docId, updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Password updated. Please login again.", Toast.LENGTH_LONG).show();
                    sessionManager.logoutUser();
                    finishAffinity();
                    startActivity(new Intent(this, LoginActivity.class));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show());
    }
}