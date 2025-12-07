package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.ScheduleItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    public interface OnScheduleActionListener {
        void onItemClick(ScheduleItem item);

        void onItemLongClick(View view, ScheduleItem item);
    }

    private Context context;
    private List<ScheduleItem> items;
    private OnScheduleActionListener listener;
    private static final int TIMELINE_HEIGHT_DP = 720; // 24 hours * 30dp per hour
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ScheduleAdapter(Context context, List<ScheduleItem> items, OnScheduleActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItem it = items.get(position);
        String start = it.getStartTime() != null ? it.getStartTime() : "";
        String end = it.getEndTime() != null ? it.getEndTime() : "";
        String timeRange = start.isEmpty() ? end : (end.isEmpty() ? start : start + " - " + end);
        holder.tvTime.setText(timeRange);
        holder.tvTitle.setText(it.getTitle() != null ? it.getTitle() : "");

        // Set icon (default to calendar emoji)
        // Fixed icon issue
        holder.tvIcon.setText("📅");

        // If notes are empty, show formatted phrase
        String notes = it.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            holder.tvNotes.setText(notes);
        } else {
            String action = it.getTitle() != null ? it.getTitle() : "do something";
            String phrase;
            if (!start.isEmpty() && !end.isEmpty()) {
                phrase = "In that day from " + start + " to " + end + ", " + action;
            } else if (!start.isEmpty()) {
                phrase = "In that day from " + start + ", " + action;
            } else {
                phrase = action;
            }
            holder.tvNotes.setText(phrase);
        }

        // Set location (show only if not empty)
        String location = it.getLocation();
        if (location != null && !location.trim().isEmpty()) {
            holder.tvLocation.setText(location);
            holder.layoutLocation.setVisibility(View.VISIBLE);
        } else {
            holder.layoutLocation.setVisibility(View.GONE);
        }

        // Position event based on start time
        if (!start.isEmpty()) {
            int topMargin = calculateTimePosition(start);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            if (params == null) {
                params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.MarginLayoutParams.MATCH_PARENT,
                        ViewGroup.MarginLayoutParams.WRAP_CONTENT);
            }
            params.topMargin = topMargin;
            holder.itemView.setLayoutParams(params);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onItemClick(it);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null)
                listener.onItemLongClick(v, it);
            return true;
        });
    }

    private int calculateTimePosition(String timeStr) {
        try {
            // Parse time string (format: "HH:mm")
            String[] parts = timeStr.split(":");
            if (parts.length >= 2) {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);

                // Calculate position: (hour * 30) + (minute / 60.0 * 30)
                // 30dp per hour, so 0.5dp per minute
                float hours = hour + (minute / 60.0f);
                int positionDp = (int) (hours * 30.0f); // 30dp per hour

                // Convert dp to pixels
                float density = context.getResources().getDisplayMetrics().density;
                return (int) (positionDp * density);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<ScheduleItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvNotes, tvIcon, tvLocation;
        View layoutLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_schedule_time);
            tvTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvNotes = itemView.findViewById(R.id.tv_schedule_notes);
            tvIcon = itemView.findViewById(R.id.tv_schedule_icon);
            tvLocation = itemView.findViewById(R.id.tv_schedule_location);
            layoutLocation = itemView.findViewById(R.id.layout_location);
        }
    }
}
