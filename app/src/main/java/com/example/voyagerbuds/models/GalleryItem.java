package com.example.voyagerbuds.models;

public class GalleryItem {
    private String imagePath;
    private int scheduleId;
    private long date;
    private String dayLabel;
    private boolean isSelected;

    public GalleryItem(String imagePath, int scheduleId, long date, String dayLabel) {
        this.imagePath = imagePath;
        this.scheduleId = scheduleId;
        this.date = date;
        this.dayLabel = dayLabel;
        this.isSelected = false;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public long getDate() {
        return date;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
