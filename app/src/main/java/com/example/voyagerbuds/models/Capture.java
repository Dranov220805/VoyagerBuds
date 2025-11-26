package com.example.voyagerbuds.models;

import java.io.Serializable;

/**
 * Capture model represents a photo or video captured during a trip.
 * Acts as a diary entry with media and optional description.
 */
public class Capture implements Serializable {
    private int captureId;
    private int userId;
    private int tripId;
    private String mediaPath; // Path to the photo/video file
    private String mediaType; // "photo" or "video"
    private String description; // User's note/diary entry
    private long capturedAt; // Timestamp when media was captured
    private long createdAt; // Timestamp when entry was created in DB
    private long updatedAt; // Timestamp of last update

    public Capture() {
    }

    public Capture(int captureId, int userId, int tripId, String mediaPath, String mediaType,
                   String description, long capturedAt, long createdAt, long updatedAt) {
        this.captureId = captureId;
        this.userId = userId;
        this.tripId = tripId;
        this.mediaPath = mediaPath;
        this.mediaType = mediaType;
        this.description = description;
        this.capturedAt = capturedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getCaptureId() {
        return captureId;
    }

    public void setCaptureId(int captureId) {
        this.captureId = captureId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(long capturedAt) {
        this.capturedAt = capturedAt;
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
}
