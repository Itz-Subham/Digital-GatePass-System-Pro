package com.example.gatepass.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.R;
import com.example.gatepass.models.SearchResult;

import java.util.List;

public class LiveSearchAdapter extends RecyclerView.Adapter<LiveSearchAdapter.ViewHolder> {
    private final List<SearchResult> results;
    private final OnResultClickListener listener;

    public interface OnResultClickListener {
        void onClick(SearchResult result);
    }

    public LiveSearchAdapter(List<SearchResult> results, OnResultClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_live_search, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult res = results.get(position);
        holder.tvName.setText(res.name);
        holder.tvReg.setText(res.regNo);
        holder.tvStatus.setText(res.status);

        if ("RESTRICTED".equals(res.status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_badge_bg);
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_badge_bg);
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(res.status.equals("Inside") ? 0xFF4CAF50 : Color.GRAY));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(res));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvReg, tvStatus;

        public ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvReg = v.findViewById(R.id.tvRegNo);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}