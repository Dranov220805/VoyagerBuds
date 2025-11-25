package com.example.voyagerbuds.services;

import android.content.Context;

import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Expense;
import com.example.voyagerbuds.models.Trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Service class for handling Trip-related business logic.
 * Encapsulates database operations and business rules for trips.
 */
public class TripService {
    private final DatabaseHelper databaseHelper;
    private final ExpenseService expenseService;
    private final Context context;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public TripService(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
        this.expenseService = new ExpenseService(context);
    }

    /**
     * Add a new trip to the database with validation
     * 
     * @param trip The trip to add
     * @return The ID of the newly created trip, or -1 if failed
     */
    public long createTrip(Trip trip) {
        // Validate trip data
        String validationError = validateTrip(trip);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        // Set timestamps if not already set
        if (trip.getCreatedAt() == 0) {
            trip.setCreatedAt(System.currentTimeMillis());
        }
        trip.setUpdatedAt(System.currentTimeMillis());

        // Set default sync status
        if (trip.getSyncStatus() == null || trip.getSyncStatus().isEmpty()) {
            trip.setSyncStatus("pending");
        }

        return databaseHelper.addTrip(trip);
    }

    /**
     * Get all trips for a specific user
     * 
     * @param userId The user ID
     * @return List of trips
     */
    public List<Trip> getAllTrips(int userId) {
        return databaseHelper.getAllTrips(userId);
    }

    /**
     * Get a specific trip by ID
     * 
     * @param tripId The trip ID
     * @return The trip or null if not found
     */
    public Trip getTripById(int tripId) {
        return databaseHelper.getTripById(tripId);
    }

    /**
     * Update an existing trip with validation
     * 
     * @param trip The trip with updated information
     * @return Number of rows affected
     */
    public int updateTrip(Trip trip) {
        // Validate trip data
        String validationError = validateTrip(trip);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        // Update timestamp
        trip.setUpdatedAt(System.currentTimeMillis());

        return databaseHelper.updateTrip(trip);
    }

    /**
     * Delete a trip and all its associated data
     * 
     * @param tripId The ID of the trip to delete
     */
    public void deleteTrip(int tripId) {
        // DatabaseHelper already handles cascade delete
        databaseHelper.deleteTrip(tripId);
    }

    /**
     * Check if a date range is available (no conflicting trips)
     * 
     * @param startDate Start date in yyyy-MM-dd format
     * @param endDate   End date in yyyy-MM-dd format
     * @return true if available, false if conflicts exist
     */
    public boolean isDateRangeAvailable(String startDate, String endDate) {
        // Additional validation
        if (!isValidDateRange(startDate, endDate)) {
            return false;
        }
        return databaseHelper.isDateRangeAvailable(startDate, endDate);
    }

    /**
     * Check if a date range is available, excluding a specific trip
     * 
     * @param startDate     Start date in yyyy-MM-dd format
     * @param endDate       End date in yyyy-MM-dd format
     * @param excludeTripId Trip ID to exclude from conflict check
     * @return true if available, false if conflicts exist
     */
    public boolean isDateRangeAvailable(String startDate, String endDate, int excludeTripId) {
        if (!isValidDateRange(startDate, endDate)) {
            return false;
        }
        return databaseHelper.isDateRangeAvailable(startDate, endDate, excludeTripId);
    }

    /**
     * Get total expenses for a trip
     * 
     * @param tripId The trip ID
     * @return Total expense amount
     */
    public double getTotalExpenses(int tripId) {
        return expenseService.getTotalExpenses(tripId);
    }

    /**
     * Get all expenses for a trip
     * 
     * @param tripId The trip ID
     * @return List of expenses
     */
    public List<Expense> getExpenses(int tripId) {
        return expenseService.getExpensesForTrip(tripId);
    }

    /**
     * Add a new expense
     * 
     * @param expense The expense to add
     * @return The ID of the newly created expense
     */
    public long addExpense(Expense expense) {
        return expenseService.createExpense(expense);
    }

    /**
     * Update an existing expense
     * 
     * @param expense The expense with updated information
     * @return Number of rows affected
     */
    public int updateExpense(Expense expense) {
        return expenseService.updateExpense(expense);
    }

    /**
     * Delete an expense
     * 
     * @param expenseId The ID of the expense to delete
     */
    public void deleteExpense(int expenseId) {
        expenseService.deleteExpense(expenseId);
    }

    /**
     * Calculate remaining budget for a trip
     * 
     * @param tripId The trip ID
     * @return Remaining budget (budget - total expenses)
     */
    public double getRemainingBudget(int tripId) {
        Trip trip = getTripById(tripId);
        if (trip == null) {
            return 0.0;
        }
        return expenseService.getRemainingBudget(tripId, trip.getBudget());
    }

    /**
     * Check if trip is over budget
     * 
     * @param tripId The trip ID
     * @return true if expenses exceed budget
     */
    public boolean isOverBudget(int tripId) {
        Trip trip = getTripById(tripId);
        if (trip == null) {
            return false;
        }
        return expenseService.isOverBudget(tripId, trip.getBudget());
    }

    /**
     * Get budget utilization percentage
     * 
     * @param tripId The trip ID
     * @return Percentage of budget used (0-100+)
     */
    public double getBudgetUtilization(int tripId) {
        Trip trip = getTripById(tripId);
        if (trip == null || trip.getBudget() <= 0) {
            return 0.0;
        }
        double expenses = getTotalExpenses(tripId);
        return (expenses / trip.getBudget()) * 100;
    }

    /**
     * Get trip duration in days
     * 
     * @param trip The trip
     * @return Number of days, or 0 if dates are invalid
     */
    public int getTripDurationDays(Trip trip) {
        try {
            Date start = DATE_FORMAT.parse(trip.getStartDate());
            Date end = DATE_FORMAT.parse(trip.getEndDate());
            if (start != null && end != null) {
                long diffMillis = end.getTime() - start.getTime();
                return (int) (diffMillis / (1000 * 60 * 60 * 24)) + 1; // +1 to include both start and end days
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Check if trip is currently active (today is between start and end dates)
     * 
     * @param trip The trip
     * @return true if trip is active
     */
    public boolean isTripActive(Trip trip) {
        try {
            Date start = DATE_FORMAT.parse(trip.getStartDate());
            Date end = DATE_FORMAT.parse(trip.getEndDate());
            Date today = new Date();

            if (start != null && end != null) {
                return !today.before(start) && !today.after(end);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if trip is upcoming (start date is in the future)
     * 
     * @param trip The trip
     * @return true if trip is upcoming
     */
    public boolean isTripUpcoming(Trip trip) {
        try {
            Date start = DATE_FORMAT.parse(trip.getStartDate());
            Date today = new Date();

            if (start != null) {
                return today.before(start);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if trip is completed (end date is in the past)
     * 
     * @param trip The trip
     * @return true if trip is completed
     */
    public boolean isTripCompleted(Trip trip) {
        try {
            Date end = DATE_FORMAT.parse(trip.getEndDate());
            Date today = new Date();

            if (end != null) {
                return today.after(end);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get upcoming trips for a user
     * 
     * @param userId The user ID
     * @return List of upcoming trips
     */
    public List<Trip> getUpcomingTrips(int userId) {
        List<Trip> allTrips = getAllTrips(userId);
        List<Trip> upcomingTrips = new ArrayList<>();

        for (Trip trip : allTrips) {
            if (isTripUpcoming(trip)) {
                upcomingTrips.add(trip);
            }
        }

        return upcomingTrips;
    }

    /**
     * Get active trips for a user
     * 
     * @param userId The user ID
     * @return List of active trips
     */
    public List<Trip> getActiveTrips(int userId) {
        List<Trip> allTrips = getAllTrips(userId);
        List<Trip> activeTrips = new ArrayList<>();

        for (Trip trip : allTrips) {
            if (isTripActive(trip)) {
                activeTrips.add(trip);
            }
        }

        return activeTrips;
    }

    /**
     * Get completed trips for a user
     * 
     * @param userId The user ID
     * @return List of completed trips
     */
    public List<Trip> getCompletedTrips(int userId) {
        List<Trip> allTrips = getAllTrips(userId);
        List<Trip> completedTrips = new ArrayList<>();

        for (Trip trip : allTrips) {
            if (isTripCompleted(trip)) {
                completedTrips.add(trip);
            }
        }

        return completedTrips;
    }

    /**
     * Validate trip data
     * 
     * @param trip The trip to validate
     * @return Error message if invalid, null if valid
     */
    private String validateTrip(Trip trip) {
        if (trip.getTripName() == null || trip.getTripName().trim().isEmpty()) {
            return "Trip name is required";
        }

        if (trip.getStartDate() == null || trip.getStartDate().trim().isEmpty()) {
            return "Start date is required";
        }

        if (trip.getEndDate() == null || trip.getEndDate().trim().isEmpty()) {
            return "End date is required";
        }

        if (!isValidDateRange(trip.getStartDate(), trip.getEndDate())) {
            return "End date must be after or equal to start date";
        }

        if (trip.getBudget() < 0) {
            return "Budget cannot be negative";
        }

        return null; // Valid
    }

    /**
     * Validate date range
     * 
     * @param startDate Start date string
     * @param endDate   End date string
     * @return true if valid
     */
    private boolean isValidDateRange(String startDate, String endDate) {
        try {
            Date start = DATE_FORMAT.parse(startDate);
            Date end = DATE_FORMAT.parse(endDate);

            if (start != null && end != null) {
                return !end.before(start);
            }
        } catch (ParseException e) {
            return false;
        }
        return false;
    }
}
