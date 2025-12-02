package com.example.voyagerbuds.models;

import java.util.List;

/**
 * AlbumSection represents a trip with its associated captures
 * Used for organizing album view by trip
 */
public class AlbumSection {
    private Trip trip;
    private List<AlbumDay> days;

    public AlbumSection(Trip trip, List<AlbumDay> days) {
        this.trip = trip;
        this.days = days;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public List<AlbumDay> getDays() {
        return days;
    }

    public void setDays(List<AlbumDay> days) {
        this.days = days;
    }
}
