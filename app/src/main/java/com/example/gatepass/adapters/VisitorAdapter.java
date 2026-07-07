package com.example.gatepass.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gatepass.R;
import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.models.Visitor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.VisitorViewHolder> {

    private List<Visitor> visitors;
    private List<Visitor> visitorsFull;
    private OnVisitorClickListener listener;

    public void setOnVisitorClickListener(OnVisitorClickListener listener) {
        this.listener = listener;
    }

    public interface OnVisitorClickListener {
        void onVisitorClick(Visitor visitor, String docId);
    }

    private List<String> docIds = new ArrayList<>();
    private List<String> docIdsFull = new ArrayList<>();

    public VisitorAdapter(List<Visitor> visitors) {
        this.visitors = visitors;
        this.visitorsFull = new ArrayList<>(visitors);
    }

    public VisitorAdapter(List<Visitor> visitors, List<String> docIds, OnVisitorClickListener listener) {
        this.visitors = visitors;
        this.visitorsFull = new ArrayList<>(visitors);
        this.docIds = docIds;
        this.docIdsFull = new ArrayList<>(docIds);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VisitorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_visitor, parent, false);
        return new VisitorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VisitorViewHolder holder, int position) {
        Visitor visitor = visitors.get(position);
        
        // Prioritize Visitor Name in big bold text
        String displayName = visitor.visitorName != null ? visitor.visitorName : "Unknown Visitor";
        holder.tvStudentName.setText(displayName);
        holder.tvStudentReg.setText("Reg: " + visitor.studentReg + " · " + visitor.studentName);
        holder.tvVisitorName.setText("Visitor Name"); // Keep ID reference but hidden or secondary
        holder.tvVisitorName.setVisibility(View.GONE);
        
        // Initials Avatar
        if (displayName != null && !displayName.isEmpty()) {
            String initials;
            String[] parts = displayName.trim().split("\\s+");
            if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                initials = (String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)).toUpperCase();
            } else if (parts.length >= 1 && !parts[0].isEmpty()) {
                initials = String.valueOf(parts[0].charAt(0)).toUpperCase();
            } else {
                initials = "??";
            }
            holder.tvAvatar.setText(initials);
            
            // Generate stable color based on name
            int colorIndex = Math.abs(displayName.hashCode()) % AVATAR_COLORS.length;
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setColor(Color.parseColor(AVATAR_COLORS[colorIndex]));
            holder.tvAvatar.setBackground(avatarBg);
        } else {
            holder.tvAvatar.setText("??");
        }

        // Status Badge Logic
        int statusColor;
        String statusText;
        String timeInfo;

        if (FirestoreConstants.STATUS_PENDING.equals(visitor.status)) {
            statusText = "INSIDE";
            statusColor = Color.parseColor("#4CAF50"); // Green
            timeInfo = "Entered: " + visitor.getFormattedEntryTime();
        } else {
            statusText = "LEFT";
            statusColor = Color.parseColor("#757575"); // Gray
            
            String exitTimeStr = (visitor.exitTimestamp > 0) 
                ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(visitor.exitTimestamp))
                : (visitor.exitTime != null ? visitor.exitTime : "---");
            timeInfo = "In: " + visitor.getFormattedEntryTime() + " | Out: " + exitTimeStr;
        }

        holder.tvStatus.setText(statusText);
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(12f);
        badge.setColor(statusColor);
        holder.tvStatus.setBackground(badge);
        
        holder.tvTime.setText(timeInfo);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && !docIds.isEmpty()) {
                listener.onVisitorClick(visitor, docIds.get(position));
            }
        });
    }

    private static final String[] AVATAR_COLORS = {
        "#1e3a8a", "#1e40af", "#1d4ed8", "#2563eb", "#3b82f6", 
        "#1e3a5a", "#1e407f", "#1d4e98", "#2563bb", "#3b82d6"
    };

    @Override
    public int getItemCount() {
        return visitors.size();
    }

    public void updateList(List<Visitor> newList) {
        this.visitors = newList;
        this.visitorsFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void updateList(List<Visitor> newList, List<String> newIds) {
        this.visitors = newList;
        this.visitorsFull = new ArrayList<>(newList);
        this.docIds = newIds;
        this.docIdsFull = new ArrayList<>(newIds);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        List<Visitor> filteredList = new ArrayList<>();
        List<String> filteredIds = new ArrayList<>();
        String query = text.toLowerCase();
        
        for (int i = 0; i < visitorsFull.size(); i++) {
            Visitor item = visitorsFull.get(i);
            boolean nameMatch = item.studentName != null && item.studentName.toLowerCase().contains(query);
            boolean regMatch = item.studentReg != null && item.studentReg.toLowerCase().contains(query);
            boolean visitorMatch = item.visitorName != null && item.visitorName.toLowerCase().contains(query);
            
            if (nameMatch || regMatch || visitorMatch) {
                filteredList.add(item);
                if (!docIdsFull.isEmpty()) {
                    filteredIds.add(docIdsFull.get(i));
                }
            }
        }
        visitors = filteredList;
        docIds = filteredIds;
        notifyDataSetChanged();
    }

    static class VisitorViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentReg, tvVisitorName, tvTime, tvStatus, tvAvatar;

        public VisitorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentReg = itemView.findViewById(R.id.tvStudentReg);
            tvVisitorName = itemView.findViewById(R.id.tvVisitorName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
        }
    }
}
