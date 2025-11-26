package com.example.voyagerbuds.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.voyagerbuds.models.Trip;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Trip entity.
 * Contains only pure database CRUD operations without business logic.
 */
public class TripDao {
    // Table and column names
    private static final String TABLE_TRIPS = "Trips";
    private static final String COLUMN_TRIP_ID = "tripId";
    private static final String COLUMN_USER_ID = "userId";
    private static final String COLUMN_TRIP_NAME = "trip_name";
    private static final String COLUMN_START_DATE = "start_date";
    private static final String COLUMN_END_DATE = "end_date";
    private static final String COLUMN_DESTINATION = "destination";
    private static final String COLUMN_NOTES = "notes";
    private static final String COLUMN_PHOTO_URL = "photo_url";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_IS_GROUP_TRIP = "is_group_trip";
    private static final String COLUMN_MAP_LATITUDE = "map_latitude";
    private static final String COLUMN_MAP_LONGITUDE = "map_longitude";
    private static final String COLUMN_SYNC_STATUS = "sync_status";
    private static final String COLUMN_FIREBASE_ID = "firebase_id";
    private static final String COLUMN_LAST_SYNCED_AT = "last_synced_at";
    private static final String COLUMN_BUDGET = "budget";
    private static final String COLUMN_PARTICIPANTS = "participants";

    private final SQLiteDatabase database;

    public TripDao(SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * Insert a new trip into database
     */
    public long insert(Trip trip) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, trip.getUserId());
        values.put(COLUMN_TRIP_NAME, trip.getTripName());
        values.put(COLUMN_START_DATE, trip.getStartDate());
        values.put(COLUMN_END_DATE, trip.getEndDate());
        values.put(COLUMN_DESTINATION, trip.getDestination());
        values.put(COLUMN_NOTES, trip.getNotes());
        values.put(COLUMN_PHOTO_URL, trip.getPhotoUrl());
        values.put(COLUMN_CREATED_AT, trip.getCreatedAt());
        values.put(COLUMN_UPDATED_AT, trip.getUpdatedAt());
        values.put(COLUMN_IS_GROUP_TRIP, trip.getIsGroupTrip());
        values.put(COLUMN_MAP_LATITUDE, trip.getMapLatitude());
        values.put(COLUMN_MAP_LONGITUDE, trip.getMapLongitude());
        values.put(COLUMN_SYNC_STATUS, trip.getSyncStatus());
        values.put(COLUMN_FIREBASE_ID, trip.getFirebaseId());
        values.put(COLUMN_LAST_SYNCED_AT, trip.getLastSyncedAt());
        values.put(COLUMN_BUDGET, trip.getBudget());
        values.put(COLUMN_PARTICIPANTS, trip.getParticipants());

        return database.insert(TABLE_TRIPS, null, values);
    }

    /**
     * Update an existing trip
     */
    public int update(Trip trip) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRIP_NAME, trip.getTripName());
        values.put(COLUMN_START_DATE, trip.getStartDate());
        values.put(COLUMN_END_DATE, trip.getEndDate());
        values.put(COLUMN_DESTINATION, trip.getDestination());
        values.put(COLUMN_NOTES, trip.getNotes());
        values.put(COLUMN_PHOTO_URL, trip.getPhotoUrl());
        values.put(COLUMN_UPDATED_AT, trip.getUpdatedAt());
        values.put(COLUMN_MAP_LATITUDE, trip.getMapLatitude());
        values.put(COLUMN_MAP_LONGITUDE, trip.getMapLongitude());
        values.put(COLUMN_BUDGET, trip.getBudget());
        values.put(COLUMN_PARTICIPANTS, trip.getParticipants());
        values.put(COLUMN_IS_GROUP_TRIP, trip.getIsGroupTrip());

        return database.update(TABLE_TRIPS, values, COLUMN_TRIP_ID + " = ?",
                new String[] { String.valueOf(trip.getTripId()) });
    }

    /**
     * Delete a trip by ID
     */
    public int delete(int tripId) {
        return database.delete(TABLE_TRIPS, COLUMN_TRIP_ID + " = ?",
                new String[] { String.valueOf(tripId) });
    }

    /**
     * Get trip by ID
     */
    public Trip getById(int tripId) {
        Cursor cursor = database.query(TABLE_TRIPS, null, COLUMN_TRIP_ID + "=?",
                new String[] { String.valueOf(tripId) }, null, null, null);

        Trip trip = null;
        if (cursor != null && cursor.moveToFirst()) {
            trip = cursorToTrip(cursor);
            cursor.close();
        }
        return trip;
    }

    /**
     * Get all trips for a specific user, ordered by creation date
     */
    public List<Trip> getAllByUserId(int userId) {
        List<Trip> tripList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TRIPS + " WHERE " + COLUMN_USER_ID + " = ? ORDER BY "
                + COLUMN_CREATED_AT + " DESC";

        Cursor cursor = database.rawQuery(selectQuery, new String[] { String.valueOf(userId) });

        if (cursor.moveToFirst()) {
            do {
                tripList.add(cursorToTrip(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tripList;
    }

    /**
     * Get trips by date range for a specific user (for checking overlaps)
     */
    public List<Trip> getTripsByDateRange(int userId, String startDate, String endDate) {
        List<Trip> tripList = new ArrayList<>();
        String selection = COLUMN_USER_ID + " = ? AND " + COLUMN_START_DATE + " <= ? AND " + COLUMN_END_DATE + " >= ?";
        String[] selectionArgs = new String[] { String.valueOf(userId), endDate, startDate };

        Cursor cursor = database.query(TABLE_TRIPS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                tripList.add(cursorToTrip(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tripList;
    }

    /**
     * Get trips by date range for a specific user, excluding a specific trip
     */
    public List<Trip> getTripsByDateRangeExcluding(int userId, String startDate, String endDate, int excludeTripId) {
        List<Trip> tripList = new ArrayList<>();
        String selection = COLUMN_USER_ID + " = ? AND " + COLUMN_START_DATE + " <= ? AND " + COLUMN_END_DATE
                + " >= ? AND " + COLUMN_TRIP_ID + " != ?";
        String[] selectionArgs = new String[] { String.valueOf(userId), endDate, startDate,
                String.valueOf(excludeTripId) };

        Cursor cursor = database.query(TABLE_TRIPS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                tripList.add(cursorToTrip(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tripList;
    }

    /**
     * Convert cursor to Trip object
     */
    private Trip cursorToTrip(Cursor cursor) {
        Trip trip = new Trip();
        trip.setTripId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TRIP_ID)));
        trip.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
        trip.setTripName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_NAME)));
        trip.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_DATE)));
        trip.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_DATE)));
        trip.setDestination(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESTINATION)));
        trip.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)));
        trip.setPhotoUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_URL)));
        trip.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        trip.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
        trip.setIsGroupTrip(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_GROUP_TRIP)));
        trip.setMapLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_MAP_LATITUDE)));
        trip.setMapLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_MAP_LONGITUDE)));
        trip.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)));
        trip.setFirebaseId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FIREBASE_ID)));
        trip.setLastSyncedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_SYNCED_AT)));
        trip.setBudget(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BUDGET)));
        trip.setParticipants(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARTICIPANTS)));
        return trip;
    }
}
