package com.example.voyagerbuds.models;

import java.io.Serializable;

public class ScheduleItem implements Serializable {
    private int id;
    private int tripId;
    private String day; // e.g., 2025-11-20 or "Day 1"
    // We'll store start and end times separately
    private String startTime; // e.g., "09:00"
    private String endTime; // e.g., "11:00"
    private String title;
    private String notes;
    // private String icon; // Removed as per requirement
    private String location; // location string
    private String participants; // comma-separated participant names
    private String imagePaths; // JSON or comma-separated paths
    private int notifyBeforeMinutes; // 0 means no notification
    private long createdAt;
    private long updatedAt;

    public ScheduleItem() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public String getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(String imagePaths) {
        this.imagePaths = imagePaths;
    }

    public int getNotifyBeforeMinutes() {
        return notifyBeforeMinutes;
    }

    public void setNotifyBeforeMinutes(int notifyBeforeMinutes) {
        this.notifyBeforeMinutes = notifyBeforeMinutes;
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

    /*
     * public String getIcon() {
     * return icon;
     * }
     * 
     * public void setIcon(String icon) {
     * this.icon = icon;
     * }
     */
}
