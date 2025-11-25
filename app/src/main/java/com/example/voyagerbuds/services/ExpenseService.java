package com.example.voyagerbuds.services;

import android.content.Context;

import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Expense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for handling Expense-related business logic.
 * Provides expense management, categorization, and analytics.
 */
public class ExpenseService {
    private final DatabaseHelper databaseHelper;

    public ExpenseService(Context context) {
        this.databaseHelper = new DatabaseHelper(context);
    }

    /**
     * Add a new expense
     * 
     * @param expense The expense to add
     * @return The ID of the newly created expense
     */
    public long createExpense(Expense expense) {
        // Set creation timestamp if not set
        if (expense.getSpentAt() == 0) {
            expense.setSpentAt((int) (System.currentTimeMillis() / 1000));
        }
        return databaseHelper.addExpense(expense);
    }

    /**
     * Update an existing expense
     * 
     * @param expense The expense with updated information
     * @return Number of rows affected
     */
    public int updateExpense(Expense expense) {
        return databaseHelper.updateExpense(expense);
    }

    /**
     * Delete an expense
     * 
     * @param expenseId The ID of the expense to delete
     */
    public void deleteExpense(int expenseId) {
        databaseHelper.deleteExpense(expenseId);
    }

    /**
     * Get all expenses for a trip
     * 
     * @param tripId The trip ID
     * @return List of expenses
     */
    public List<Expense> getExpensesForTrip(int tripId) {
        return databaseHelper.getExpensesForTrip(tripId);
    }

    /**
     * Get total expenses for a trip
     * 
     * @param tripId The trip ID
     * @return Total expense amount
     */
    public double getTotalExpenses(int tripId) {
        return databaseHelper.getTotalExpensesForTrip(tripId);
    }

    /**
     * Get expenses grouped by category
     * 
     * @param tripId The trip ID
     * @return Map of category to list of expenses
     */
    public Map<String, List<Expense>> getExpensesByCategory(int tripId) {
        List<Expense> allExpenses = getExpensesForTrip(tripId);
        Map<String, List<Expense>> categoryMap = new HashMap<>();

        for (Expense expense : allExpenses) {
            String category = expense.getCategory();
            if (category == null || category.isEmpty()) {
                category = "Uncategorized";
            }

            if (!categoryMap.containsKey(category)) {
                categoryMap.put(category, new ArrayList<>());
            }
            categoryMap.get(category).add(expense);
        }

        return categoryMap;
    }

    /**
     * Get total expenses by category
     * 
     * @param tripId The trip ID
     * @return Map of category to total amount
     */
    public Map<String, Double> getTotalsByCategory(int tripId) {
        List<Expense> allExpenses = getExpensesForTrip(tripId);
        Map<String, Double> totals = new HashMap<>();

        for (Expense expense : allExpenses) {
            String category = expense.getCategory();
            if (category == null || category.isEmpty()) {
                category = "Uncategorized";
            }

            double currentTotal = totals.getOrDefault(category, 0.0);
            totals.put(category, currentTotal + expense.getAmount());
        }

        return totals;
    }

    /**
     * Get percentage of budget spent by category
     * 
     * @param tripId      The trip ID
     * @param totalBudget The total budget for the trip
     * @return Map of category to percentage
     */
    public Map<String, Double> getCategoryPercentages(int tripId, double totalBudget) {
        Map<String, Double> totals = getTotalsByCategory(tripId);
        Map<String, Double> percentages = new HashMap<>();

        if (totalBudget <= 0) {
            return percentages;
        }

        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            double percentage = (entry.getValue() / totalBudget) * 100;
            percentages.put(entry.getKey(), percentage);
        }

        return percentages;
    }

    /**
     * Get the highest expense for a trip
     * 
     * @param tripId The trip ID
     * @return The expense with the highest amount, or null if no expenses
     */
    public Expense getHighestExpense(int tripId) {
        List<Expense> expenses = getExpensesForTrip(tripId);
        if (expenses.isEmpty()) {
            return null;
        }

        Expense highest = expenses.get(0);
        for (Expense expense : expenses) {
            if (expense.getAmount() > highest.getAmount()) {
                highest = expense;
            }
        }
        return highest;
    }

    /**
     * Get average expense amount for a trip
     * 
     * @param tripId The trip ID
     * @return Average expense amount
     */
    public double getAverageExpense(int tripId) {
        List<Expense> expenses = getExpensesForTrip(tripId);
        if (expenses.isEmpty()) {
            return 0.0;
        }

        double total = getTotalExpenses(tripId);
        return total / expenses.size();
    }

    /**
     * Get the most expensive category
     * 
     * @param tripId The trip ID
     * @return Category name with highest total, or null if no expenses
     */
    public String getMostExpensiveCategory(int tripId) {
        Map<String, Double> totals = getTotalsByCategory(tripId);
        if (totals.isEmpty()) {
            return null;
        }

        String mostExpensive = null;
        double highestTotal = 0.0;

        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            if (entry.getValue() > highestTotal) {
                highestTotal = entry.getValue();
                mostExpensive = entry.getKey();
            }
        }

        return mostExpensive;
    }

    /**
     * Validate expense data
     * 
     * @param expense The expense to validate
     * @return Error message if invalid, null if valid
     */
    public String validateExpense(Expense expense) {
        if (expense.getTripId() <= 0) {
            return "Invalid trip ID";
        }

        if (expense.getAmount() <= 0) {
            return "Amount must be greater than zero";
        }

        if (expense.getCategory() == null || expense.getCategory().trim().isEmpty()) {
            return "Category is required";
        }

        if (expense.getCurrency() == null || expense.getCurrency().trim().isEmpty()) {
            return "Currency is required";
        }

        return null; // Valid
    }

    /**
     * Check if expenses exceed budget
     * 
     * @param tripId The trip ID
     * @param budget The trip budget
     * @return true if expenses exceed budget
     */
    public boolean isOverBudget(int tripId, double budget) {
        double total = getTotalExpenses(tripId);
        return total > budget;
    }

    /**
     * Get remaining budget
     * 
     * @param tripId The trip ID
     * @param budget The trip budget
     * @return Remaining budget amount
     */
    public double getRemainingBudget(int tripId, double budget) {
        double total = getTotalExpenses(tripId);
        return budget - total;
    }
}
