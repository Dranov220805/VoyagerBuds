package com.example.voyagerbuds.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.voyagerbuds.models.ScheduleItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Schedule entity.
 * Contains only pure database CRUD operations without business logic.
 */
public class ScheduleDao {
    // Table and column names
    private static final String TABLE_SCHEDULES = "Schedules";
    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_SCHEDULE_TRIP_ID = "tripId";
    private static final String COLUMN_SCHEDULE_DAY = "day";
    private static final String COLUMN_SCHEDULE_START_TIME = "start_time";
    private static final String COLUMN_SCHEDULE_END_TIME = "end_time";
    private static final String COLUMN_SCHEDULE_TITLE = "title";
    private static final String COLUMN_SCHEDULE_NOTES = "notes";
    private static final String COLUMN_SCHEDULE_LOCATION = "location";
    private static final String COLUMN_SCHEDULE_PARTICIPANTS = "participants";
    private static final String COLUMN_SCHEDULE_IMAGES = "image_paths";
    private static final String COLUMN_SCHEDULE_NOTIFY_BEFORE = "notify_before_minutes";
    private static final String COLUMN_SCHEDULE_CREATED_AT = "created_at";
    private static final String COLUMN_SCHEDULE_UPDATED_AT = "updated_at";

    private final SQLiteDatabase database;

    public ScheduleDao(SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * Insert a new schedule item
     */
    public long insert(ScheduleItem item) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCHEDULE_TRIP_ID, item.getTripId());
        values.put(COLUMN_SCHEDULE_DAY, item.getDay());
        values.put(COLUMN_SCHEDULE_START_TIME, item.getStartTime());
        values.put(COLUMN_SCHEDULE_END_TIME, item.getEndTime());
        values.put(COLUMN_SCHEDULE_TITLE, item.getTitle());
        values.put(COLUMN_SCHEDULE_NOTES, item.getNotes());
        values.put(COLUMN_SCHEDULE_LOCATION, item.getLocation());
        values.put(COLUMN_SCHEDULE_PARTICIPANTS, item.getParticipants());
        values.put(COLUMN_SCHEDULE_IMAGES, item.getImagePaths());
        values.put(COLUMN_SCHEDULE_NOTIFY_BEFORE, item.getNotifyBeforeMinutes());
        values.put(COLUMN_SCHEDULE_CREATED_AT, item.getCreatedAt());
        values.put(COLUMN_SCHEDULE_UPDATED_AT, item.getUpdatedAt());

        return database.insert(TABLE_SCHEDULES, null, values);
    }

    /**
     * Update an existing schedule item
     */
    public int update(ScheduleItem item) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCHEDULE_DAY, item.getDay());
        values.put(COLUMN_SCHEDULE_START_TIME, item.getStartTime());
        values.put(COLUMN_SCHEDULE_END_TIME, item.getEndTime());
        values.put(COLUMN_SCHEDULE_TITLE, item.getTitle());
        values.put(COLUMN_SCHEDULE_NOTES, item.getNotes());
        values.put(COLUMN_SCHEDULE_LOCATION, item.getLocation());
        values.put(COLUMN_SCHEDULE_PARTICIPANTS, item.getParticipants());
        values.put(COLUMN_SCHEDULE_IMAGES, item.getImagePaths());
        values.put(COLUMN_SCHEDULE_NOTIFY_BEFORE, item.getNotifyBeforeMinutes());
        values.put(COLUMN_SCHEDULE_UPDATED_AT, item.getUpdatedAt());

        return database.update(TABLE_SCHEDULES, values, COLUMN_SCHEDULE_ID + " = ?",
                new String[] { String.valueOf(item.getId()) });
    }

    /**
     * Delete a schedule item by ID
     */
    public int delete(int scheduleId) {
        return database.delete(TABLE_SCHEDULES, COLUMN_SCHEDULE_ID + " = ?",
                new String[] { String.valueOf(scheduleId) });
    }

    /**
     * Delete all schedules for a trip
     */
    public int deleteByTripId(int tripId) {
        return database.delete(TABLE_SCHEDULES, COLUMN_SCHEDULE_TRIP_ID + " = ?",
                new String[] { String.valueOf(tripId) });
    }

    /**
     * Get schedule item by ID
     */
    public ScheduleItem getById(int scheduleId) {
        Cursor cursor = database.query(TABLE_SCHEDULES, null, COLUMN_SCHEDULE_ID + "=?",
                new String[] { String.valueOf(scheduleId) }, null, null, null);

        ScheduleItem item = null;
        if (cursor != null && cursor.moveToFirst()) {
            item = cursorToScheduleItem(cursor);
            cursor.close();
        }
        return item;
    }

    /**
     * Get all schedule items for a trip, ordered by day and start time
     */
    public List<ScheduleItem> getAllByTripId(int tripId) {
        List<ScheduleItem> list = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_SCHEDULES + " WHERE " + COLUMN_SCHEDULE_TRIP_ID + " = ? ORDER BY "
                + COLUMN_SCHEDULE_DAY + ", " + COLUMN_SCHEDULE_START_TIME;

        Cursor cursor = database.rawQuery(query, new String[] { String.valueOf(tripId) });

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToScheduleItem(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Get schedules for a specific day
     */
    public List<ScheduleItem> getByTripIdAndDay(int tripId, String day) {
        List<ScheduleItem> list = new ArrayList<>();
        String selection = COLUMN_SCHEDULE_TRIP_ID + " = ? AND " + COLUMN_SCHEDULE_DAY + " = ?";
        String[] selectionArgs = new String[] { String.valueOf(tripId), day };

        Cursor cursor = database.query(TABLE_SCHEDULES, null, selection, selectionArgs,
                null, null, COLUMN_SCHEDULE_START_TIME);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToScheduleItem(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Update only the images field
     */
    public int updateImages(int scheduleId, String imagesJson) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCHEDULE_IMAGES, imagesJson);
        return database.update(TABLE_SCHEDULES, values, COLUMN_SCHEDULE_ID + " = ?",
                new String[] { String.valueOf(scheduleId) });
    }

    /**
     * Convert cursor to ScheduleItem object
     */
    private ScheduleItem cursorToScheduleItem(Cursor cursor) {
        ScheduleItem item = new ScheduleItem();
        item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_ID)));
        item.setTripId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_TRIP_ID)));
        item.setDay(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_DAY)));
        item.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_START_TIME)));
        item.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_END_TIME)));
        item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_TITLE)));
        item.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_NOTES)));

        // Handle optional columns
        int locationIndex = cursor.getColumnIndex(COLUMN_SCHEDULE_LOCATION);
        if (locationIndex != -1) {
            item.setLocation(cursor.getString(locationIndex));
        }

        int participantsIndex = cursor.getColumnIndex(COLUMN_SCHEDULE_PARTICIPANTS);
        if (participantsIndex != -1) {
            item.setParticipants(cursor.getString(participantsIndex));
        }

        int imagesIndex = cursor.getColumnIndex(COLUMN_SCHEDULE_IMAGES);
        if (imagesIndex != -1) {
            item.setImagePaths(cursor.getString(imagesIndex));
        }

        int notifyBeforeIndex = cursor.getColumnIndex(COLUMN_SCHEDULE_NOTIFY_BEFORE);
        if (notifyBeforeIndex != -1) {
            item.setNotifyBeforeMinutes(cursor.getInt(notifyBeforeIndex));
        }

        item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_CREATED_AT)));
        item.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_UPDATED_AT)));

        return item;
    }
}
