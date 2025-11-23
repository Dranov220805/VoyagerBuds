package com.example.voyagerbuds.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.voyagerbuds.R;
import com.google.android.material.datepicker.DayViewDecorator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class BookedDateDecorator extends DayViewDecorator {

    private final List<Long> bookedRanges;

    public BookedDateDecorator(List<Long> bookedRanges) {
        this.bookedRanges = bookedRanges;
    }

    protected BookedDateDecorator(Parcel in) {
        bookedRanges = new ArrayList<>();
        in.readList(bookedRanges, Long.class.getClassLoader());
    }

    public static final Creator<BookedDateDecorator> CREATOR = new Creator<BookedDateDecorator>() {
        @Override
        public BookedDateDecorator createFromParcel(Parcel in) {
            return new BookedDateDecorator(in);
        }

        @Override
        public BookedDateDecorator[] newArray(int size) {
            return new BookedDateDecorator[size];
        }
    };

    @Nullable
    @Override
    public Drawable getCompoundDrawableBottom(@NonNull Context context, int year, int month, int day, boolean valid,
            boolean selected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long date = calendar.getTimeInMillis();

        // Check for past dates (before today)
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        if (date < today.getTimeInMillis()) {
            return ContextCompat.getDrawable(context, R.drawable.ic_underline_red);
        }

        if (isBooked(date)) {
            return ContextCompat.getDrawable(context, R.drawable.ic_underline_red);
        }
        return null;
    }

    @Nullable
    @Override
    public ColorStateList getTextColor(@NonNull Context context, int year, int month, int day, boolean valid,
            boolean selected) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long date = calendar.getTimeInMillis();

        // Check for past dates (before today)
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        if (date < today.getTimeInMillis()) {
            return ColorStateList.valueOf(Color.RED);
        }

        if (isBooked(date)) {
            return ColorStateList.valueOf(Color.RED);
        }
        return null;
    }

    private boolean isBooked(long date) {
        if (bookedRanges == null)
            return false;

        // Normalize date to UTC midnight for comparison if needed,
        // but MaterialDatePicker usually passes UTC midnight timestamps.

        for (int i = 0; i < bookedRanges.size(); i += 2) {
            long start = bookedRanges.get(i);
            long end = bookedRanges.get(i + 1);
            if (date >= start && date <= end) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(bookedRanges);
    }
}
