package com.example.gatepass.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.models.Guard;
import com.example.gatepass.adapters.GuardAdapter;
import com.example.gatepass.utils.HashUtils;
import com.example.gatepass.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageGuardsActivity extends BaseActivity implements GuardAdapter.OnGuardActionListener {

    private GuardAdapter adapter;
    private List<Guard> guardList;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Security check
        if (!checkLogin() || !FirestoreConstants.ROLE_ADMIN.equals(getUserRole())) {
            finish();
            return;
        }

        setContentView(R.layout.activity_manage_guards);

        RecyclerView rvGuards = findViewById(R.id.rvGuards);
        progressBar = findViewById(R.id.progressBar);
        FloatingActionButton fabAddGuard = findViewById(R.id.fabAddGuard);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        guardList = new ArrayList<>();
        adapter = new GuardAdapter(guardList, this);
        rvGuards.setLayoutManager(new LinearLayoutManager(this));
        rvGuards.setAdapter(adapter);

        fabAddGuard.setOnClickListener(v -> showGuardDialog(null));

        fetchGuards();
    }

    private void fetchGuards() {
        progressBar.setVisibility(View.VISIBLE);
        repository.getGuardsQuery()
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Error fetching guards", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    guardList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Guard guard = doc.toObject(Guard.class);
                            guard.setId(doc.getId());
                            guardList.add(guard);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showGuardDialog(Guard guard) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_guard, null);
        EditText etUsername = view.findViewById(R.id.etGuardUsername);
        EditText etPassword = view.findViewById(R.id.etGuardPassword);
        SwitchMaterial swActive = view.findViewById(R.id.swActive);

        boolean isEdit = guard != null;
        if (isEdit) {
            etUsername.setText(guard.getUsername());
            etPassword.setText(""); // Don't show hashed password
            etPassword.setHint("Enter new password (or leave blank)");
            swActive.setChecked(guard.isActive());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(isEdit ? "Edit Guard" : "Add New Guard")
                .setView(view)
                .setPositiveButton(isEdit ? "Update" : "Add", (dialog, which) -> {
                    String username = etUsername.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    boolean active = swActive.isChecked();

                    if (username.isEmpty() || (!isEdit && password.isEmpty())) {
                        Toast.makeText(this, "Required fields are empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveGuard(guard, username, password, active);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveGuard(Guard guard, String username, String password, boolean active) {
        Map<String, Object> data = new HashMap<>();
        data.put(FirestoreConstants.FIELD_USERNAME, username);
        data.put(FirestoreConstants.FIELD_ACTIVE, active);
        data.put(FirestoreConstants.FIELD_ROLE, FirestoreConstants.ROLE_SECURITY);
        
        // Only update password if provided
        if (!password.isEmpty()) {
            data.put(FirestoreConstants.FIELD_PASSWORD, HashUtils.hashString(password));
        }

        if (guard == null) {
            repository.addUser(data)
                    .addOnSuccessListener(ref -> Toast.makeText(this, "Guard added", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show());
        } else {
            repository.updateUser(guard.getId(), data)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Guard updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onEdit(Guard guard) {
        showGuardDialog(guard);
    }

    @Override
    public void onDelete(Guard guard) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Guard")
                .setMessage("Are you sure you want to delete guard " + guard.getUsername() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteUser(guard.getId())
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Guard deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}