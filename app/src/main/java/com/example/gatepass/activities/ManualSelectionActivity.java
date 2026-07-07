package com.example.gatepass.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.utils.Constants;
import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.R;
import com.example.gatepass.models.Visitor;
import com.example.gatepass.adapters.VisitorAdapter;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManualSelectionActivity extends BaseActivity {

    public static final String ACTION_CHECKOUT = "CHECKOUT";
    public static final String ACTION_BLACKLIST = "BLACKLIST";

    private VisitorAdapter adapter;
    private final List<Visitor> visitorList = new ArrayList<>();
    private final List<String> docIds = new ArrayList<>();
    private String currentAction;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_manual_selection);

        currentAction = getIntent().getStringExtra("ACTION");
        if (currentAction == null) currentAction = ACTION_CHECKOUT;

        TextView tvTitle = findViewById(R.id.tvSelectionTitle);
        tvTitle.setText(currentAction.equals(ACTION_CHECKOUT) ? "Manual Checkout" : "Restrict Visitor");
        
        TextView tvSubTitle = findViewById(R.id.tvSelectionSubTitle);
        if (tvSubTitle != null) {
            tvSubTitle.setText(currentAction.equals(ACTION_CHECKOUT) 
                ? "Select a visitor currently inside to perform checkout."
                : "Search and select a visitor to add to the restricted list.");
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressBar = findViewById(R.id.progressBar);
        
        adapter = new VisitorAdapter(visitorList, docIds, (visitor, docId) -> {
            if (ACTION_CHECKOUT.equals(currentAction)) {
                Intent intent = new Intent(this, CheckoutDetailActivity.class);
                intent.putExtra(Constants.EXTRA_DOCUMENT_ID, docId);
                startActivity(intent);
                finish();
            } else if (ACTION_BLACKLIST.equals(currentAction)) {
                confirmBlacklist(visitor, docId);
            }
        });
        recyclerView.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 1) {
                    performSearch(newText);
                } else if (newText.isEmpty()) {
                    loadInitialList();
                }
                return false;
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        loadInitialList();
    }

    private void loadInitialList() {
        if (ACTION_CHECKOUT.equals(currentAction)) {
            fetchInsideVisitors();
        } else {
            fetchAllVisitors();
        }
    }

    private void fetchInsideVisitors() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        repository.getVisitorsByStatusQuery(FirestoreConstants.STATUS_PENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    visitorList.clear();
                    docIds.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Visitor v = doc.toObject(Visitor.class);
                        if (v.visitorName == null) v.visitorName = doc.getString("parentName");
                        visitorList.add(v);
                        docIds.add(doc.getId());
                    }
                    adapter.updateList(new ArrayList<>(visitorList), new ArrayList<>(docIds));
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load visitors: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void fetchAllVisitors() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        repository.getRecentVisitorsQuery(0)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    visitorList.clear();
                    docIds.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Visitor v = doc.toObject(Visitor.class);
                        if (v.visitorName == null) v.visitorName = doc.getString("parentName");
                        visitorList.add(v);
                        docIds.add(doc.getId());
                    }
                    adapter.updateList(new ArrayList<>(visitorList), new ArrayList<>(docIds));
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                });
    }

    private void performSearch(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Client-side filter for simplicity in manual selection
        repository.getRecentVisitorsQuery(0)
                .limit(100)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    visitorList.clear();
                    docIds.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Visitor v = doc.toObject(Visitor.class);
                        if (v.visitorName == null) v.visitorName = doc.getString("parentName");
                        
                        boolean matches = (v.visitorName != null && v.visitorName.toLowerCase().contains(lowerQuery)) ||
                                         (v.studentName != null && v.studentName.toLowerCase().contains(lowerQuery)) ||
                                         (v.studentReg != null && v.studentReg.toLowerCase().contains(lowerQuery));

                        if (matches) {
                            if (ACTION_CHECKOUT.equals(currentAction)) {
                                if (FirestoreConstants.STATUS_PENDING.equals(v.status)) {
                                    visitorList.add(v);
                                    docIds.add(doc.getId());
                                }
                            } else {
                                visitorList.add(v);
                                docIds.add(doc.getId());
                            }
                        }
                    }
                    adapter.updateList(new ArrayList<>(visitorList), new ArrayList<>(docIds));
                });
    }

    private void confirmBlacklist(Visitor visitor, String docId) {
        new AlertDialog.Builder(this)
                .setTitle("Restrict Visitor")
                .setMessage("Are you sure you want to add " + visitor.visitorName + " to the restricted list?")
                .setPositiveButton("Confirm Restriction", (dialog, which) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put(FirestoreConstants.FIELD_NAME, visitor.visitorName);
                    data.put(FirestoreConstants.FIELD_NAME_LOWER, visitor.visitorName.toLowerCase());
                    data.put(FirestoreConstants.FIELD_ID, visitor.visitorIdHash);
                    data.put(FirestoreConstants.FIELD_TIMESTAMP, System.currentTimeMillis());
                    
                    repository.addToBlacklist(data)
                            .addOnSuccessListener(doc -> {
                                Toast.makeText(this, visitor.visitorName + " restricted successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}