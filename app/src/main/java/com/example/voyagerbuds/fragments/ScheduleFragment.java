package com.example.voyagerbuds.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import com.example.voyagerbuds.utils.DateUtils;
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

    private String mParam1;
    private String mParam2;
    private long tripId = -1;

    private TextView tvScheduleTitle;
    private ImageView ivScheduleMenu;
    private Spinner spinnerTripSelector;
    private TextView tvEmptyState;
    private RecyclerView recyclerView;
    private HorizontalScrollView scrollDateChips;
    private LinearLayout layoutDateChips;

    private ScheduleDayAdapter dayAdapter;
    private DatabaseHelper databaseHelper;
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

        tvScheduleTitle = view.findViewById(R.id.tv_schedule_title);
        ivScheduleMenu = view.findViewById(R.id.iv_schedule_menu);
        spinnerTripSelector = view.findViewById(R.id.spinner_trip_selector);
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
        // TODO replace with logged-in user id when authentication is ready
        trips.addAll(databaseHelper.getAllTrips(1));

        // Sort trips by start date in ascending order (earliest upcoming trips first)
        Collections.sort(trips, new Comparator<Trip>() {
            @Override
            public int compare(Trip t1, Trip t2) {
                try {
                    Date date1 = DB_DATE_FORMAT.parse(t1.getStartDate());
                    Date date2 = DB_DATE_FORMAT.parse(t2.getStartDate());
                    if (date1 == null || date2 == null)
                        return 0;
                    return date2.compareTo(date1); // Descending order (latest date first)
                } catch (ParseException e) {
                    return 0;
                }
            }
        });

        if (trips.isEmpty()) {
            selectedTrip = null;
            setupTripSpinner();
            dayAdapter.updateGroups(new ArrayList<>());
            showEmptyState(getString(R.string.schedule_no_trips_prompt));
            return;
        }

        // Priority: 1. Running trip, 2. Trip from args (if valid), 3. First trip
        long runningTripId = detectRunningTripId();

        if (runningTripId != -1) {
            for (Trip trip : trips) {
                if (trip.getTripId() == runningTripId) {
                    selectedTrip = trip;
                    break;
                }
            }
        } else if (tripId > 0) {
            for (Trip trip : trips) {
                if (trip.getTripId() == tripId) {
                    selectedTrip = trip;
                    break;
                }
            }
        }

        if (selectedTrip == null && !trips.isEmpty()) {
            selectedTrip = trips.get(0);
        }

        setupTripSpinner();

        if (selectedTrip != null) {
            tripId = selectedTrip.getTripId();
            selectedDate = null;
            tripDates.clear();
            loadSchedulesForSelectedTrip();
        } else {
            dayAdapter.updateGroups(new ArrayList<>());
            showEmptyState("No current trip happening now.");
            scrollDateChips.setVisibility(View.GONE);
        }
    }

    private void setupTripSpinner() {
        if (trips.isEmpty()) {
            spinnerTripSelector.setVisibility(View.GONE);
            return;
        }

        spinnerTripSelector.setVisibility(View.VISIBLE);

        List<String> tripNames = new ArrayList<>();
        int selectedPosition = 0;
        for (int i = 0; i < trips.size(); i++) {
            Trip trip = trips.get(i);
            String displayName = !TextUtils.isEmpty(trip.getDestination())
                    ? trip.getDestination()
                    : trip.getTripName();
            tripNames.add(displayName);
            if (selectedTrip != null && trip.getTripId() == selectedTrip.getTripId()) {
                selectedPosition = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, tripNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTripSelector.setAdapter(adapter);
        spinnerTripSelector.setSelection(selectedPosition);

        spinnerTripSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < trips.size()) {
                    Trip newSelectedTrip = trips.get(position);
                    if (selectedTrip == null || selectedTrip.getTripId() != newSelectedTrip.getTripId()) {
                        selectedTrip = newSelectedTrip;
                        tripId = selectedTrip.getTripId();
                        selectedDate = null;
                        tripDates.clear();
                        loadSchedulesForSelectedTrip();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
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

            while (!cal.after(endCal)) {
                String dateKey = DB_DATE_FORMAT.format(cal.getTime());
                tripDates.add(dateKey);

                LinearLayout chip = createCompactDateChip(cal.getTime(), dateKey);
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

    private LinearLayout createCompactDateChip(Date date, String dateKey) {
        LinearLayout chipContainer = new LinearLayout(requireContext());
        chipContainer.setOrientation(LinearLayout.VERTICAL);
        chipContainer.setPadding(24, 16, 24, 16);
        chipContainer.setClickable(true);
        chipContainer.setFocusable(true);
        chipContainer.setGravity(android.view.Gravity.CENTER);

        // Get localized weekday abbreviation from strings
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
        String weekdayKey;
        switch (dayOfWeek) {
            case java.util.Calendar.MONDAY:
                weekdayKey = "weekday_mon_short";
                break;
            case java.util.Calendar.TUESDAY:
                weekdayKey = "weekday_tue_short";
                break;
            case java.util.Calendar.WEDNESDAY:
                weekdayKey = "weekday_wed_short";
                break;
            case java.util.Calendar.THURSDAY:
                weekdayKey = "weekday_thu_short";
                break;
            case java.util.Calendar.FRIDAY:
                weekdayKey = "weekday_fri_short";
                break;
            case java.util.Calendar.SATURDAY:
                weekdayKey = "weekday_sat_short";
                break;
            case java.util.Calendar.SUNDAY:
                weekdayKey = "weekday_sun_short";
                break;
            default:
                weekdayKey = "weekday_mon_short";
        }

        int resId = getResources().getIdentifier(weekdayKey, "string", requireContext().getPackageName());
        String weekdayText = resId != 0 ? getString(resId) : "";

        TextView weekdayView = new TextView(requireContext());
        weekdayView.setText(weekdayText);
        weekdayView.setTextSize(12);
        weekdayView.setGravity(android.view.Gravity.CENTER);

        TextView dayNumberView = new TextView(requireContext());
        dayNumberView.setText(String.valueOf(cal.get(java.util.Calendar.DAY_OF_MONTH)));
        dayNumberView.setTextSize(18);
        dayNumberView.setTypeface(null, android.graphics.Typeface.BOLD);
        dayNumberView.setGravity(android.view.Gravity.CENTER);

        chipContainer.addView(weekdayView);
        chipContainer.addView(dayNumberView);

        // Set initial style (unselected)
        chipContainer.setBackgroundResource(R.drawable.chip_bg_white);
        weekdayView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
        dayNumberView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(12);
        chipContainer.setLayoutParams(params);

        chipContainer.setOnClickListener(v -> selectDate(dateKey));

        return chipContainer;
    }

    private void selectDate(String dateKey) {
        selectedDate = dateKey;

        // Update chip styles
        for (int i = 0; i < layoutDateChips.getChildCount(); i++) {
            View child = layoutDateChips.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout chipContainer = (LinearLayout) child;
                String chipDateKey = tripDates.get(i);

                TextView weekdayView = (TextView) chipContainer.getChildAt(0);
                TextView dayNumberView = (TextView) chipContainer.getChildAt(1);

                if (chipDateKey.equals(dateKey)) {
                    // Selected style - teal background
                    chipContainer.setBackgroundResource(R.drawable.chip_bg_selected);
                    weekdayView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                    dayNumberView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                } else {
                    // Unselected style - white background
                    chipContainer.setBackgroundResource(R.drawable.chip_bg_white);
                    weekdayView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
                    dayNumberView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
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
