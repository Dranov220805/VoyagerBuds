package com.example.voyagerbuds.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.voyagerbuds.database.dao.ExpenseDao;
import com.example.voyagerbuds.database.dao.ScheduleDao;
import com.example.voyagerbuds.database.dao.TripDao;
import com.example.voyagerbuds.models.Expense;
import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.models.Trip;

import java.util.List;

/**
 * DatabaseHelper - Manages database schema, migrations, and provides access to
 * DAOs.
 * This class should only contain database structure logic, not business
 * operations.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "voyagerbuds.db";
    private static final int DATABASE_VERSION = 10;

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
    private static final String COLUMN_BUDGET_CURRENCY = "budget_currency";
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
    private static final String COLUMN_EXPENSE_IMAGES = "image_paths";
    // Schedules table
    private static final String TABLE_SCHEDULES = "Schedules";
    private static final String COLUMN_SCHEDULE_ID = "scheduleId";
    private static final String COLUMN_SCHEDULE_TRIP_ID = "tripId";
    private static final String COLUMN_SCHEDULE_DAY = "day";
    private static final String COLUMN_SCHEDULE_START_TIME = "start_time";
    private static final String COLUMN_SCHEDULE_END_TIME = "end_time";
    private static final String COLUMN_SCHEDULE_TITLE = "title";
    private static final String COLUMN_SCHEDULE_NOTES = "notes";
    // private static final String COLUMN_SCHEDULE_ICON = "icon"; // Removed
    private static final String COLUMN_SCHEDULE_LOCATION = "location";
    private static final String COLUMN_SCHEDULE_PARTICIPANTS = "participants";
    // private static final String COLUMN_SCHEDULE_EXPENSE_AMOUNT =
    // "expense_amount"; // Removed
    // private static final String COLUMN_SCHEDULE_EXPENSE_CURRENCY =
    // "expense_currency"; // Removed
    private static final String COLUMN_SCHEDULE_IMAGES = "image_paths";
    private static final String COLUMN_SCHEDULE_NOTIFY_BEFORE = "notify_before_minutes";
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
                + COLUMN_BUDGET_CURRENCY + " TEXT DEFAULT 'USD',"
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
                + COLUMN_EXPENSE_IMAGES + " TEXT,"
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
                // + COLUMN_SCHEDULE_ICON + " TEXT," // Removed
                + COLUMN_SCHEDULE_LOCATION + " TEXT,"
                + COLUMN_SCHEDULE_PARTICIPANTS + " TEXT,"
                // + COLUMN_SCHEDULE_EXPENSE_AMOUNT + " REAL," // Removed
                // + COLUMN_SCHEDULE_EXPENSE_CURRENCY + " TEXT," // Removed
                + COLUMN_SCHEDULE_IMAGES + " TEXT,"
                + COLUMN_SCHEDULE_NOTIFY_BEFORE + " INTEGER DEFAULT 0,"
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
        if (oldVersion < 4) {
            // Add icon, location, and participants columns to Schedules table
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + "icon" + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COLUMN_SCHEDULE_LOCATION + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COLUMN_SCHEDULE_PARTICIPANTS + " TEXT");
        }
        if (oldVersion < 5) {
            // Add images column to Schedules table
            // Previous versions added expense columns here, but those fields have been
            // removed from the data model. We avoid reintroducing them during upgrade.
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COLUMN_SCHEDULE_IMAGES + " TEXT");
            // Note: We are leaving the 'icon' column alone to avoid complex migration
            // logic; it will remain unused in the code.
        }
        if (oldVersion < 6) {
            // Add notify_before_minutes column to Schedules table
            db.execSQL("ALTER TABLE " + TABLE_SCHEDULES + " ADD COLUMN " + COLUMN_SCHEDULE_NOTIFY_BEFORE
                    + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 9) {
            // Add image_paths column to Expenses table
            db.execSQL("ALTER TABLE " + TABLE_EXPENSES + " ADD COLUMN " + COLUMN_EXPENSE_IMAGES + " TEXT");
        }

        // Version 10: Add budget_currency to Trips table
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE " + TABLE_TRIPS + " ADD COLUMN " + COLUMN_BUDGET_CURRENCY
                    + " TEXT DEFAULT 'USD'");
        }
    }

    // Trip CRUD operations - Delegate to DAO
    public long addTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        TripDao dao = new TripDao(db);
        long id = dao.insert(trip);
        db.close();
        return id;
    }

    // Schedule CRUD - Delegate to DAO
    public long addSchedule(ScheduleItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ScheduleDao dao = new ScheduleDao(db);
        long id = dao.insert(item);
        db.close();
        return id;
    }

    public List<ScheduleItem> getSchedulesForTrip(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ScheduleDao dao = new ScheduleDao(db);
        List<ScheduleItem> list = dao.getAllByTripId(tripId);
        db.close();
        return list;
    }

    public int updateSchedule(ScheduleItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ScheduleDao dao = new ScheduleDao(db);
        int result = dao.update(item);
        db.close();
        return result;
    }

    public void deleteSchedule(int scheduleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ScheduleDao dao = new ScheduleDao(db);
        dao.delete(scheduleId);
        db.close();
    }

    public List<Trip> getAllTrips(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        TripDao dao = new TripDao(db);
        List<Trip> list = dao.getAllByUserId(userId);
        db.close();
        return list;
    }

    public Trip getTripById(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        TripDao dao = new TripDao(db);
        Trip trip = dao.getById(tripId);
        db.close();
        return trip;
    }

    public double getTotalExpensesForTrip(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ExpenseDao dao = new ExpenseDao(db);
        double total = dao.getTotalByTripId(tripId);
        db.close();
        return total;
    }

    public java.util.Map<String, Double> getTotalExpensesByCurrency(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ExpenseDao dao = new ExpenseDao(db);
        java.util.Map<String, Double> totals = dao.getTotalsByCurrency(tripId);
        db.close();
        return totals;
    }

    public int updateTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        TripDao dao = new TripDao(db);
        int result = dao.update(trip);
        db.close();
        return result;
    }

    public void deleteTrip(int tripId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ExpenseDao expenseDao = new ExpenseDao(db);
        ScheduleDao scheduleDao = new ScheduleDao(db);
        TripDao tripDao = new TripDao(db);

        // Delete related data first
        expenseDao.deleteByTripId(tripId);
        scheduleDao.deleteByTripId(tripId);
        tripDao.delete(tripId);
        db.close();
    }

    public boolean isDateRangeAvailable(int userId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        TripDao dao = new TripDao(db);
        List<Trip> overlappingTrips = dao.getTripsByDateRange(userId, startDate, endDate);
        db.close();
        return overlappingTrips.isEmpty();
    }

    public boolean isDateRangeAvailable(int userId, String startDate, String endDate, int excludeTripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        TripDao dao = new TripDao(db);
        List<Trip> overlappingTrips = dao.getTripsByDateRangeExcluding(userId, startDate, endDate, excludeTripId);
        db.close();
        return overlappingTrips.isEmpty();
    }

    public ScheduleItem getScheduleById(int scheduleId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ScheduleDao dao = new ScheduleDao(db);
        ScheduleItem item = dao.getById(scheduleId);
        db.close();
        return item;
    }

    public void updateScheduleImages(int scheduleId, String imagesJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        ScheduleDao dao = new ScheduleDao(db);
        dao.updateImages(scheduleId, imagesJson);
        db.close();
    }

    public List<Expense> getExpensesForTrip(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ExpenseDao dao = new ExpenseDao(db);
        List<Expense> expenses = dao.getAllByTripId(tripId);
        db.close();
        return expenses;
    }

    public long addExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ExpenseDao dao = new ExpenseDao(db);
        long id = dao.insert(expense);
        db.close();
        return id;
    }

    public int updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ExpenseDao dao = new ExpenseDao(db);
        int rows = dao.update(expense);
        db.close();
        return rows;
    }

    public void deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ExpenseDao dao = new ExpenseDao(db);
        dao.delete(expenseId);
        db.close();
    }

    public Expense getExpenseById(int expenseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ExpenseDao dao = new ExpenseDao(db);
        Expense expense = dao.getById(expenseId);
        db.close();
        return expense;
    }

    public void updateExpenseImages(int expenseId, String imagesJson) {
        SQLiteDatabase db = this.getWritableDatabase();
        ExpenseDao dao = new ExpenseDao(db);
        dao.updateImages(expenseId, imagesJson);
        db.close();
    }
}
