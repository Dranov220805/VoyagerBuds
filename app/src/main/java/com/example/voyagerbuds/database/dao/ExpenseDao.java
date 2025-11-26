package com.example.voyagerbuds.database.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.voyagerbuds.models.Expense;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Expense entity.
 * Contains only pure database CRUD operations without business logic.
 */
public class ExpenseDao {
    // Table and column names
    private static final String TABLE_EXPENSES = "Expenses";
    private static final String COLUMN_EXPENSE_ID = "expenseId";
    private static final String COLUMN_EXPENSE_TRIP_ID = "tripId";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_CURRENCY = "currency";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_SPENT_AT = "spent_at";
    private static final String COLUMN_EXPENSE_IMAGES = "image_paths";

    private final SQLiteDatabase database;

    public ExpenseDao(SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * Insert a new expense
     */
    public long insert(Expense expense) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPENSE_TRIP_ID, expense.getTripId());
        values.put(COLUMN_CATEGORY, expense.getCategory());
        values.put(COLUMN_AMOUNT, expense.getAmount());
        values.put(COLUMN_CURRENCY, expense.getCurrency());
        values.put(COLUMN_NOTE, expense.getNote());
        values.put(COLUMN_SPENT_AT, expense.getSpentAt());
        values.put(COLUMN_EXPENSE_IMAGES, expense.getImagePaths());

        return database.insert(TABLE_EXPENSES, null, values);
    }

    /**
     * Update an existing expense
     */
    public int update(Expense expense) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY, expense.getCategory());
        values.put(COLUMN_AMOUNT, expense.getAmount());
        values.put(COLUMN_CURRENCY, expense.getCurrency());
        values.put(COLUMN_NOTE, expense.getNote());
        values.put(COLUMN_SPENT_AT, expense.getSpentAt());
        values.put(COLUMN_EXPENSE_IMAGES, expense.getImagePaths());

        return database.update(TABLE_EXPENSES, values, COLUMN_EXPENSE_ID + " = ?",
                new String[] { String.valueOf(expense.getExpenseId()) });
    }

    /**
     * Delete an expense by ID
     */
    public int delete(int expenseId) {
        return database.delete(TABLE_EXPENSES, COLUMN_EXPENSE_ID + " = ?",
                new String[] { String.valueOf(expenseId) });
    }

    /**
     * Delete all expenses for a trip
     */
    public int deleteByTripId(int tripId) {
        return database.delete(TABLE_EXPENSES, COLUMN_EXPENSE_TRIP_ID + " = ?",
                new String[] { String.valueOf(tripId) });
    }

    /**
     * Get expense by ID
     */
    public Expense getById(int expenseId) {
        Cursor cursor = database.query(TABLE_EXPENSES, null, COLUMN_EXPENSE_ID + "=?",
                new String[] { String.valueOf(expenseId) }, null, null, null);

        Expense expense = null;
        if (cursor != null && cursor.moveToFirst()) {
            expense = cursorToExpense(cursor);
            cursor.close();
        }
        return expense;
    }

    /**
     * Get all expenses for a trip, ordered by spent date descending
     */
    public List<Expense> getAllByTripId(int tripId) {
        List<Expense> expenses = new ArrayList<>();
        Cursor cursor = database.query(TABLE_EXPENSES, null, COLUMN_EXPENSE_TRIP_ID + "=?",
                new String[] { String.valueOf(tripId) }, null, null, COLUMN_SPENT_AT + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                expenses.add(cursorToExpense(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return expenses;
    }

    /**
     * Get expenses by category for a trip
     */
    public List<Expense> getByTripIdAndCategory(int tripId, String category) {
        List<Expense> expenses = new ArrayList<>();
        String selection = COLUMN_EXPENSE_TRIP_ID + " = ? AND " + COLUMN_CATEGORY + " = ?";
        String[] selectionArgs = new String[] { String.valueOf(tripId), category };

        Cursor cursor = database.query(TABLE_EXPENSES, null, selection, selectionArgs,
                null, null, COLUMN_SPENT_AT + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                expenses.add(cursorToExpense(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return expenses;
    }

    /**
     * Get total amount of expenses for a trip
     */
    public double getTotalByTripId(int tripId) {
        String query = "SELECT SUM(" + COLUMN_AMOUNT + ") as total FROM " + TABLE_EXPENSES + " WHERE "
                + COLUMN_EXPENSE_TRIP_ID + " = ?";
        Cursor cursor = database.rawQuery(query, new String[] { String.valueOf(tripId) });

        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
        }
        cursor.close();
        return total;
    }

    /**
     * Get total amount by category for a trip
     */
    public double getTotalByTripIdAndCategory(int tripId, String category) {
        String query = "SELECT SUM(" + COLUMN_AMOUNT + ") as total FROM " + TABLE_EXPENSES + " WHERE "
                + COLUMN_EXPENSE_TRIP_ID + " = ? AND " + COLUMN_CATEGORY + " = ?";
        Cursor cursor = database.rawQuery(query, new String[] { String.valueOf(tripId), category });

        double total = 0.0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
        }
        cursor.close();
        return total;
    }

    /**
     * Convert cursor to Expense object
     */
    private Expense cursorToExpense(Cursor cursor) {
        Expense expense = new Expense();
        expense.setExpenseId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_ID)));
        expense.setTripId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_TRIP_ID)));
        expense.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
        expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
        expense.setCurrency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)));
        expense.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)));
        expense.setSpentAt(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SPENT_AT)));
        
        int imageColIndex = cursor.getColumnIndex(COLUMN_EXPENSE_IMAGES);
        if (imageColIndex != -1) {
            expense.setImagePaths(cursor.getString(imageColIndex));
        }
        
        return expense;
    }
}
