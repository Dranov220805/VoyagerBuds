package com.example.voyagerbuds.models;

import java.io.Serializable;

public class Trip implements Serializable {
    private int tripId;
    private int userId;
    private String tripName;
    private String startDate;
    private String endDate;
    private String destination;
    private String notes;
    private String photoUrl;
    private long createdAt;
    private long updatedAt;
    private int isGroupTrip;
    private double mapLatitude;
    private double mapLongitude;
    private String syncStatus;
    private int firebaseId;
    private long lastSyncedAt;
    private double budget;
    private String budgetCurrency;
    private String participants;

    public Trip() {
    }

    public Trip(int tripId, int userId, String tripName, String startDate, String endDate,
            String destination, String notes, String photoUrl, long createdAt,
            long updatedAt, int isGroupTrip, double mapLatitude, double mapLongitude,
            String syncStatus, int firebaseId, long lastSyncedAt, double budget, String budgetCurrency,
            String participants) {
        this.tripId = tripId;
        this.userId = userId;
        this.tripName = tripName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.destination = destination;
        this.notes = notes;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isGroupTrip = isGroupTrip;
        this.mapLatitude = mapLatitude;
        this.mapLongitude = mapLongitude;
        this.syncStatus = syncStatus;
        this.firebaseId = firebaseId;
        this.lastSyncedAt = lastSyncedAt;
        this.budget = budget;
        this.budgetCurrency = budgetCurrency;
        this.participants = participants;
    }

    // Getters and Setters
    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTripName() {
        return tripName;
    }

    public void setTripName(String tripName) {
        this.tripName = tripName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getIsGroupTrip() {
        return isGroupTrip;
    }

    public void setIsGroupTrip(int isGroupTrip) {
        this.isGroupTrip = isGroupTrip;
    }

    public double getMapLatitude() {
        return mapLatitude;
    }

    public void setMapLatitude(double mapLatitude) {
        this.mapLatitude = mapLatitude;
    }

    public double getMapLongitude() {
        return mapLongitude;
    }

    public void setMapLongitude(double mapLongitude) {
        this.mapLongitude = mapLongitude;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public int getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(int firebaseId) {
        this.firebaseId = firebaseId;
    }

    public long getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(long lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getBudgetCurrency() {
        return budgetCurrency;
    }

    public void setBudgetCurrency(String budgetCurrency) {
        this.budgetCurrency = budgetCurrency;
    }

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }
}
