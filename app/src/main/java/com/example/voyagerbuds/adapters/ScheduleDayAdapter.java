package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
        private final FrameLayout containerEvents;
        private final View viewCurrentTimeLine;
        private final View viewCurrentTimeDot;
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
            viewCurrentTimeLine = itemView.findViewById(R.id.view_current_time_line);
            viewCurrentTimeDot = itemView.findViewById(R.id.view_current_time_dot);
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

            // Add event views positioned based on their time
            List<ScheduleItem> events = dayGroup.getEvents();
            for (ScheduleItem event : events) {
                View eventView = createEventView(context, event);
                containerEvents.addView(eventView);
            }

            // Update current time indicator
            updateCurrentTimeIndicator(dayGroup, selectedDateKey);
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

            // Position event based on start time
            if (!start.isEmpty()) {
                int topMargin = calculateTimePosition(context, start);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                params.topMargin = topMargin;
                eventView.setLayoutParams(params);
            }

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

        private int calculateTimePosition(Context context, String timeStr) {
            try {
                String[] parts = timeStr.split(":");
                if (parts.length >= 2) {
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    
                    // Calculate position: 30dp per hour
                    float hours = hour + (minute / 60.0f);
                    int positionDp = (int) (hours * 30.0f);
                    
                    // Convert dp to pixels
                    float density = context.getResources().getDisplayMetrics().density;
                    return (int) (positionDp * density);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        private void updateCurrentTimeIndicator(ScheduleDayGroup dayGroup, String selectedDateKey) {
            // Check if this is today's date
            String todayKey = DATE_FORMAT.format(new Date());
            boolean isToday = selectedDateKey != null && selectedDateKey.equals(todayKey)
                    && !dayGroup.isFlexible() && dayGroup.getDayKey() != null
                    && dayGroup.getDayKey().equals(todayKey);

            if (isToday) {
                // Calculate current time position
                Calendar now = Calendar.getInstance();
                int currentHour = now.get(Calendar.HOUR_OF_DAY);
                int currentMinute = now.get(Calendar.MINUTE);

                // Calculate position as percentage of day (0-24 hours)
                double timePosition = currentHour + (currentMinute / 60.0);
                double percentage = (timePosition / 24.0) * 100.0;

                // Show indicators
                viewCurrentTimeLine.setVisibility(View.VISIBLE);
                viewCurrentTimeDot.setVisibility(View.VISIBLE);

                // Position the indicators after layout
                itemViewRoot.post(new Runnable() {
                    @Override
                    public void run() {
                        int containerHeight = viewTimelineContainer.getHeight();
                        if (containerHeight > 0) {
                            int position = (int) (containerHeight * percentage / 100.0);

                            // Position the dot on the timeline
                            FrameLayout.LayoutParams dotParams = (FrameLayout.LayoutParams) viewCurrentTimeDot
                                    .getLayoutParams();
                            if (dotParams == null) {
                                dotParams = new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT);
                            }
                            dotParams.gravity = Gravity.START | Gravity.TOP;
                            dotParams.topMargin = position - 8; // -8 to center the 16dp dot
                            dotParams.leftMargin = 8; // Align with timeline (15dp margin - 7dp to center)
                            viewCurrentTimeDot.setLayoutParams(dotParams);

                            // Position the horizontal line extending from timeline
                            FrameLayout.LayoutParams lineParams = (FrameLayout.LayoutParams) viewCurrentTimeLine
                                    .getLayoutParams();
                            if (lineParams == null) {
                                lineParams = new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT);
                            }
                            lineParams.gravity = Gravity.TOP;
                            lineParams.topMargin = position - 1; // -1 to center the 2dp line
                            lineParams.leftMargin = 32; // Start after timeline (15dp + 2dp line + 15dp)
                            viewCurrentTimeLine.setLayoutParams(lineParams);
                        }
                    }
                });
            } else {
                // Hide indicators if not today
                viewCurrentTimeLine.setVisibility(View.GONE);
                viewCurrentTimeDot.setVisibility(View.GONE);
            }
        }
    }
}
