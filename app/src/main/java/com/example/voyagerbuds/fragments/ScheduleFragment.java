package com.example.voyagerbuds.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

    private Spinner tripPicker;
    private TextView tvTripSummary;
    private TextView tvTripDates;
    private TextView tvTripStatusHint;
    private TextView tvEmptyState;
    private Button btnAdd;
    private RecyclerView recyclerView;
    private HorizontalScrollView scrollDateChips;
    private LinearLayout layoutDateChips;

    private ScheduleDayAdapter dayAdapter;
    private DatabaseHelper databaseHelper;
    private final List<Trip> trips = new ArrayList<>();
    private Trip selectedTrip;
    private boolean suppressTripSelection = false;
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

        tripPicker = view.findViewById(R.id.spinner_trip_picker);
        tvTripSummary = view.findViewById(R.id.tv_trip_summary);
        tvTripDates = view.findViewById(R.id.tv_trip_dates);
        tvTripStatusHint = view.findViewById(R.id.tv_trip_status_hint);
        tvEmptyState = view.findViewById(R.id.tv_schedule_empty_state);
        btnAdd = view.findViewById(R.id.btn_add_schedule_item);
        recyclerView = view.findViewById(R.id.recycler_view_schedule);
        scrollDateChips = view.findViewById(R.id.scroll_date_chips);
        layoutDateChips = view.findViewById(R.id.layout_date_chips);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dayAdapter = new ScheduleDayAdapter(requireContext(), new ArrayList<>(),
                new ScheduleAdapter.OnScheduleActionListener() {
                    @Override
                    public void onEdit(ScheduleItem item) {
                        showAddEditDialog(item);
                    }

                    @Override
                    public void onDelete(ScheduleItem item) {
                        databaseHelper.deleteSchedule(item.getId());
                        loadSchedulesForSelectedTrip();
                        Toast.makeText(getContext(), R.string.schedule_deleted, Toast.LENGTH_SHORT).show();
                    }
                });
        recyclerView.setAdapter(dayAdapter);

        // Fade-in animation for Schedule fragment root view
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(400).start();

        tripPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressTripSelection)
                    return;
                applyTripSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        btnAdd.setOnClickListener(v -> {
            if (selectedTrip == null) {
                Toast.makeText(getContext(), R.string.schedule_no_trip_selected, Toast.LENGTH_SHORT).show();
            } else {
                showAddEditDialog(null);
            }
        });

        loadTrips();

        return view;
    }

    private void loadTrips() {
        long previouslySelectedId = selectedTrip != null ? selectedTrip.getTripId() : -1;
        trips.clear();
        // TODO replace with logged-in user id when authentication is ready
        trips.addAll(databaseHelper.getAllTrips(1));

        List<String> labels = new ArrayList<>();
        for (Trip trip : trips) {
            labels.add(formatTripLabel(trip));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tripPicker.setAdapter(adapter);
        tripPicker.setEnabled(!trips.isEmpty());

        if (trips.isEmpty()) {
            selectedTrip = null;
            updateTripSummary(null);
            dayAdapter.updateGroups(new ArrayList<>());
            showEmptyState(getString(R.string.schedule_no_trips_prompt));
            toggleAddButtonState(false);
            return;
        }

        int targetIndex = findTripIndexById(previouslySelectedId);
        if (targetIndex < 0) {
            // Priority: 1. Running trip, 2. Trip from args, 3. First trip
            long runningTripId = detectRunningTripId();
            if (runningTripId != -1) {
                targetIndex = findTripIndexById(runningTripId);
            } else if (tripId > 0) {
                targetIndex = findTripIndexById(tripId);
            }
        }
        if (targetIndex < 0) {
            targetIndex = 0;
        }
        selectTripAtIndex(targetIndex);
    }

    private void selectTripAtIndex(int index) {
        if (index < 0 || index >= trips.size()) {
            selectedTrip = null;
            updateTripSummary(null);
            showEmptyState(getString(R.string.schedule_no_current_trip));
            dayAdapter.updateGroups(new ArrayList<>());
            toggleAddButtonState(false);
            return;
        }
        suppressTripSelection = true;
        tripPicker.setSelection(index, false);
        suppressTripSelection = false;
        applyTripSelection(index);
    }

    private void applyTripSelection(int position) {
        if (position < 0 || position >= trips.size()) {
            return;
        }
        selectedTrip = trips.get(position);
        tripId = selectedTrip.getTripId();
        selectedDate = null; // Reset selected date when trip changes
        tripDates.clear(); // Clear trip dates to regenerate
        updateTripSummary(selectedTrip);
        toggleAddButtonState(true);
        loadSchedulesForSelectedTrip();
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

    private void toggleAddButtonState(boolean enabled) {
        btnAdd.setEnabled(enabled);
        btnAdd.setAlpha(enabled ? 1f : 0.5f);
    }

    private String formatTripLabel(Trip trip) {
        String destination = !TextUtils.isEmpty(trip.getDestination()) ? trip.getDestination() : trip.getTripName();
        String dates = formatTripDates(trip.getStartDate(), trip.getEndDate());
        return destination + " • " + dates;
    }

    private String formatTripDates(String start, String end) {
        try {
            Date startDate = DB_DATE_FORMAT.parse(start);
            Date endDate = DB_DATE_FORMAT.parse(end);
            if (startDate != null && endDate != null) {
                return DISPLAY_DATE_FORMAT.format(startDate) + " - " + DISPLAY_DATE_FORMAT.format(endDate);
            }
        } catch (Exception ignored) {
        }
        return (start != null ? start : "?") + " - " + (end != null ? end : "?");
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

    private int findTripIndexById(long id) {
        if (id <= 0)
            return -1;
        for (int i = 0; i < trips.size(); i++) {
            if (trips.get(i).getTripId() == id) {
                return i;
            }
        }
        return -1;
    }

    private void showAddEditDialog(@Nullable ScheduleItem editing) {
        if (selectedTrip == null && editing == null) {
            Toast.makeText(getContext(), R.string.schedule_no_trip_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_schedule, null);
        EditText etDay = dialogView.findViewById(R.id.et_schedule_day);
        EditText etStartTime = dialogView.findViewById(R.id.et_schedule_start_time);
        EditText etEndTime = dialogView.findViewById(R.id.et_schedule_end_time);
        EditText etTitle = dialogView.findViewById(R.id.et_schedule_title);
        EditText etNotes = dialogView.findViewById(R.id.et_schedule_notes);

        if (editing != null) {
            etDay.setText(editing.getDay());
            etStartTime.setText(editing.getStartTime());
            etEndTime.setText(editing.getEndTime());
            etTitle.setText(editing.getTitle());
            etNotes.setText(editing.getNotes());
            builder.setTitle(R.string.schedule_edit_title);
        } else {
            builder.setTitle(R.string.schedule_add_event);
        }

        etDay.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int year = cal.get(java.util.Calendar.YEAR);
            int month = cal.get(java.util.Calendar.MONTH);
            int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
            DatePickerDialog dp = new DatePickerDialog(requireContext(), (view1, y, m, d) -> {
                java.util.Calendar picked = java.util.Calendar.getInstance();
                picked.set(y, m, d);
                etDay.setText(DB_DATE_FORMAT.format(picked.getTime()));
            }, year, month, day);
            dp.show();
        });

        etStartTime.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            int minute = cal.get(java.util.Calendar.MINUTE);
            TimePickerDialog tp = new TimePickerDialog(requireContext(),
                    (view12, h, m) -> etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                    hour, minute, true);
            tp.show();
        });

        etEndTime.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            int minute = cal.get(java.util.Calendar.MINUTE);
            TimePickerDialog tp = new TimePickerDialog(requireContext(),
                    (view13, h, m) -> etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                    hour, minute, true);
            tp.show();
        });

        builder.setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String day = etDay.getText().toString().trim();
                    String start = etStartTime.getText().toString().trim();
                    String end = etEndTime.getText().toString().trim();
                    String title = etTitle.getText().toString().trim();
                    String notes = etNotes.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(getContext(), R.string.schedule_title_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (editing == null) {
                        ScheduleItem newItem = new ScheduleItem();
                        newItem.setTripId(selectedTrip != null ? selectedTrip.getTripId() : (int) tripId);
                        newItem.setDay(day);
                        newItem.setStartTime(start);
                        newItem.setEndTime(end);
                        newItem.setTitle(title);
                        newItem.setNotes(notes);
                        newItem.setCreatedAt(System.currentTimeMillis());
                        newItem.setUpdatedAt(System.currentTimeMillis());
                        databaseHelper.addSchedule(newItem);
                        Toast.makeText(getContext(), R.string.schedule_added, Toast.LENGTH_SHORT).show();
                    } else {
                        editing.setDay(day);
                        editing.setStartTime(start);
                        editing.setEndTime(end);
                        editing.setTitle(title);
                        editing.setNotes(notes);
                        editing.setUpdatedAt(System.currentTimeMillis());
                        databaseHelper.updateSchedule(editing);
                        Toast.makeText(getContext(), R.string.schedule_updated, Toast.LENGTH_SHORT).show();
                    }

                    loadSchedulesForSelectedTrip();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}