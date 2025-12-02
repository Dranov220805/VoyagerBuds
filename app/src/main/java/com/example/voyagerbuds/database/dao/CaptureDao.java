package com.example.voyagerbuds.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.voyagerbuds.models.Capture;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Capture entity.
 * Contains only pure database CRUD operations without business logic.
 * Manages photo/video diary entries for trips.
 */
public class CaptureDao {
    // Table and column names (public for DatabaseHelper access)
    public static final String TABLE_NAME = "Captures";
    public static final String COLUMN_CAPTURE_ID = "captureId";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_TRIP_ID = "tripId";
    public static final String COLUMN_MEDIA_PATH = "media_path";
    public static final String COLUMN_MEDIA_TYPE = "media_type";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CAPTURED_AT = "captured_at";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";

    // Keep private alias for backward compatibility
    private static final String TABLE_CAPTURES = TABLE_NAME;

    private final SQLiteDatabase database;

    public CaptureDao(SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * Insert a new capture (photo/video with description)
     */
    public long insert(Capture capture) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, capture.getUserId());
        values.put(COLUMN_TRIP_ID, capture.getTripId());
        values.put(COLUMN_MEDIA_PATH, capture.getMediaPath());
        values.put(COLUMN_MEDIA_TYPE, capture.getMediaType());
        values.put(COLUMN_DESCRIPTION, capture.getDescription());
        values.put(COLUMN_CAPTURED_AT, capture.getCapturedAt());
        values.put(COLUMN_CREATED_AT, capture.getCreatedAt());
        values.put(COLUMN_UPDATED_AT, capture.getUpdatedAt());

        return database.insert(TABLE_CAPTURES, null, values);
    }

    /**
     * Update an existing capture
     */
    public int update(Capture capture) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MEDIA_PATH, capture.getMediaPath());
        values.put(COLUMN_MEDIA_TYPE, capture.getMediaType());
        values.put(COLUMN_DESCRIPTION, capture.getDescription());
        values.put(COLUMN_CAPTURED_AT, capture.getCapturedAt());
        values.put(COLUMN_UPDATED_AT, capture.getUpdatedAt());

        return database.update(TABLE_CAPTURES, values, COLUMN_CAPTURE_ID + " = ?",
                new String[] { String.valueOf(capture.getCaptureId()) });
    }

    /**
     * Delete a capture by ID
     */
    public int delete(int captureId) {
        return database.delete(TABLE_CAPTURES, COLUMN_CAPTURE_ID + " = ?",
                new String[] { String.valueOf(captureId) });
    }

    /**
     * Delete all captures for a trip
     */
    public int deleteByTripId(int tripId) {
        return database.delete(TABLE_CAPTURES, COLUMN_TRIP_ID + " = ?",
                new String[] { String.valueOf(tripId) });
    }

    /**
     * Delete all captures for a user
     */
    public int deleteByUserId(int userId) {
        return database.delete(TABLE_CAPTURES, COLUMN_USER_ID + " = ?",
                new String[] { String.valueOf(userId) });
    }

    /**
     * Get capture by ID
     */
    public Capture getById(int captureId) {
        Cursor cursor = database.query(TABLE_CAPTURES, null, COLUMN_CAPTURE_ID + "=?",
                new String[] { String.valueOf(captureId) }, null, null, null);

        Capture capture = null;
        if (cursor != null && cursor.moveToFirst()) {
            capture = cursorToCapture(cursor);
            cursor.close();
        }
        return capture;
    }

    /**
     * Get all captures for a trip, ordered by captured date descending (most recent
     * first)
     */
    public List<Capture> getAllByTripId(int tripId) {
        List<Capture> captures = new ArrayList<>();
        Cursor cursor = database.query(TABLE_CAPTURES, null, COLUMN_TRIP_ID + "=?",
                new String[] { String.valueOf(tripId) }, null, null, COLUMN_CAPTURED_AT + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                captures.add(cursorToCapture(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return captures;
    }

    /**
     * Get all captures for a user, ordered by captured date descending
     */
    public List<Capture> getAllByUserId(int userId) {
        List<Capture> captures = new ArrayList<>();
        Cursor cursor = database.query(TABLE_CAPTURES, null, COLUMN_USER_ID + "=?",
                new String[] { String.valueOf(userId) }, null, null, COLUMN_CAPTURED_AT + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                captures.add(cursorToCapture(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return captures;
    }

    /**
     * Get captures by media type (photo or video) for a trip
     */
    public List<Capture> getByTripIdAndMediaType(int tripId, String mediaType) {
        List<Capture> captures = new ArrayList<>();
        String selection = COLUMN_TRIP_ID + " = ? AND " + COLUMN_MEDIA_TYPE + " = ?";
        String[] selectionArgs = new String[] { String.valueOf(tripId), mediaType };

        Cursor cursor = database.query(TABLE_CAPTURES, null, selection, selectionArgs,
                null, null, COLUMN_CAPTURED_AT + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                captures.add(cursorToCapture(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return captures;
    }

    /**
     * Get count of captures for a trip
     */
    public int getCountByTripId(int tripId) {
        Cursor cursor = database.query(TABLE_CAPTURES, new String[] { "COUNT(*) as count" },
                COLUMN_TRIP_ID + "=?", new String[] { String.valueOf(tripId) },
                null, null, null);

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
            cursor.close();
        }
        return count;
    }

    /**
     * Get count of photos (media_type = 'photo') for a trip
     */
    public int getPhotoCountByTripId(int tripId) {
        Cursor cursor = database.query(TABLE_CAPTURES, new String[] { "COUNT(*) as count" },
                COLUMN_TRIP_ID + "=? AND " + COLUMN_MEDIA_TYPE + "=?",
                new String[] { String.valueOf(tripId), "photo" },
                null, null, null);

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
            cursor.close();
        }
        return count;
    }

    /**
     * Get count of videos (media_type = 'video') for a trip
     */
    public int getVideoCountByTripId(int tripId) {
        Cursor cursor = database.query(TABLE_CAPTURES, new String[] { "COUNT(*) as count" },
                COLUMN_TRIP_ID + "=? AND " + COLUMN_MEDIA_TYPE + "=?",
                new String[] { String.valueOf(tripId), "video" },
                null, null, null);

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
            cursor.close();
        }
        return count;
    }

    /**
     * Convert cursor row to Capture object (public for DatabaseHelper access)
     */
    public Capture cursorToCapture(Cursor cursor) {
        Capture capture = new Capture();
        capture.setCaptureId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAPTURE_ID)));
        capture.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
        capture.setTripId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TRIP_ID)));
        capture.setMediaPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_PATH)));
        capture.setMediaType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_TYPE)));
        capture.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
        capture.setCapturedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CAPTURED_AT)));
        capture.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        capture.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
        return capture;
    }
}
