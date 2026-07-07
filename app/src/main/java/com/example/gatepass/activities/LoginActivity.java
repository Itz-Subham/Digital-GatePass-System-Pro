package com.example.gatepass.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.utils.HashUtils;
import com.example.gatepass.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends BaseActivity {

    private TabLayout roleTabLayout;
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin, btnContinue, btnLogout;
    private ProgressBar progressBar;
    private View cardActiveSession, loginFormContainer;
    private TextView tvWelcomeBack, tvRoleDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeUI();

        if (sessionManager.isLoggedIn()) {
            showActiveSessionUI();
        } else {
            showLoginFormUI();
        }

        btnLogin.setOnClickListener(v -> performLogin());
        btnContinue.setOnClickListener(v -> navigateToDashboard(sessionManager.getRole()));
        btnLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            showLoginFormUI();
        });
    }

    private void initializeUI() {
        roleTabLayout = findViewById(R.id.roleTabLayout);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        
        cardActiveSession = findViewById(R.id.cardActiveSession);
        loginFormContainer = findViewById(R.id.loginFormContainer);
        tvWelcomeBack = findViewById(R.id.tvWelcomeBack);
        tvRoleDisplay = findViewById(R.id.tvRoleDisplay);
        btnContinue = findViewById(R.id.btnContinue);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void showActiveSessionUI() {
        loginFormContainer.setVisibility(View.GONE);
        cardActiveSession.setVisibility(View.VISIBLE);
        tvWelcomeBack.setText("Welcome back, " + sessionManager.getUsername());
        String role = sessionManager.getRole();
        tvRoleDisplay.setText("Currently logged in as: " + role.toUpperCase());
    }

    private void showLoginFormUI() {
        cardActiveSession.setVisibility(View.GONE);
        loginFormContainer.setVisibility(View.VISIBLE);
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int selectedTab = roleTabLayout.getSelectedTabPosition();
        String role = (selectedTab == 0) ? FirestoreConstants.ROLE_ADMIN : FirestoreConstants.ROLE_SECURITY;

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        repository.getLoginQuery(username, role).get().addOnSuccessListener(queryDocumentSnapshots -> {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);

            if (!queryDocumentSnapshots.isEmpty()) {
                DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                String storedPassword = doc.getString(FirestoreConstants.FIELD_PASSWORD);
                boolean isActive = !doc.contains(FirestoreConstants.FIELD_ACTIVE) || 
                                  Boolean.TRUE.equals(doc.getBoolean(FirestoreConstants.FIELD_ACTIVE));

                if (!isActive) {
                    Toast.makeText(this, "This account has been disabled.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Verify Password (Hashed or Plaintext Migration)
                if (HashUtils.verifyPassword(password, storedPassword)) {
                    // Safe Migration: If plaintext, hash it and update DB
                    if (!HashUtils.isHashed(storedPassword)) {
                        migrateToHash(doc.getId(), password);
                    }
                    
                    sessionManager.createLoginSession(username, role);
                    navigateToDashboard(role);
                } else {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "User not found or role mismatch", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void migrateToHash(String docId, String plainPassword) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirestoreConstants.FIELD_PASSWORD, HashUtils.hashString(plainPassword));
        repository.updateUser(docId, updates);
    }

    private void navigateToDashboard(String role) {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}