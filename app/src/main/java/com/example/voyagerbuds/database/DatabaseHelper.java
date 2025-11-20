package com.example.voyagerbuds.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.models.Expense;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "voyagerbuds.db";
    private static final int DATABASE_VERSION = 3;

    // Trips table
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

    // Expenses table
    private static final String TABLE_EXPENSES = "Expenses";
    private static final String COLUMN_EXPENSE_ID = "expenseId";
    private static final String COLUMN_EXPENSE_TRIP_ID = "tripId";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_CURRENCY = "currency";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_SPENT_AT = "spent_at";
    // Schedules table
    private static final String TABLE_SCHEDULES = "Schedules";
    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_SCHEDULE_TRIP_ID = "tripId";
    private static final String COLUMN_SCHEDULE_DAY = "day";
    private static final String COLUMN_SCHEDULE_START_TIME = "start_time";
    private static final String COLUMN_SCHEDULE_END_TIME = "end_time";
    private static final String COLUMN_SCHEDULE_TITLE = "title";
    private static final String COLUMN_SCHEDULE_NOTES = "notes";
    private static final String COLUMN_SCHEDULE_CREATED_AT = "created_at";
    private static final String COLUMN_SCHEDULE_UPDATED_AT = "updated_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TRIPS_TABLE = "CREATE TABLE " + TABLE_TRIPS + "("
                + COLUMN_TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " INTEGER,"
                + COLUMN_TRIP_NAME + " TEXT NOT NULL,"
                + COLUMN_START_DATE + " TEXT,"
                + COLUMN_END_DATE + " TEXT,"
                + COLUMN_DESTINATION + " TEXT,"
                + COLUMN_NOTES + " TEXT,"
                + COLUMN_PHOTO_URL + " TEXT,"
                + COLUMN_CREATED_AT + " INTEGER,"
                + COLUMN_UPDATED_AT + " INTEGER,"
                + COLUMN_IS_GROUP_TRIP + " INTEGER DEFAULT 0,"
                + COLUMN_MAP_LATITUDE + " REAL,"
                + COLUMN_MAP_LONGITUDE + " REAL,"
                + COLUMN_SYNC_STATUS + " TEXT,"
                + COLUMN_FIREBASE_ID + " INTEGER,"
                + COLUMN_LAST_SYNCED_AT + " INTEGER,"
                + COLUMN_BUDGET + " REAL,"
                + COLUMN_PARTICIPANTS + " TEXT"
                + ")";
        db.execSQL(CREATE_TRIPS_TABLE);

        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + "("
                + COLUMN_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EXPENSE_TRIP_ID + " INTEGER,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_AMOUNT + " REAL,"
                + COLUMN_CURRENCY + " TEXT,"
                + COLUMN_NOTE + " TEXT,"
                + COLUMN_SPENT_AT + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_EXPENSE_TRIP_ID + ") REFERENCES "
                + TABLE_TRIPS + "(" + COLUMN_TRIP_ID + ")"
                + ")";
        db.execSQL(CREATE_EXPENSES_TABLE);

        String CREATE_SCHEDULES_TABLE = "CREATE TABLE " + TABLE_SCHEDULES + "("
                + COLUMN_SCHEDULE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SCHEDULE_TRIP_ID + " INTEGER,"
                + COLUMN_SCHEDULE_DAY + " TEXT,"
                + COLUMN_SCHEDULE_START_TIME + " TEXT,"
                + COLUMN_SCHEDULE_END_TIME + " TEXT,"
                + COLUMN_SCHEDULE_TITLE + " TEXT,"
                + COLUMN_SCHEDULE_NOTES + " TEXT,"
                + COLUMN_SCHEDULE_CREATED_AT + " INTEGER,"
                + COLUMN_SCHEDULE_UPDATED_AT + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_SCHEDULE_TRIP_ID + ") REFERENCES "
                + TABLE_TRIPS + "(" + COLUMN_TRIP_ID + ")"
                + ")";
        db.execSQL(CREATE_SCHEDULES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Create Schedules table if it doesn't exist (for databases created before
            // version 2)
            String CREATE_SCHEDULES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SCHEDULES + "("
                    + COLUMN_SCHEDULE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_SCHEDULE_TRIP_ID + " INTEGER,"
                    + COLUMN_SCHEDULE_DAY + " TEXT,"
                    + COLUMN_SCHEDULE_START_TIME + " TEXT,"
                    + COLUMN_SCHEDULE_END_TIME + " TEXT,"
                    + COLUMN_SCHEDULE_TITLE + " TEXT,"
                    + COLUMN_SCHEDULE_NOTES + " TEXT,"
                    + COLUMN_SCHEDULE_CREATED_AT + " INTEGER,"
                    + COLUMN_SCHEDULE_UPDATED_AT + " INTEGER,"
                    + "FOREIGN KEY(" + COLUMN_SCHEDULE_TRIP_ID + ") REFERENCES "
                    + TABLE_TRIPS + "(" + COLUMN_TRIP_ID + ")"
                    + ")";
            db.execSQL(CREATE_SCHEDULES_TABLE);
        }
        if (oldVersion < 3) {
            // Add budget and participants columns
            db.execSQL("ALTER TABLE " + TABLE_TRIPS + " ADD COLUMN " + COLUMN_BUDGET + " REAL");
            db.execSQL("ALTER TABLE " + TABLE_TRIPS + " ADD COLUMN " + COLUMN_PARTICIPANTS + " TEXT");
        }
    }

    // Trip CRUD operations
    public long addTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
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

        long id = db.insert(TABLE_TRIPS, null, values);
        db.close();
        return id;
    }

    // Schedule CRUD
    public long addSchedule(com.example.voyagerbuds.models.ScheduleItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(COLUMN_SCHEDULE_TRIP_ID, item.getTripId());
        values.put(COLUMN_SCHEDULE_DAY, item.getDay());
        values.put(COLUMN_SCHEDULE_START_TIME, item.getStartTime());
        values.put(COLUMN_SCHEDULE_END_TIME, item.getEndTime());
        values.put(COLUMN_SCHEDULE_TITLE, item.getTitle());
        values.put(COLUMN_SCHEDULE_NOTES, item.getNotes());
        values.put(COLUMN_SCHEDULE_CREATED_AT, item.getCreatedAt());
        values.put(COLUMN_SCHEDULE_UPDATED_AT, item.getUpdatedAt());

        long id = db.insert(TABLE_SCHEDULES, null, values);
        db.close();
        return id;
    }

    public java.util.List<com.example.voyagerbuds.models.ScheduleItem> getSchedulesForTrip(int tripId) {
        java.util.List<com.example.voyagerbuds.models.ScheduleItem> list = new java.util.ArrayList<>();
        String query = "SELECT * FROM " + TABLE_SCHEDULES + " WHERE " + COLUMN_SCHEDULE_TRIP_ID + " = ? ORDER BY "
                + COLUMN_SCHEDULE_DAY + ", " + COLUMN_SCHEDULE_START_TIME;
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(tripId) });

        if (cursor.moveToFirst()) {
            do {
                com.example.voyagerbuds.models.ScheduleItem it = new com.example.voyagerbuds.models.ScheduleItem();
                it.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_ID)));
                it.setTripId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_TRIP_ID)));
                it.setDay(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_DAY)));
                it.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_START_TIME)));
                it.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_END_TIME)));
                it.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_TITLE)));
                it.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_NOTES)));
                it.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_CREATED_AT)));
                it.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_UPDATED_AT)));
                list.add(it);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public int updateSchedule(com.example.voyagerbuds.models.ScheduleItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(COLUMN_SCHEDULE_DAY, item.getDay());
        values.put(COLUMN_SCHEDULE_START_TIME, item.getStartTime());
        values.put(COLUMN_SCHEDULE_END_TIME, item.getEndTime());
        values.put(COLUMN_SCHEDULE_TITLE, item.getTitle());
        values.put(COLUMN_SCHEDULE_NOTES, item.getNotes());
        values.put(COLUMN_SCHEDULE_UPDATED_AT, item.getUpdatedAt());

        int result = db.update(TABLE_SCHEDULES, values, COLUMN_SCHEDULE_ID + " = ?",
                new String[] { String.valueOf(item.getId()) });
        db.close();
        return result;
    }

    public void deleteSchedule(int scheduleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCHEDULES, COLUMN_SCHEDULE_ID + " = ?", new String[] { String.valueOf(scheduleId) });
        db.close();
    }

    public List<Trip> getAllTrips(int userId) {
        List<Trip> tripList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TRIPS + " WHERE " + COLUMN_USER_ID + " = ? ORDER BY "
                + COLUMN_CREATED_AT + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[] { String.valueOf(userId) });

        if (cursor.moveToFirst()) {
            do {
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
                tripList.add(trip);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tripList;
    }

    public Trip getTripById(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRIPS, null, COLUMN_TRIP_ID + "=?",
                new String[] { String.valueOf(tripId) }, null, null, null);

        Trip trip = null;
        if (cursor != null && cursor.moveToFirst()) {
            trip = new Trip();
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
            cursor.close();
        }
        db.close();
        return trip;
    }

    public double getTotalExpensesForTrip(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + COLUMN_AMOUNT + ") as total FROM " + TABLE_EXPENSES + " WHERE "
                + COLUMN_EXPENSE_TRIP_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(tripId) });

        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
        }
        cursor.close();
        db.close();
        return total;
    }

    public int updateTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRIP_NAME, trip.getTripName());
        values.put(COLUMN_START_DATE, trip.getStartDate());
        values.put(COLUMN_END_DATE, trip.getEndDate());
        values.put(COLUMN_DESTINATION, trip.getDestination());
        values.put(COLUMN_NOTES, trip.getNotes());
        values.put(COLUMN_PHOTO_URL, trip.getPhotoUrl());
        values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
        values.put(COLUMN_MAP_LATITUDE, trip.getMapLatitude());
        values.put(COLUMN_MAP_LONGITUDE, trip.getMapLongitude());

        int result = db.update(TABLE_TRIPS, values, COLUMN_TRIP_ID + " = ?",
                new String[] { String.valueOf(trip.getTripId()) });
        db.close();
        return result;
    }

    public void deleteTrip(int tripId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, COLUMN_EXPENSE_TRIP_ID + " = ?", new String[] { String.valueOf(tripId) });
        db.delete(TABLE_TRIPS, COLUMN_TRIP_ID + " = ?", new String[] { String.valueOf(tripId) });
        db.close();
    }

    public boolean isDateRangeAvailable(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Check for overlap: (StartA <= EndB) and (EndA >= StartB)
        // New trip: StartA, EndA
        // Existing trip: StartB, EndB
        // Query: SELECT * FROM Trips WHERE (newStart <= endDate) AND (newEnd >= startDate)

        String selection = COLUMN_START_DATE + " <= ? AND " + COLUMN_END_DATE + " >= ?";
        String[] selectionArgs = new String[]{endDate, startDate};

        Cursor cursor = db.query(TABLE_TRIPS, null, selection, selectionArgs, null, null, null);
        boolean isAvailable = cursor.getCount() == 0;
        cursor.close();
        db.close();
        return isAvailable;
    }
}
