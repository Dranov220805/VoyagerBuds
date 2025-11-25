package com.example.voyagerbuds.services;

import android.content.Context;

import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.utils.NotificationHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service class for handling Schedule-related business logic.
 * Encapsulates database operations and business rules for schedule items.
 */
public class ScheduleService {
    private final DatabaseHelper databaseHelper;
    private final Context context;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ScheduleService(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
    }

    /**
     * Add a new schedule item with validation
     * 
     * @param item The schedule item to add
     * @return The ID of the newly created schedule item
     */
    public long createScheduleItem(ScheduleItem item) {
        // Validate schedule item
        String validationError = validateScheduleItem(item);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        // Set timestamps if not set
        if (item.getCreatedAt() == 0) {
            item.setCreatedAt(System.currentTimeMillis());
        }
        item.setUpdatedAt(System.currentTimeMillis());

        long id = databaseHelper.addSchedule(item);

        // Schedule notification if item was added successfully
        if (id > 0 && item.getNotifyBeforeMinutes() > 0) {
            item.setId((int) id);
            NotificationHelper.scheduleNotification(context, item);
        }

        return id;
    }

    /**
     * Get all schedule items for a trip
     * 
     * @param tripId The trip ID
     * @return List of schedule items
     */
    public List<ScheduleItem> getSchedulesForTrip(int tripId) {
        return databaseHelper.getSchedulesForTrip(tripId);
    }

    /**
     * Get a specific schedule item by ID
     * 
     * @param scheduleId The schedule ID
     * @return The schedule item or null if not found
     */
    public ScheduleItem getScheduleById(int scheduleId) {
        return databaseHelper.getScheduleById(scheduleId);
    }

    /**
     * Update an existing schedule item with validation
     * 
     * @param item The schedule item with updated information
     * @return Number of rows affected
     */
    public int updateScheduleItem(ScheduleItem item) {
        // Validate schedule item
        String validationError = validateScheduleItem(item);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        // Update timestamp
        item.setUpdatedAt(System.currentTimeMillis());

        int result = databaseHelper.updateSchedule(item);

        // Update notification
        if (result > 0) {
            if (item.getNotifyBeforeMinutes() > 0) {
                NotificationHelper.scheduleNotification(context, item);
            } else {
                NotificationHelper.cancelNotification(context, item.getId());
            }
        }

        return result;
    }

    /**
     * Delete a schedule item
     * 
     * @param scheduleId The ID of the schedule item to delete
     */
    public void deleteScheduleItem(int scheduleId) {
        // Cancel notification before deleting
        NotificationHelper.cancelNotification(context, scheduleId);
        databaseHelper.deleteSchedule(scheduleId);
    }

    /**
     * Update images for a schedule item
     * 
     * @param scheduleId The schedule ID
     * @param imagesJson JSON string containing image paths
     */
    public void updateScheduleImages(int scheduleId, String imagesJson) {
        databaseHelper.updateScheduleImages(scheduleId, imagesJson);
    }

    /**
     * Get schedule items for a specific day
     * 
     * @param tripId The trip ID
     * @param day    The day in yyyy-MM-dd format
     * @return List of schedule items for that day
     */
    public List<ScheduleItem> getSchedulesForDay(int tripId, String day) {
        List<ScheduleItem> allSchedules = getSchedulesForTrip(tripId);

        // Filter by day
        java.util.List<ScheduleItem> daySchedules = new java.util.ArrayList<>();
        for (ScheduleItem item : allSchedules) {
            if (day.equals(item.getDay())) {
                daySchedules.add(item);
            }
        }

        return daySchedules;
    }

    /**
     * Schedule notification for a schedule item
     * 
     * @param item The schedule item to schedule notification for
     */
    public void scheduleNotification(ScheduleItem item) {
        NotificationHelper.scheduleNotification(context, item);
    }

    /**
     * Cancel notification for a schedule item
     * 
     * @param scheduleId The schedule ID
     */
    public void cancelNotification(int scheduleId) {
        NotificationHelper.cancelNotification(context, scheduleId);
    }

    /**
     * Reschedule all notifications for a trip
     * Useful after updating trip dates or schedule items
     * 
     * @param tripId The trip ID
     */
    public void rescheduleAllNotifications(int tripId) {
        List<ScheduleItem> schedules = getSchedulesForTrip(tripId);
        for (ScheduleItem item : schedules) {
            if (item.getNotifyBeforeMinutes() > 0) {
                NotificationHelper.scheduleNotification(context, item);
            }
        }
    }

    /**
     * Group schedules by day
     * 
     * @param tripId The trip ID
     * @return Map of day to list of schedule items
     */
    public Map<String, List<ScheduleItem>> getSchedulesGroupedByDay(int tripId) {
        List<ScheduleItem> allSchedules = getSchedulesForTrip(tripId);
        Map<String, List<ScheduleItem>> groupedSchedules = new HashMap<>();

        for (ScheduleItem item : allSchedules) {
            String day = item.getDay();
            if (!groupedSchedules.containsKey(day)) {
                groupedSchedules.put(day, new ArrayList<>());
            }
            groupedSchedules.get(day).add(item);
        }

        return groupedSchedules;
    }

    /**
     * Get schedules that have notifications enabled
     * 
     * @param tripId The trip ID
     * @return List of schedule items with notifications
     */
    public List<ScheduleItem> getSchedulesWithNotifications(int tripId) {
        List<ScheduleItem> allSchedules = getSchedulesForTrip(tripId);
        List<ScheduleItem> withNotifications = new ArrayList<>();

        for (ScheduleItem item : allSchedules) {
            if (item.getNotifyBeforeMinutes() > 0) {
                withNotifications.add(item);
            }
        }

        return withNotifications;
    }

    /**
     * Get schedules with a specific location
     * 
     * @param tripId   The trip ID
     * @param location The location to filter by
     * @return List of schedule items at that location
     */
    public List<ScheduleItem> getSchedulesByLocation(int tripId, String location) {
        List<ScheduleItem> allSchedules = getSchedulesForTrip(tripId);
        List<ScheduleItem> filtered = new ArrayList<>();

        for (ScheduleItem item : allSchedules) {
            if (item.getLocation() != null && item.getLocation().equalsIgnoreCase(location)) {
                filtered.add(item);
            }
        }

        return filtered;
    }

    /**
     * Check if a time slot overlaps with existing schedules
     * 
     * @param tripId        The trip ID
     * @param day           The day
     * @param startTime     Start time
     * @param endTime       End time
     * @param excludeItemId Schedule ID to exclude from check (for updates)
     * @return true if there's an overlap
     */
    public boolean hasTimeOverlap(int tripId, String day, String startTime, String endTime, int excludeItemId) {
        List<ScheduleItem> daySchedules = getSchedulesForDay(tripId, day);

        try {
            Date newStart = TIME_FORMAT.parse(startTime);
            Date newEnd = TIME_FORMAT.parse(endTime);

            if (newStart == null || newEnd == null) {
                return false;
            }

            for (ScheduleItem item : daySchedules) {
                if (item.getId() == excludeItemId) {
                    continue; // Skip the item being updated
                }

                Date existingStart = TIME_FORMAT.parse(item.getStartTime());
                Date existingEnd = TIME_FORMAT.parse(item.getEndTime());

                if (existingStart != null && existingEnd != null) {
                    // Check for overlap: (StartA < EndB) and (EndA > StartB)
                    if (newStart.before(existingEnd) && newEnd.after(existingStart)) {
                        return true;
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get total scheduled hours for a day
     * 
     * @param tripId The trip ID
     * @param day    The day
     * @return Total hours scheduled
     */
    public double getTotalScheduledHours(int tripId, String day) {
        List<ScheduleItem> daySchedules = getSchedulesForDay(tripId, day);
        double totalHours = 0;

        try {
            for (ScheduleItem item : daySchedules) {
                Date start = TIME_FORMAT.parse(item.getStartTime());
                Date end = TIME_FORMAT.parse(item.getEndTime());

                if (start != null && end != null) {
                    long diffMillis = end.getTime() - start.getTime();
                    double hours = diffMillis / (1000.0 * 60 * 60);
                    totalHours += hours;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return totalHours;
    }

    /**
     * Check if schedule is happening now
     * 
     * @param item The schedule item
     * @return true if currently active
     */
    public boolean isScheduleActive(ScheduleItem item) {
        try {
            Date today = new Date();
            String todayStr = DATE_FORMAT.format(today);

            // Check if it's the right day
            if (!item.getDay().equals(todayStr)) {
                return false;
            }

            // Check if current time is between start and end
            Date start = TIME_FORMAT.parse(item.getStartTime());
            Date end = TIME_FORMAT.parse(item.getEndTime());
            Date now = TIME_FORMAT.parse(TIME_FORMAT.format(today));

            if (start != null && end != null && now != null) {
                return !now.before(start) && !now.after(end);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Validate schedule item data
     * 
     * @param item The schedule item to validate
     * @return Error message if invalid, null if valid
     */
    private String validateScheduleItem(ScheduleItem item) {
        if (item.getTripId() <= 0) {
            return "Invalid trip ID";
        }

        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
            return "Title is required";
        }

        if (item.getDay() == null || item.getDay().trim().isEmpty()) {
            return "Day is required";
        }

        if (item.getStartTime() == null || item.getStartTime().trim().isEmpty()) {
            return "Start time is required";
        }

        if (item.getEndTime() == null || item.getEndTime().trim().isEmpty()) {
            return "End time is required";
        }

        // Validate time format and range
        try {
            Date start = TIME_FORMAT.parse(item.getStartTime());
            Date end = TIME_FORMAT.parse(item.getEndTime());

            if (start != null && end != null && !end.after(start)) {
                return "End time must be after start time";
            }
        } catch (ParseException e) {
            return "Invalid time format. Use HH:mm";
        }

        // Validate date format
        try {
            DATE_FORMAT.parse(item.getDay());
        } catch (ParseException e) {
            return "Invalid date format. Use yyyy-MM-dd";
        }

        return null; // Valid
    }
}
