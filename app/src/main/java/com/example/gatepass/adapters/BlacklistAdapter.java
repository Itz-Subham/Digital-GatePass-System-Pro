package com.example.gatepass.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.R;
import com.example.gatepass.firebase.FirestoreConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlacklistAdapter extends RecyclerView.Adapter<BlacklistAdapter.ViewHolder> {
    private final List<Map<String, String>> data;
    private final OnUnrestrictListener listener;

    public interface OnUnrestrictListener {
        void onUnrestrict(String docId, String name);
    }

    public BlacklistAdapter(List<Map<String, String>> data, OnUnrestrictListener listener) {
        this.data = new ArrayList<>(data);
        this.listener = listener;
    }

    public void updateData(List<Map<String, String>> newData) {
        this.data.clear();
        this.data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blacklist, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = data.get(position);
        holder.tvName.setText(item.get(FirestoreConstants.FIELD_NAME));
        holder.tvId.setText("ID (Hashed): " + item.get(FirestoreConstants.FIELD_ID));
        holder.btnUnrestrict.setOnClickListener(v -> {
            if (listener != null) listener.onUnrestrict(item.get("docId"), item.get(FirestoreConstants.FIELD_NAME));
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvId;
        ImageView btnUnrestrict;

        public ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvId = v.findViewById(R.id.tvId);
            btnUnrestrict = v.findViewById(R.id.btnUnrestrict);
        }
    }
}