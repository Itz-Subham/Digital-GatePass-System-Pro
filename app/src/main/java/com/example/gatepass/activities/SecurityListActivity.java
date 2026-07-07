package com.example.gatepass.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.adapters.BlacklistAdapter;
import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityListActivity extends BaseActivity {

    private BlacklistAdapter adapter;
    private final List<Map<String, String>> blacklist = new ArrayList<>();
    private View llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_list);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        llEmptyState = findViewById(R.id.llEmptyState);
        FloatingActionButton fab = findViewById(R.id.fabAddBlacklist);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        adapter = new BlacklistAdapter(blacklist, this::unrestrictPerson);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchBlacklist();

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualSelectionActivity.class);
            intent.putExtra("ACTION", ManualSelectionActivity.ACTION_BLACKLIST);
            startActivity(intent);
        });
    }

    private void fetchBlacklist() {
        repository.getBlacklistAllQuery()
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    blacklist.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, String> item = new HashMap<>();
                            item.put(FirestoreConstants.FIELD_NAME, doc.getString(FirestoreConstants.FIELD_NAME));
                            item.put(FirestoreConstants.FIELD_ID, doc.getString(FirestoreConstants.FIELD_ID));
                            item.put("docId", doc.getId());
                            blacklist.add(item);
                        }
                    }
                    adapter.updateData(blacklist);
                    llEmptyState.setVisibility(blacklist.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void unrestrictPerson(String docId, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Unrestrict Visitor")
                .setMessage("Are you sure you want to remove " + name + " from the restricted list?")
                .setPositiveButton("Unrestrict", (dialog, which) -> {
                    repository.removeFromBlacklist(docId)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, name + " unrestriced", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}