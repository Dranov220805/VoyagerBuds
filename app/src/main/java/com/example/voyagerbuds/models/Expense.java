package com.example.voyagerbuds.models;

public class Expense {
    private int expenseId;
    private int tripId;
    private String category;
    private double amount;
    private String currency;
    private String note;
    private int spentAt;

    public Expense() {
    }

    public Expense(int expenseId, int tripId, String category, double amount,
            String currency, String note, int spentAt) {
        this.expenseId = expenseId;
        this.tripId = tripId;
        this.category = category;
        this.amount = amount;
        this.currency = currency;
        this.note = note;
        this.spentAt = spentAt;
    }

    // Getters and Setters
    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getSpentAt() {
        return spentAt;
    }

    public void setSpentAt(int spentAt) {
        this.spentAt = spentAt;
    }
}
