package com.example.voyagerbuds.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Represents a day in the trip timeline along with the schedule events for that day.
 */
public class ScheduleDayGroup {

    private static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("EEEE", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd", Locale.getDefault());

    private final String dayKey;
    private final boolean flexible;
    private final List<ScheduleItem> events = new ArrayList<>();

    public ScheduleDayGroup(String dayKey, boolean flexible) {
        this.dayKey = dayKey;
        this.flexible = flexible;
    }

    public void addEvent(ScheduleItem item) {
        events.add(item);
    }

    public List<ScheduleItem> getEvents() {
        return events;
    }

    public String getDayKey() {
        return dayKey;
    }

    public boolean isFlexible() {
        return flexible;
    }

    public String getDisplayTitle() {
        if (flexible || dayKey == null || dayKey.trim().isEmpty()) {
            return "Flexible day";
        }
        try {
            Date date = INPUT_FORMAT.parse(dayKey);
            if (date != null) {
                return DAY_FORMAT.format(date) + ", " + DATE_FORMAT.format(date);
            }
        } catch (ParseException ignored) {
        }
        return dayKey;
    }

    public String getDisplaySubtitle(int eventCount) {
        if (flexible) {
            return "Events without a fixed day";
        }
        return null;
    }
}

