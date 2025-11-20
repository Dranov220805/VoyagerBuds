package com.example.voyagerbuds.utils;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.material.datepicker.CalendarConstraints;

import java.util.ArrayList;
import java.util.List;

public class DateValidatorBlockTrips implements CalendarConstraints.DateValidator {

    private final List<Long> blockedRanges; // Stored as start1, end1, start2, end2...

    public DateValidatorBlockTrips(List<Long> blockedRanges) {
        this.blockedRanges = blockedRanges;
    }

    private DateValidatorBlockTrips(Parcel in) {
        blockedRanges = new ArrayList<>();
        in.readList(blockedRanges, Long.class.getClassLoader());
    }

    public static final Creator<DateValidatorBlockTrips> CREATOR = new Creator<DateValidatorBlockTrips>() {
        @Override
        public DateValidatorBlockTrips createFromParcel(Parcel in) {
            return new DateValidatorBlockTrips(in);
        }

        @Override
        public DateValidatorBlockTrips[] newArray(int size) {
            return new DateValidatorBlockTrips[size];
        }
    };

    @Override
    public boolean isValid(long date) {
        if (blockedRanges == null) return true;
        
        for (int i = 0; i < blockedRanges.size(); i += 2) {
            long start = blockedRanges.get(i);
            long end = blockedRanges.get(i + 1);
            // If date is within the range [start, end], it is invalid
            if (date >= start && date <= end) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(blockedRanges);
    }
}
