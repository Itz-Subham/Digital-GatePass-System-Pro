package com.example.gatepass.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.utils.Constants;
import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.adapters.LiveSearchAdapter;
import com.example.gatepass.R;
import com.example.gatepass.models.SearchResult;
import com.example.gatepass.models.Visitor;
import com.example.gatepass.adapters.VisitorAdapter;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// shared dashboard for admin + security, features toggle based on role
public class DashboardActivity extends BaseActivity {

    private TextView tvDateTime, tvDashboardTitle, tvTodayEntries, tvActiveInside;
    private TextView tabAll, tabInside, tabLeft;
    private RecyclerView rvRecentEntries, rvLiveSearch;
    private VisitorAdapter recentAdapter;
    private LiveSearchAdapter searchAdapter;
    private final List<SearchResult> searchResults = new ArrayList<>();
    
    private final List<Visitor> allRecentVisitors = new ArrayList<>();
    private final List<String> recentDocIds = new ArrayList<>();

    private ListenerRegistration visitorListener;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Security Check: Standard across both roles now
        if (!checkLogin()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        initializeUI();
        setupRecentEntries();
        setupTabs();
        setupLiveSearch();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRealTimeListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (visitorListener != null) visitorListener.remove();
    }

    private void initializeUI() {
        tvDateTime = findViewById(R.id.tvDateTime);
        tvDashboardTitle = findViewById(R.id.tvDashboardTitle);
        tvTodayEntries = findViewById(R.id.tvTodayEntries);
        tvActiveInside = findViewById(R.id.tvActiveInside);
        
        tabAll = findViewById(R.id.tabAll);
        tabInside = findViewById(R.id.tabInside);
        tabLeft = findViewById(R.id.tabLeft);
        rvRecentEntries = findViewById(R.id.rvRecentEntries);
        rvLiveSearch = findViewById(R.id.rvLiveSearch);

        // Role-based title
        String role = getUserRole();
        if (FirestoreConstants.ROLE_ADMIN.equals(role)) {
            tvDashboardTitle.setText("Admin Dashboard");
        } else {
            tvDashboardTitle.setText("Security Dashboard");
        }

        updateDateTime();

        // Primary Grid Actions
        findViewById(R.id.cardScan).setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.btnCheckout).setOnClickListener(v -> startActivity(new Intent(this, CheckoutScannerActivity.class)));
        findViewById(R.id.btnActiveList).setOnClickListener(v -> startActivity(new Intent(this, ActiveVisitorsActivity.class)));
        findViewById(R.id.btnBlacklist).setOnClickListener(v -> startActivity(new Intent(this, SecurityListActivity.class)));
        findViewById(R.id.btnManualForm).setOnClickListener(v -> startActivity(new Intent(this, GatePassFormActivity.class)));

        // Profile Menu
        findViewById(R.id.ivAvatar).setOnClickListener(this::showProfileMenu);
    }

    private void showProfileMenu(View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, anchor);
        String username = sessionManager.getUsername();
        String role = getUserRole();
        
        popup.getMenu().add(0, 0, 0, "Hello, " + username).setEnabled(false);
        popup.getMenu().add(0, 1, 1, "Visitor History");
        
        // Admins only: Manage Guards
        if (FirestoreConstants.ROLE_ADMIN.equals(role)) {
            popup.getMenu().add(0, 2, 2, "Manage Guards");
        }
        
        popup.getMenu().add(0, 3, 3, "Change Password");
        popup.getMenu().add(0, 4, 4, "Logout");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    startActivity(new Intent(this, HistoryActivity.class));
                    return true;
                case 2:
                    startActivity(new Intent(this, ManageGuardsActivity.class));
                    return true;
                case 3:
                    startActivity(new Intent(this, ChangePasswordActivity.class));
                    return true;
                case 4:
                    sessionManager.logoutUser();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void updateDateTime() {
        String currentDateTime = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvDateTime.setText(currentDateTime);
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> filterRecent("All"));
        tabInside.setOnClickListener(v -> filterRecent("Inside"));
        tabLeft.setOnClickListener(v -> filterRecent("Left"));
    }

    private void filterRecent(String type) {
        tabAll.setBackgroundResource(R.drawable.bg_tab_inactive);
        tabInside.setBackgroundResource(R.drawable.bg_tab_inactive);
        tabLeft.setBackgroundResource(R.drawable.bg_tab_inactive);
        tabAll.setTextColor(Color.parseColor("#666666"));
        tabInside.setTextColor(Color.parseColor("#666666"));
        tabLeft.setTextColor(Color.parseColor("#666666"));

        List<Visitor> filtered = new ArrayList<>();
        List<String> filteredIds = new ArrayList<>();

        switch (type) {
            case "All":
                tabAll.setBackgroundResource(R.drawable.bg_tab_active);
                tabAll.setTextColor(Color.WHITE);
                filtered.addAll(allRecentVisitors);
                filteredIds.addAll(recentDocIds);
                break;
            case "Inside":
                tabInside.setBackgroundResource(R.drawable.bg_tab_active);
                tabInside.setTextColor(Color.WHITE);
                for (int i = 0; i < allRecentVisitors.size(); i++) {
                    if (FirestoreConstants.STATUS_PENDING.equals(allRecentVisitors.get(i).status)) {
                        filtered.add(allRecentVisitors.get(i));
                        filteredIds.add(recentDocIds.get(i));
                    }
                }
                break;
            case "Left":
                tabLeft.setBackgroundResource(R.drawable.bg_tab_active);
                tabLeft.setTextColor(Color.WHITE);
                for (int i = 0; i < allRecentVisitors.size(); i++) {
                    if (FirestoreConstants.STATUS_LEFT.equalsIgnoreCase(allRecentVisitors.get(i).status)) {
                        filtered.add(allRecentVisitors.get(i));
                        filteredIds.add(recentDocIds.get(i));
                    }
                }
                break;
        }
        recentAdapter.updateList(filtered, filteredIds);
    }

    private void setupRecentEntries() {
        recentAdapter = new VisitorAdapter(new ArrayList<>());
        rvRecentEntries.setLayoutManager(new LinearLayoutManager(this));
        rvRecentEntries.setAdapter(recentAdapter);
        
        recentAdapter.setOnVisitorClickListener((visitor, docId) -> {
            Intent intent = new Intent(this, CheckoutDetailActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, docId);
            startActivity(intent);
        });
    }

    private void setupLiveSearch() {
        SearchView searchView = findViewById(R.id.searchView);
        searchAdapter = new LiveSearchAdapter(searchResults, result -> {
            if ("RESTRICTED".equals(result.status)) {
                showRestrictedWarning(result);
            } else {
                openPassPreview(result);
            }
        });
        rvLiveSearch.setLayoutManager(new LinearLayoutManager(this));
        rvLiveSearch.setAdapter(searchAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performLiveSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                if (newText.length() > 1) {
                    searchRunnable = () -> performLiveSearch(newText);
                    searchHandler.postDelayed(searchRunnable, 300);
                } else {
                    searchResults.clear();
                    searchAdapter.notifyDataSetChanged();
                    rvLiveSearch.setVisibility(View.GONE);
                }
                return false;
            }
        });
    }

    private void performLiveSearch(String query) {
        String lowerQuery = query.toLowerCase();
        
        repository.getVisitorSearchQuery(FirestoreConstants.FIELD_VISITOR_NAME_LOWER, lowerQuery)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    searchResults.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Visitor visitor = doc.toObject(Visitor.class);
                        String currentStatus = visitor.status;
                        String displayStatus = FirestoreConstants.STATUS_PENDING.equals(currentStatus) ? "Inside" : "Left";
                        
                        searchResults.add(new SearchResult(doc.getId(), visitor.visitorName, visitor.studentReg, displayStatus, visitor.studentName, visitor.getFormattedEntryTime(), visitor.hasStudentVisit));
                    }
                    
                    if (searchResults.isEmpty()) {
                         repository.getVisitorSearchQuery(FirestoreConstants.FIELD_STUDENT_REG, query)
                            .get()
                            .addOnSuccessListener(regSnapshots -> {
                                for (QueryDocumentSnapshot doc : regSnapshots) {
                                    Visitor visitor = doc.toObject(Visitor.class);
                                    String currentStatus = visitor.status;
                                    String displayStatus = FirestoreConstants.STATUS_PENDING.equals(currentStatus) ? "Inside" : "Left";
                                    searchResults.add(new SearchResult(doc.getId(), visitor.visitorName, visitor.studentReg, displayStatus, visitor.studentName, visitor.getFormattedEntryTime(), visitor.hasStudentVisit));
                                }
                                searchBlacklist(lowerQuery);
                            });
                    } else {
                        searchBlacklist(lowerQuery);
                    }
                });
    }

    private void searchBlacklist(String lowerQuery) {
        repository.getBlacklistSearchQuery(lowerQuery)
                .get()
                .addOnSuccessListener(blacklistSnapshots -> {
                    for (QueryDocumentSnapshot doc : blacklistSnapshots) {
                        String name = doc.getString(FirestoreConstants.FIELD_NAME);
                        String id = doc.getString(FirestoreConstants.FIELD_ID);
                        searchResults.add(0, new SearchResult(doc.getId(), name, "ID: " + id, "RESTRICTED", "", "", false));
                    }
                    searchAdapter.notifyDataSetChanged();
                    rvLiveSearch.setVisibility(searchResults.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void showRestrictedWarning(SearchResult result) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ SECURITY ALERT")
                .setMessage("RESTRICTED INDIVIDUAL: " + result.name + "\nAction: Do not issue gate pass. Contact supervisor.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void openPassPreview(SearchResult result) {
        Intent intent = new Intent(this, GatePassPreviewActivity.class);
        intent.putExtra(Constants.EXTRA_STUDENT_NAME, result.studentName);
        intent.putExtra(Constants.EXTRA_STUDENT_REG, result.regNo);
        intent.putExtra(Constants.EXTRA_VISITOR_NAME, result.name);
        intent.putExtra(Constants.EXTRA_ENTRY_TIME, result.entryTime);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, result.id);
        intent.putExtra(FirestoreConstants.FIELD_HAS_STUDENT_VISIT, result.hasStudentVisit);
        startActivity(intent);
    }

    private void startRealTimeListeners() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        long startOfToday = cal.getTimeInMillis();

        visitorListener = repository.getRecentVisitorsQuery(startOfToday)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    allRecentVisitors.clear();
                    recentDocIds.clear();
                    int totalToday = value.size();
                    int insideCount = 0;

                    for (QueryDocumentSnapshot doc : value) {
                        Visitor visitor = doc.toObject(Visitor.class);
                        if (visitor.visitorName == null) visitor.visitorName = doc.getString("parentName");
                        
                        allRecentVisitors.add(visitor);
                        recentDocIds.add(doc.getId());

                        if (FirestoreConstants.STATUS_PENDING.equals(doc.getString(FirestoreConstants.FIELD_STATUS))) {
                            insideCount++;
                        }
                    }
                    filterRecent("All");
                    tvTodayEntries.setText(String.valueOf(totalToday));
                    tvActiveInside.setText(String.valueOf(insideCount));
                });
    }
}
