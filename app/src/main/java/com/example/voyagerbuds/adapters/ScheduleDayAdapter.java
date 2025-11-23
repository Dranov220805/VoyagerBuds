package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.adapters.ScheduleAdapter;
import com.example.voyagerbuds.models.ScheduleDayGroup;
import com.example.voyagerbuds.models.ScheduleItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleDayAdapter extends RecyclerView.Adapter<ScheduleDayAdapter.DayViewHolder> {

    private final Context context;
    private final ScheduleAdapter.OnScheduleActionListener listener;
    private List<ScheduleDayGroup> dayGroups = new ArrayList<>();
    private String selectedDateKey = null; // Date key in format "yyyy-MM-dd"

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public ScheduleDayAdapter(Context context,
            List<ScheduleDayGroup> dayGroups,
            ScheduleAdapter.OnScheduleActionListener listener) {
        this.context = context;
        this.dayGroups = dayGroups;
        this.listener = listener;
    }

    public void setSelectedDateKey(String dateKey) {
        this.selectedDateKey = dateKey;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_day, parent, false);
        return new DayViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(dayGroups.get(position), selectedDateKey);
    }

    @Override
    public int getItemCount() {
        return dayGroups.size();
    }

    public void updateGroups(List<ScheduleDayGroup> groups) {
        this.dayGroups = groups;
        notifyDataSetChanged();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDayTitle;
        private final TextView tvDaySubtitle;
        private final LinearLayout containerEvents;
        private final View viewTimelineContainer;
        private final View itemViewRoot;
        private ScheduleAdapter.OnScheduleActionListener listener;

        DayViewHolder(@NonNull View itemView, ScheduleAdapter.OnScheduleActionListener listener) {
            super(itemView);
            itemViewRoot = itemView;
            this.listener = listener;
            tvDayTitle = itemView.findViewById(R.id.tv_day_title);
            tvDaySubtitle = itemView.findViewById(R.id.tv_day_subtitle);
            containerEvents = itemView.findViewById(R.id.container_events);
            viewTimelineContainer = itemView.findViewById(R.id.container_timeline);
        }

        void bind(ScheduleDayGroup dayGroup, String selectedDateKey) {
            tvDayTitle.setText(dayGroup.getDisplayTitle());

            int eventCount = dayGroup.getEvents().size();
            Context context = itemView.getContext();
            String eventLabel = eventCount == 1
                    ? context.getString(R.string.schedule_event_count_one, eventCount)
                    : context.getString(R.string.schedule_event_count_other, eventCount);
            if (dayGroup.isFlexible()) {
                eventLabel = context.getString(R.string.schedule_flexible_subtitle, eventLabel);
            }
            tvDaySubtitle.setText(eventLabel);

            // Clear existing event views
            containerEvents.removeAllViews();

            // Add event views
            List<ScheduleItem> events = dayGroup.getEvents();
            for (ScheduleItem event : events) {
                View eventView = createEventView(context, event);
                containerEvents.addView(eventView);
            }
        }

        private View createEventView(Context context, ScheduleItem event) {
            View eventView = LayoutInflater.from(context).inflate(R.layout.item_schedule, containerEvents, false);

            TextView tvTime = eventView.findViewById(R.id.tv_schedule_time);
            TextView tvTitle = eventView.findViewById(R.id.tv_schedule_title);
            TextView tvNotes = eventView.findViewById(R.id.tv_schedule_notes);

            String start = event.getStartTime() != null ? event.getStartTime() : "";
            String end = event.getEndTime() != null ? event.getEndTime() : "";
            String timeRange = start.isEmpty() ? end : (end.isEmpty() ? start : start + " - " + end);

            tvTime.setText(timeRange);
            tvTitle.setText(event.getTitle() != null ? event.getTitle() : "");

            String notes = event.getNotes();
            if (notes != null && !notes.trim().isEmpty()) {
                tvNotes.setText(notes);
            } else {
                tvNotes.setText("");
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 16; // Add some spacing between items
            eventView.setLayoutParams(params);

            eventView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onEdit(event);
            });

            eventView.setOnLongClickListener(v -> {
                if (listener != null)
                    listener.onDelete(event);
                return true;
            });

            return eventView;
        }
    }
}
