package com.example.gatepass.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.R;
import com.example.gatepass.models.Visitor;
import com.example.gatepass.adapters.VisitorAdapter;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ActiveVisitorsActivity extends BaseActivity {

    private VisitorAdapter adapter;
    private final List<Visitor> visitorList = new ArrayList<>();
    private View llEmptyState;
    private View progressBar;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_list);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Active Visitors");

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        SearchView searchView = findViewById(R.id.searchView);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressBar = findViewById(R.id.progressBar);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        adapter = new VisitorAdapter(visitorList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchActiveVisitors();

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

    private void fetchActiveVisitors() {
        progressBar.setVisibility(View.VISIBLE);
        
        registration = repository.getVisitorsByStatusQuery(FirestoreConstants.STATUS_PENDING)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Failed to load visitors: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    visitorList.clear();
                    List<String> docIds = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Visitor visitor = doc.toObject(Visitor.class);
                            if (visitor.visitorName == null) visitor.visitorName = doc.getString("parentName");

                            visitorList.add(visitor);
                            docIds.add(doc.getId());
                        }
                    }
                    adapter.updateList(new ArrayList<>(visitorList), docIds);
                    llEmptyState.setVisibility(visitorList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}