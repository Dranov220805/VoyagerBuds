package com.example.voyagerbuds.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.adapters.ScheduleAdapter;
import com.example.voyagerbuds.adapters.ScheduleDayAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.ScheduleDayGroup;
import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.models.Trip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String FLEXIBLE_DAY_KEY = "__flexible__";
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("EEE, MMM dd",
            Locale.getDefault());

    private String mParam1;
    private String mParam2;
    private long tripId = -1;

    private TextView tvTripSummary;
    private TextView tvTripDates;
    private TextView tvTripStatusHint;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyStateText;
    private HorizontalScrollView scrollDateChips;
    private LinearLayout layoutDateChips;
    private RelativeLayout layoutTimelineContainer;

    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private final List<Trip> trips = new ArrayList<>();
    private Trip selectedTrip;
    private String selectedDate = null;
    private List<String> tripDates = new ArrayList<>();

    public ScheduleFragment() {
        // Required empty public constructor
    }

    public static ScheduleFragment newInstance(String param1, String param2) {
        ScheduleFragment fragment = new ScheduleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public static ScheduleFragment newInstanceForTrip(long tripId) {
        ScheduleFragment fragment = new ScheduleFragment();
        Bundle args = new Bundle();
        args.putLong("arg_trip_id", tripId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            if (getArguments().containsKey("arg_trip_id")) {
                tripId = getArguments().getLong("arg_trip_id", -1);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        databaseHelper = new DatabaseHelper(requireContext());

        tvTripSummary = view.findViewById(R.id.tv_trip_summary);
        tvTripDates = view.findViewById(R.id.tv_trip_dates);
        tvTripStatusHint = view.findViewById(R.id.tv_trip_status_hint);
        layoutEmptyState = view.findViewById(R.id.tv_schedule_empty_state);
        tvEmptyStateText = view.findViewById(R.id.tv_empty_state_text);
        scrollDateChips = view.findViewById(R.id.scroll_date_chips);
        layoutDateChips = view.findViewById(R.id.layout_date_chips);
        layoutTimelineContainer = view.findViewById(R.id.layout_timeline_container);

        // Fade-in animation for Schedule fragment root view
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(400).start();

        loadCurrentTrip();

        return view;
    }

    private void loadCurrentTrip() {
        trips.clear();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            int userId = currentUser.getUid().hashCode();
            List<Trip> userTrips = databaseHelper.getAllTrips(userId);
            if (userTrips != null) {
                trips.addAll(userTrips);
            }
        }

        if (trips.isEmpty()) {
            selectedTrip = null;
            updateTripSummary(null);
            showEmptyState("No current trip");
            return;
        }

        long runningTripId = detectRunningTripId();

        if (runningTripId != -1) {
            for (Trip trip : trips) {
                if (trip.getTripId() == runningTripId) {
                    selectedTrip = trip;
                    break;
                }
            }
        } else {
            selectedTrip = null;
        }

        if (selectedTrip != null) {
            tripId = selectedTrip.getTripId();
            selectedDate = null;
            tripDates.clear();
            updateTripSummary(selectedTrip);
            loadSchedulesForSelectedTrip();
        } else {
            updateTripSummary(null);
            showEmptyState("No current trip");
        }
    }

    private void loadSchedulesForSelectedTrip() {
        if (selectedTrip == null) {
            showEmptyState("No current trip");
            scrollDateChips.setVisibility(View.GONE);
            return;
        }

        if (tripDates.isEmpty()) {
            generateDateChips();
        }

        List<ScheduleItem> scheduleItems = databaseHelper.getSchedulesForTrip(selectedTrip.getTripId());
        
        if (selectedDate != null) {
            scheduleItems = filterByDate(scheduleItems, selectedDate);
        } else {
            scheduleItems = new ArrayList<>();
        }

        showTimeline(scheduleItems);
    }

    private void generateDateChips() {
        if (selectedTrip == null) {
            scrollDateChips.setVisibility(View.GONE);
            return;
        }

        layoutDateChips.removeAllViews();
        tripDates.clear();

        try {
            Date startDate = DB_DATE_FORMAT.parse(selectedTrip.getStartDate());
            Date endDate = DB_DATE_FORMAT.parse(selectedTrip.getEndDate());

            if (startDate == null || endDate == null) {
                scrollDateChips.setVisibility(View.GONE);
                return;
            }

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(startDate);
            java.util.Calendar endCal = java.util.Calendar.getInstance();
            endCal.setTime(endDate);

            SimpleDateFormat chipDateFormat = new SimpleDateFormat("EEE - MMM-dd", Locale.getDefault());

            while (!cal.after(endCal)) {
                String dateKey = DB_DATE_FORMAT.format(cal.getTime());
                String displayText = chipDateFormat.format(cal.getTime());
                tripDates.add(dateKey);

                TextView chip = createDateChip(displayText, dateKey);
                layoutDateChips.addView(chip);

                cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
            }

            scrollDateChips.setVisibility(View.VISIBLE);

            if (selectedDate == null && !tripDates.isEmpty()) {
                String currentDateKey = getCurrentDateKey();
                if (tripDates.contains(currentDateKey)) {
                    selectDate(currentDateKey);
                } else {
                    selectDate(tripDates.get(0));
                }
            } else if (selectedDate != null) {
                selectDate(selectedDate);
            }

        } catch (ParseException e) {
            e.printStackTrace();
            scrollDateChips.setVisibility(View.GONE);
        }
    }

    private TextView createDateChip(String displayText, String dateKey) {
        TextView chip = new TextView(requireContext());
        chip.setText(displayText);
        chip.setPadding(20, 12, 20, 12);
        chip.setTextSize(14);
        chip.setClickable(true);
        chip.setFocusable(true);

        chip.setBackgroundResource(R.drawable.chip_bg_white);
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(8);
        chip.setLayoutParams(params);

        chip.setOnClickListener(v -> selectDate(dateKey));

        return chip;
    }

    private void selectDate(String dateKey) {
        selectedDate = dateKey;

        for (int i = 0; i < layoutDateChips.getChildCount(); i++) {
            View child = layoutDateChips.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                String chipDateKey = tripDates.get(i);
                if (chipDateKey.equals(dateKey)) {
                    chip.setBackgroundResource(R.drawable.chip_bg);
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                } else {
                    chip.setBackgroundResource(R.drawable.chip_bg_white);
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                }
            }
        }

        loadSchedulesForSelectedTrip();
    }

    private String getCurrentDateKey() {
        return DB_DATE_FORMAT.format(new Date());
    }

    private List<ScheduleItem> filterByDate(List<ScheduleItem> items, String dateKey) {
        List<ScheduleItem> filtered = new ArrayList<>();
        for (ScheduleItem item : items) {
            String itemDay = normalizeDayKey(item.getDay());
            if (itemDay.equals(dateKey)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private void showEmptyState(String message) {
        tvEmptyStateText.setText(message);
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutTimelineContainer.setVisibility(View.GONE);
        if (selectedTrip == null) {
            scrollDateChips.setVisibility(View.GONE);
        }
    }

    private void showTimeline(List<ScheduleItem> items) {
        layoutEmptyState.setVisibility(View.GONE);
        layoutTimelineContainer.setVisibility(View.VISIBLE);
        layoutTimelineContainer.removeAllViews();

        int hourHeight = dpToPx(60);
        int timeWidth = dpToPx(50);
        
        for (int i = 0; i < 24; i++) {
            TextView timeLabel = new TextView(requireContext());
            timeLabel.setText(String.format(Locale.getDefault(), "%02d:00", i));
            timeLabel.setTextSize(12);
            timeLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            RelativeLayout.LayoutParams lpLabel = new RelativeLayout.LayoutParams(timeWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpLabel.topMargin = i * hourHeight;
            lpLabel.leftMargin = dpToPx(8);
            timeLabel.setLayoutParams(lpLabel);
            layoutTimelineContainer.addView(timeLabel);

            View line = new View(requireContext());
            line.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray));
            RelativeLayout.LayoutParams lpLine = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            lpLine.topMargin = i * hourHeight + dpToPx(10);
            lpLine.leftMargin = timeWidth + dpToPx(8);
            line.setLayoutParams(lpLine);
            layoutTimelineContainer.addView(line);
        }

        for (ScheduleItem item : items) {
            drawEvent(item, hourHeight, timeWidth);
        }
    }

    private void drawEvent(ScheduleItem item, int hourHeight, int timeWidth) {
        int startMinutes = parseTime(item.getStartTime());
        int endMinutes = parseTime(item.getEndTime());
        
        if (startMinutes == -1) return;
        if (endMinutes == -1 || endMinutes <= startMinutes) endMinutes = startMinutes + 60;

        int topMargin = (startMinutes * hourHeight) / 60;
        int height = ((endMinutes - startMinutes) * hourHeight) / 60;

        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(requireContext());
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary_gray));
        card.setRadius(dpToPx(8));
        card.setCardElevation(dpToPx(2));
        
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        lp.topMargin = topMargin + dpToPx(10);
        lp.leftMargin = timeWidth + dpToPx(16);
        lp.rightMargin = dpToPx(16);
        card.setLayoutParams(lp);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        
        TextView title = new TextView(requireContext());
        title.setText(item.getTitle());
        title.setTextSize(14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        content.addView(title);

        if (item.getNotes() != null && !item.getNotes().isEmpty()) {
            TextView notes = new TextView(requireContext());
            notes.setText(item.getNotes());
            notes.setTextSize(12);
            notes.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
            content.addView(notes);
        }

        card.addView(content);
        layoutTimelineContainer.addView(card);
    }

    private int parseTime(String timeStr) {
        if (timeStr == null) return -1;
        try {
            String[] parts = timeStr.split(":");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateTripSummary(@Nullable Trip trip) {
        if (trip == null) {
            tvTripSummary.setText(getString(R.string.schedule_no_trip_selected));
            tvTripDates.setVisibility(View.GONE);
            tvTripStatusHint.setVisibility(View.GONE);
            return;
        }

        // Set trip name/destination
        String name = !TextUtils.isEmpty(trip.getDestination()) ? trip.getDestination() : trip.getTripName();
        tvTripSummary.setText(name);

        // Calculate and format trip duration
        try {
            Date startDate = DB_DATE_FORMAT.parse(trip.getStartDate());
            Date endDate = DB_DATE_FORMAT.parse(trip.getEndDate());
            if (startDate != null && endDate != null) {
                long diff = endDate.getTime() - startDate.getTime();
                long days = (diff / (1000 * 60 * 60 * 24)) + 1; // +1 to include both start and end days

                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE - MMM dd", Locale.getDefault());
                String startDateStr = dateFormat.format(startDate);
                String durationText = startDateStr + " - " + days + " Days";
                tvTripDates.setText(durationText);
                tvTripDates.setVisibility(View.VISIBLE);
            } else {
                tvTripDates.setVisibility(View.GONE);
            }
        } catch (ParseException e) {
            tvTripDates.setVisibility(View.GONE);
        }

        // Set trip status
        if (isTripRunning(trip)) {
            tvTripStatusHint.setVisibility(View.VISIBLE);
            tvTripStatusHint.setText(R.string.schedule_trip_status_running);
        } else if (isTripUpcoming(trip)) {
            tvTripStatusHint.setVisibility(View.VISIBLE);
            tvTripStatusHint.setText(R.string.schedule_trip_status_upcoming);
        } else if (areTripDatesValid(trip)) {
            tvTripStatusHint.setVisibility(View.VISIBLE);
            tvTripStatusHint.setText(R.string.schedule_trip_status_past);
        } else {
            tvTripStatusHint.setVisibility(View.GONE);
        }
    }



    private long detectRunningTripId() {
        Date now = new Date();
        for (Trip trip : trips) {
            if (isTripRunning(trip, now)) {
                return trip.getTripId();
            }
        }
        return -1;
    }

    private boolean isTripRunning(Trip trip) {
        return isTripRunning(trip, new Date());
    }

    private boolean isTripRunning(Trip trip, Date reference) {
        try {
            Date startDate = DB_DATE_FORMAT.parse(trip.getStartDate());
            Date endDate = DB_DATE_FORMAT.parse(trip.getEndDate());
            if (startDate == null || endDate == null)
                return false;
            return !reference.before(startDate) && !reference.after(endDate);
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean areTripDatesValid(Trip trip) {
        try {
            return DB_DATE_FORMAT.parse(trip.getStartDate()) != null
                    && DB_DATE_FORMAT.parse(trip.getEndDate()) != null;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isTripUpcoming(Trip trip) {
        try {
            Date startDate = DB_DATE_FORMAT.parse(trip.getStartDate());
            if (startDate == null)
                return false;
            return new Date().before(startDate);
        } catch (ParseException e) {
            return false;
        }
    }

    private String normalizeDayKey(@Nullable String day) {
        if (day == null || day.trim().isEmpty()) {
            return FLEXIBLE_DAY_KEY;
        }
        return day.trim();
    }
}
