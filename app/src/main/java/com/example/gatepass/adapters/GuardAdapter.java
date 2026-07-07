package com.example.gatepass.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.R;
import com.example.gatepass.models.Guard;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class GuardAdapter extends RecyclerView.Adapter<GuardAdapter.GuardViewHolder> {

    private List<Guard> guardList;
    private OnGuardActionListener listener;

    public interface OnGuardActionListener {
        void onEdit(Guard guard);
        void onDelete(Guard guard);
    }

    public GuardAdapter(List<Guard> guardList, OnGuardActionListener listener) {
        this.guardList = guardList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GuardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guard, parent, false);
        return new GuardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuardViewHolder holder, int position) {
        Guard guard = guardList.get(position);
        holder.tvUsername.setText(guard.getUsername());
        holder.tvStatus.setText("Status: " + (guard.isActive() ? "Active" : "Disabled"));
        holder.tvStatus.setTextColor(guard.isActive() ? 0xFF4CAF50 : 0xFFF44336);

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(guard));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(guard));
    }

    @Override
    public int getItemCount() {
        return guardList.size();
    }

    static class GuardViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvStatus;
        MaterialButton btnEdit, btnDelete;

        public GuardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}