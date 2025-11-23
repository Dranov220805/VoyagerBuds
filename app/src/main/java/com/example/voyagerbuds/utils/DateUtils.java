package com.example.voyagerbuds.utils;

import android.content.Context;
import android.os.Build;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static Locale getLocale(Context context) {
        if (context == null)
            return Locale.getDefault();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            return context.getResources().getConfiguration().locale;
        }
    }

    public static boolean isVietnamese(Context context) {
        Locale locale = getLocale(context);
        return locale != null && "vi".equals(locale.getLanguage());
    }

    public static boolean isVietnamese(Locale locale) {
        return locale != null && "vi".equals(locale.getLanguage());
    }

    public static String formatShortDate(Locale locale, Date date) {
        if (date == null)
            return "";
        if (isVietnamese(locale)) {
            SimpleDateFormat viFormat = new SimpleDateFormat("dd/MM", new Locale("vi"));
            return viFormat.format(date);
        }
        SimpleDateFormat defaultFormat = new SimpleDateFormat("MMM dd", locale == null ? Locale.getDefault() : locale);
        return defaultFormat.format(date);
    }

    public static String formatFullDate(Locale locale, Date date) {
        if (date == null)
            return "";
        if (isVietnamese(locale)) {
            SimpleDateFormat viFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi"));
            return viFormat.format(date);
        }
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, locale == null ? Locale.getDefault() : locale);
        return df.format(date);
    }

    public static String formatFullDate(Context context, Date date) {
        if (date == null)
            return "";
        if (isVietnamese(context)) {
            SimpleDateFormat viFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi"));
            return viFormat.format(date);
        }
        // Default to Medium style for other locales (e.g., Apr 13, 2021)
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, getLocale(context));
        return df.format(date);
    }

    public static String formatShortDate(Context context, Date date) {
        if (date == null)
            return "";
        if (isVietnamese(context)) {
            SimpleDateFormat viFormat = new SimpleDateFormat("dd/MM", new Locale("vi"));
            return viFormat.format(date);
        }
        SimpleDateFormat defaultFormat = new SimpleDateFormat("MMM dd", getLocale(context));
        return defaultFormat.format(date);
    }

    public static String formatDay(Context context, Date date) {
        if (date == null)
            return "";
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", getLocale(context));
        return dayFormat.format(date);
    }

    /**
     * Format a date range in a locale-aware way that uses numeric (dd/MM) for
     * Vietnamese.
     */
    public static String formatDateRangeSimple(Context context, Date start, Date end) {
        if (start == null || end == null)
            return "";
        if (isVietnamese(context)) {
            SimpleDateFormat startFmt = new SimpleDateFormat("dd/MM", new Locale("vi"));
            SimpleDateFormat endFmt = new SimpleDateFormat("dd/MM", new Locale("vi"));
            SimpleDateFormat yearFmt = new SimpleDateFormat("yyyy", new Locale("vi"));
            return startFmt.format(start) + " - " + endFmt.format(end) + ", " + yearFmt.format(end);
        }
        SimpleDateFormat monthDay = new SimpleDateFormat("MMM dd", getLocale(context));
        SimpleDateFormat year = new SimpleDateFormat("yyyy", getLocale(context));
        return monthDay.format(start) + " - " + monthDay.format(end) + ", " + year.format(end);
    }

    /**
     * Format a date range (start - end) with week day and month/day or numeric date
     */
    public static String formatDateRangeHome(Context context, Date start, Date end) {
        if (start == null || end == null)
            return "";
        if (isVietnamese(context)) {
            SimpleDateFormat full = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi"));
            SimpleDateFormat shortD = new SimpleDateFormat("dd", new Locale("vi"));
            if (sameMonthYear(start, end)) {
                // If same month/year: start "dd/MM" - end "dd"
                SimpleDateFormat startFmt = new SimpleDateFormat("dd/MM", new Locale("vi"));
                return startFmt.format(start) + " - " + shortD.format(end);
            }
            SimpleDateFormat startFmt = new SimpleDateFormat("dd/MM", new Locale("vi"));
            SimpleDateFormat endFmt = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi"));
            return startFmt.format(start) + " - " + endFmt.format(end);
        }
        // default behavior using month names
        SimpleDateFormat inputFormat = new SimpleDateFormat("EEE - MMM dd", getLocale(context));
        // We'll fallback to returning as 'MMM dd - MMM dd' similar to previous
        // formatting
        SimpleDateFormat monthDay = new SimpleDateFormat("MMM dd", getLocale(context));
        if (sameMonthYear(start, end)) {
            // start MMM dd - end dd
            return monthDay.format(start) + " - " + new SimpleDateFormat("dd", getLocale(context)).format(end);
        }
        return monthDay.format(start) + " - " + monthDay.format(end);
    }

    private static boolean sameMonthYear(Date start, Date end) {
        if (start == null || end == null)
            return false;
        SimpleDateFormat monthYear = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        return monthYear.format(start).equals(monthYear.format(end));
    }

}
