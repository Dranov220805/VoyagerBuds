package com.example.voyagerbuds.models;

public class GalleryItem {
    private String imagePath;
    private int itemId; // Can be scheduleId or expenseId
    private int itemType; // 0 = schedule, 1 = expense
    private long date;
    private String dayLabel;
    private boolean isSelected;

    public GalleryItem(String imagePath, int itemId, int itemType, String dayLabel) {
        this.imagePath = imagePath;
        this.itemId = itemId;
        this.itemType = itemType;
        this.date = 0;
        this.dayLabel = dayLabel;
        this.isSelected = false;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getItemId() {
        return itemId;
    }

    public int getScheduleId() {
        return itemId; // For backward compatibility
    }

    public int getItemType() {
        return itemType;
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
