package com.example.voyagerbuds.utils;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.material.datepicker.CalendarConstraints;

/**
 * Date validator that only allows dates within a specific range (for schedule
 * dates within a trip)
 * Dates outside the range are marked invalid (red and untouchable)
 */
public class DateValidatorWithinRange implements CalendarConstraints.DateValidator {

    private final long startDate; // Trip start date in UTC millis
    private final long endDate; // Trip end date in UTC millis

    public DateValidatorWithinRange(long startDate, long endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private DateValidatorWithinRange(Parcel in) {
        startDate = in.readLong();
        endDate = in.readLong();
    }

    public static final Creator<DateValidatorWithinRange> CREATOR = new Creator<DateValidatorWithinRange>() {
        @Override
        public DateValidatorWithinRange createFromParcel(Parcel in) {
            return new DateValidatorWithinRange(in);
        }

        @Override
        public DateValidatorWithinRange[] newArray(int size) {
            return new DateValidatorWithinRange[size];
        }
    };

    @Override
    public boolean isValid(long date) {
        // Date is valid only if it falls within the trip's date range
        return date >= startDate && date <= endDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(startDate);
        dest.writeLong(endDate);
    }
}
