package com.example.voyagerbuds.models;

import java.util.List;

/**
 * AlbumDay represents captures taken on a specific day
 * Used for organizing album view by date within a trip
 */
public class AlbumDay {
    private String dateLabel; // e.g., "Day 1 - Dec 1, 2025"
    private long timestamp; // Timestamp for the day (midnight UTC)
    private List<Capture> captures;

    public AlbumDay(String dateLabel, long timestamp, List<Capture> captures) {
        this.dateLabel = dateLabel;
        this.timestamp = timestamp;
        this.captures = captures;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<Capture> getCaptures() {
        return captures;
    }

    public void setCaptures(List<Capture> captures) {
        this.captures = captures;
    }
}
