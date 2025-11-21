package com.example.voyagerbuds.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
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
    private TextView tvEmptyState;
    private RecyclerView recyclerView;
    private HorizontalScrollView scrollDateChips;
    private LinearLayout layoutDateChips;

    private ScheduleDayAdapter dayAdapter;
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
        tvEmptyState = view.findViewById(R.id.tv_schedule_empty_state);
        recyclerView = view.findViewById(R.id.recycler_view_schedule);
        scrollDateChips = view.findViewById(R.id.scroll_date_chips);
        layoutDateChips = view.findViewById(R.id.layout_date_chips);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dayAdapter = new ScheduleDayAdapter(requireContext(), new ArrayList<>(),
                new ScheduleAdapter.OnScheduleActionListener() {
                    @Override
                    public void onEdit(ScheduleItem item) {
                        // Edit handled in Trip Detail Fragment
                    }

                    @Override
                    public void onDelete(ScheduleItem item) {
                        // Delete handled in Trip Detail Fragment
                    }
                });
        recyclerView.setAdapter(dayAdapter);

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
            // The database expects a 'long' user ID, but Firebase provides a String UID.
            // As a temporary solution, we are using the hashcode of the UID.
            // This is NOT guaranteed to be unique and should be replaced with a proper user ID mapping system.
            int userId = currentUser.getUid().hashCode();
            List<Trip> userTrips = databaseHelper.getAllTrips(userId);
            if (userTrips != null) {
                trips.addAll(userTrips);
            }
        } else {
            // User is not logged in, do nothing.
            // The UI will show an empty state because the trips list is empty.
        }

        if (trips.isEmpty()) {
            selectedTrip = null;
            updateTripSummary(null);
            dayAdapter.updateGroups(new ArrayList<>());
            showEmptyState(getString(R.string.schedule_no_trips_prompt));
            return;
        }

        // Priority: 1. Running trip, 2. Trip from args (if valid), 3. Upcoming trip?
        // Requirement: "Always show... from the current trip if have"
        long runningTripId = detectRunningTripId();

        if (runningTripId != -1) {
            for (Trip trip : trips) {
                if (trip.getTripId() == runningTripId) {
                    selectedTrip = trip;
                    break;
                }
            }
        } else if (tripId > 0) {
            // Fallback to argument trip if no running trip?
            // Or strictly "current trip"?
            // "if there is no current trip, say that there is no current trip"
            // I will interpret "current trip" as "running trip".
            // But if user navigated from "Add Schedule" for a specific trip, maybe we
            // should show it?
            // The prompt says "Always show... from the current trip".
            // I will prioritize running trip. If no running trip, show empty state.
            selectedTrip = null;
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
            dayAdapter.updateGroups(new ArrayList<>());
            // "if there is no current trip, say that there is no current trip with icon"
            // I'll use a generic message for now, user can add icon in XML if needed or I
            // can add drawable here
            showEmptyState("No current trip happening now.");
            scrollDateChips.setVisibility(View.GONE);
        }
    }

    private void loadSchedulesForSelectedTrip() {
        if (selectedTrip == null) {
            showEmptyState(getString(R.string.schedule_no_trips_prompt));
            dayAdapter.updateGroups(new ArrayList<>());
            scrollDateChips.setVisibility(View.GONE);
            return;
        }

        // Generate date chips for trip dates (only if not already generated)
        if (tripDates.isEmpty()) {
            generateDateChips();
        }

        List<ScheduleItem> scheduleItems = databaseHelper.getSchedulesForTrip(selectedTrip.getTripId());
        if (scheduleItems.isEmpty()) {
            if (isTripRunning(selectedTrip)) {
                showEmptyState(getString(R.string.schedule_no_events_running, selectedTrip.getTripName()));
            } else {
                showEmptyState(getString(R.string.schedule_no_events_for_trip, selectedTrip.getTripName()));
            }
            dayAdapter.updateGroups(new ArrayList<>());
            scrollDateChips.setVisibility(View.GONE);
            return;
        }

        // Filter by selected date if one is selected
        if (selectedDate != null) {
            scheduleItems = filterByDate(scheduleItems, selectedDate);
        }

        List<ScheduleDayGroup> groups = groupSchedulesByDay(scheduleItems);
        dayAdapter.updateGroups(groups);
        dayAdapter.setSelectedDateKey(selectedDate);
        showScheduleList();
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

            // Auto-select current date if it's within trip range, otherwise select first
            // date
            if (selectedDate == null && !tripDates.isEmpty()) {
                String currentDateKey = getCurrentDateKey();
                if (tripDates.contains(currentDateKey)) {
                    selectDate(currentDateKey);
                } else {
                    selectDate(tripDates.get(0));
                }
            } else if (selectedDate != null) {
                // Re-select the previously selected date
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

        // Set initial style (unselected)
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

        // Update chip styles
        for (int i = 0; i < layoutDateChips.getChildCount(); i++) {
            View child = layoutDateChips.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                String chipDateKey = tripDates.get(i);
                if (chipDateKey.equals(dateKey)) {
                    // Selected style
                    chip.setBackgroundResource(R.drawable.chip_bg);
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                } else {
                    // Unselected style
                    chip.setBackgroundResource(R.drawable.chip_bg_white);
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                }
            }
        }

        // Reload schedules for selected date
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
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showScheduleList() {
        tvEmptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        // No RecyclerView fade-in animation, static UI loading
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

    private List<ScheduleDayGroup> groupSchedulesByDay(List<ScheduleItem> items) {
        List<ScheduleItem> sorted = new ArrayList<>(items);
        Collections.sort(sorted, new Comparator<ScheduleItem>() {
            @Override
            public int compare(ScheduleItem o1, ScheduleItem o2) {
                String day1 = normalizeDayKey(o1.getDay());
                String day2 = normalizeDayKey(o2.getDay());
                if (!day1.equals(day2)) {
                    if (FLEXIBLE_DAY_KEY.equals(day1))
                        return 1;
                    if (FLEXIBLE_DAY_KEY.equals(day2))
                        return -1;
                    return day1.compareTo(day2);
                }
                String time1 = safeValue(o1.getStartTime());
                String time2 = safeValue(o2.getStartTime());
                return time1.compareTo(time2);
            }
        });

        Map<String, ScheduleDayGroup> grouped = new LinkedHashMap<>();
        for (ScheduleItem item : sorted) {
            String key = normalizeDayKey(item.getDay());
            boolean flexible = FLEXIBLE_DAY_KEY.equals(key);
            String storedKey = flexible ? null : key;
            String mapKey = flexible ? FLEXIBLE_DAY_KEY : key;
            ScheduleDayGroup group = grouped.get(mapKey);
            if (group == null) {
                group = new ScheduleDayGroup(storedKey, flexible);
                grouped.put(mapKey, group);
            }
            group.addEvent(item);
        }

        return new ArrayList<>(grouped.values());
    }

    private String normalizeDayKey(@Nullable String day) {
        if (day == null || day.trim().isEmpty()) {
            return FLEXIBLE_DAY_KEY;
        }
        return day.trim();
    }

    private String safeValue(@Nullable String value) {
        return value == null ? "" : value;
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
}