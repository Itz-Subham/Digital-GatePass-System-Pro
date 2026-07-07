package com.example.gatepass.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gatepass.utils.Constants;
import com.example.gatepass.R;
import com.example.gatepass.models.Visitor;
import com.example.gatepass.adapters.VisitorAdapter;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends BaseActivity {

    private VisitorAdapter adapter;
    private final List<Visitor> visitorList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private View llEmptyState;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_list);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Visitor History");

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        SearchView searchView = findViewById(R.id.searchView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        adapter = new VisitorAdapter(visitorList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnVisitorClickListener((visitor, docId) -> {
            android.content.Intent intent = new android.content.Intent(this, CheckoutDetailActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, docId);
            startActivity(intent);
        });

        fetchHistory();

        swipeRefreshLayout.setOnRefreshListener(this::fetchHistory);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });
    }

    private void fetchHistory() {
        if (!swipeRefreshLayout.isRefreshing()) progressBar.setVisibility(View.VISIBLE);
        
        repository.getRecentVisitorsQuery(0) // 0 means all time since timestamp > 0
                .limit(100)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    visitorList.clear();
                    List<String> docIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Visitor visitor = doc.toObject(Visitor.class);
                        if (visitor.visitorName == null) visitor.visitorName = doc.getString("parentName");

                        visitorList.add(visitor);
                        docIds.add(doc.getId());
                    }
                    adapter.updateList(new ArrayList<>(visitorList), docIds);
                    
                    llEmptyState.setVisibility(visitorList.isEmpty() ? View.VISIBLE : View.GONE);
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Network error: check connection", Toast.LENGTH_SHORT).show();
                });
    }
}