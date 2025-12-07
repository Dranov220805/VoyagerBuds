package com.example.voyagerbuds.fragments;

import android.app.AlertDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.adapters.ScheduleAdapter;
import com.example.voyagerbuds.adapters.ScheduleDayAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.ScheduleDayGroup;
import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.models.Trip;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import com.example.voyagerbuds.utils.DateUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.voyagerbuds.adapters.ExpenseDateAdapter;
import java.util.Calendar;

public class ScheduleFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String FLEXIBLE_DAY_KEY = "__flexible__";
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    public static final String ARG_SCHEDULE_ID = "arg_schedule_id";

    private String mParam1;
    private String mParam2;
    private long tripId = -1;
    private int targetScheduleId = -1;

    private TextView tvScheduleTitle;
    private ImageView ivScheduleMenu;
    private Spinner spinnerTripSelector;
    private TextView tvEmptyState;
    private LinearLayout layoutEmptyState;
    private com.google.android.material.button.MaterialButton btnGoToTripDetail;
    private RecyclerView recyclerView;
    private RecyclerView rvDateChips;

    private ScheduleDayAdapter dayAdapter;
    private ExpenseDateAdapter dateAdapter;
    private DatabaseHelper databaseHelper;
    private final List<Trip> trips = new ArrayList<>();
    private Trip selectedTrip;
    private String selectedDate = null;
    private List<String> tripDates = new ArrayList<>();
    private List<Date> dateList = new ArrayList<>();

    private java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors
            .newSingleThreadExecutor();
    private android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

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

    public static ScheduleFragment newInstanceForSchedule(int scheduleId) {
        ScheduleFragment fragment = new ScheduleFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SCHEDULE_ID, scheduleId);
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
            if (getArguments().containsKey(ARG_SCHEDULE_ID)) {
                targetScheduleId = getArguments().getInt(ARG_SCHEDULE_ID, -1);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        databaseHelper = new DatabaseHelper(requireContext());

        tvScheduleTitle = view.findViewById(R.id.tv_schedule_title);
        // ivScheduleMenu = view.findViewById(R.id.iv_schedule_menu);
        spinnerTripSelector = view.findViewById(R.id.spinner_trip_selector);
        tvEmptyState = view.findViewById(R.id.tv_schedule_empty_state);
        layoutEmptyState = view.findViewById(R.id.layout_schedule_empty_state);
        btnGoToTripDetail = view.findViewById(R.id.btn_go_to_trip_detail);
        recyclerView = view.findViewById(R.id.recycler_view_schedule);
        rvDateChips = view.findViewById(R.id.rv_date_chips);

        btnGoToTripDetail.setOnClickListener(v -> {
            if (selectedTrip != null) {
                TripDetailFragment fragment = TripDetailFragment.newInstance(selectedTrip.getTripId());
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left,
                                R.anim.slide_in_left,
                                R.anim.slide_out_right)
                        .replace(R.id.content_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView
                .setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_slide_in));
        dayAdapter = new ScheduleDayAdapter(requireContext(), new ArrayList<>(),
                new ScheduleAdapter.OnScheduleActionListener() {
                    @Override
                    public void onItemClick(ScheduleItem item) {
                        showDetailDialog(item);
                    }

                    @Override
                    public void onItemLongClick(View view, ScheduleItem item) {
                        showItemMenu(view, item);
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
        int userId = com.example.voyagerbuds.utils.UserSessionManager.getCurrentUserId(requireContext());
        if (userId != -1) {
            trips.addAll(databaseHelper.getAllTrips(userId));
        }

        // Sort trips by start date in ascending order (earliest upcoming trips first)
        Collections.sort(trips, new Comparator<Trip>() {
            @Override
            public int compare(Trip t1, Trip t2) {
                try {
                    LocalDate d1 = DateUtils.parseDbDateToLocalDate(t1.getStartDate());
                    LocalDate d2 = DateUtils.parseDbDateToLocalDate(t2.getStartDate());
                    if (d1 == null || d2 == null)
                        return 0;
                    // Keep previous behavior: latest date first (descending)
                    return d2.compareTo(d1);
                } catch (Exception e) {
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

        // Priority: 1. Target Schedule Trip, 2. Running trip, 3. Trip from args (if
        // valid), 4. First trip
        long runningTripId = detectRunningTripId();

        if (targetScheduleId != -1) {
            ScheduleItem item = databaseHelper.getScheduleById(targetScheduleId);
            if (item != null) {
                for (Trip trip : trips) {
                    if (trip.getTripId() == item.getTripId()) {
                        selectedTrip = trip;
                        break;
                    }
                }
            }
        }

        if (selectedTrip == null && runningTripId != -1) {
            for (Trip trip : trips) {
                if (trip.getTripId() == runningTripId) {
                    selectedTrip = trip;
                    break;
                }
            }
        } else if (selectedTrip == null && tripId > 0) {
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
            loadSchedulesForSelectedTrip(true);
        } else {
            dayAdapter.updateGroups(new ArrayList<>());
            showEmptyState("No current trip happening now.");
            rvDateChips.setVisibility(View.GONE);
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
                        loadSchedulesForSelectedTrip(true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void loadSchedulesForSelectedTrip(boolean animateLayout) {
        if (selectedTrip == null) {
            showEmptyState(getString(R.string.schedule_no_trips_prompt));
            dayAdapter.updateGroups(new ArrayList<>());
            rvDateChips.setVisibility(View.GONE);
            return;
        }

        // Handle target schedule ID from notification
        if (targetScheduleId != -1) {
            ScheduleItem item = databaseHelper.getScheduleById(targetScheduleId);
            if (item != null && item.getTripId() == selectedTrip.getTripId()) {
                // Set selected date to the item's date so it's visible
                if (item.getDay() != null && !item.getDay().isEmpty()) {
                    selectedDate = item.getDay();
                }
                // Show detail dialog
                showDetailDialog(item);
                targetScheduleId = -1; // Consume the event
            }
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
            rvDateChips.setVisibility(View.GONE);
            return;
        }

        // Filter by selected date if one is selected
        if (selectedDate != null) {
            scheduleItems = filterByDate(scheduleItems, selectedDate);
            if (scheduleItems.isEmpty()) {
                showEmptyState(getString(R.string.schedule_no_events_today));
                dayAdapter.updateGroups(new ArrayList<>());
                return;
            }
        }

        List<ScheduleDayGroup> groups = groupSchedulesByDay(scheduleItems);
        dayAdapter.updateGroups(groups);
        if (animateLayout) {
            recyclerView.scheduleLayoutAnimation();
        }
        dayAdapter.setSelectedDateKey(selectedDate);
        showScheduleList();
    }

    private void generateDateChips() {
        if (selectedTrip == null) {
            rvDateChips.setVisibility(View.GONE);
            return;
        }

        tripDates.clear();
        dateList.clear();

        try {
            LocalDate startDate = DateUtils.parseDbDateToLocalDate(selectedTrip.getStartDate());
            LocalDate endDate = DateUtils.parseDbDateToLocalDate(selectedTrip.getEndDate());

            if (startDate == null || endDate == null) {
                rvDateChips.setVisibility(View.GONE);
                return;
            }
            LocalDate cur = startDate;
            while (!cur.isAfter(endDate)) {
                String dateKey = DateUtils.formatLocalDateToDbKey(cur);
                tripDates.add(dateKey);

                java.util.Date displayDate = java.util.Date.from(cur.atStartOfDay(ZoneId.systemDefault()).toInstant());
                dateList.add(displayDate);

                cur = cur.plusDays(1);
            }

            rvDateChips.setVisibility(View.VISIBLE);
            rvDateChips.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

            dateAdapter = new ExpenseDateAdapter(getContext(), dateList, (date, position) -> {
                String dateKey = tripDates.get(position);
                selectDate(dateKey);
            });
            rvDateChips.setAdapter(dateAdapter);

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

        } catch (Exception e) {
            e.printStackTrace();
            rvDateChips.setVisibility(View.GONE);
        }
    }

    private void selectDate(String dateKey) {
        String previousSelected = selectedDate;
        selectedDate = dateKey;

        // Update adapter selection
        if (dateAdapter != null && tripDates.contains(dateKey)) {
            int index = tripDates.indexOf(dateKey);
            dateAdapter.setSelectedPosition(index);
            rvDateChips.smoothScrollToPosition(index);
        }

        // Determine direction (kept for reference or future use, but not used for fade)
        // boolean movingRight = true;
        // if (previousSelected != null && dateKey != null) {
        // movingRight = dateKey.compareTo(previousSelected) > 0;
        // }

        // Animate list change with Fade
        recyclerView.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    loadSchedulesForSelectedTrip(false);
                    recyclerView.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private String getCurrentDateKey() {
        return DateUtils.getTodayKey();
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
        layoutEmptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Show button only if we have a selected trip
        if (selectedTrip != null) {
            btnGoToTripDetail.setVisibility(View.VISIBLE);
        } else {
            btnGoToTripDetail.setVisibility(View.GONE);
        }
    }

    private void showScheduleList() {
        layoutEmptyState.setVisibility(View.GONE);
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
        LocalDate today = DateUtils.todayLocalDate();
        for (Trip trip : trips) {
            if (isTripRunning(trip, today)) {
                return trip.getTripId();
            }
        }
        return -1;
    }

    private boolean isTripRunning(Trip trip) {
        return isTripRunning(trip, DateUtils.todayLocalDate());
    }

    private boolean isTripRunning(Trip trip, LocalDate reference) {
        try {
            LocalDate start = DateUtils.parseDbDateToLocalDate(trip.getStartDate());
            LocalDate end = DateUtils.parseDbDateToLocalDate(trip.getEndDate());
            if (start == null || end == null || reference == null)
                return false;
            return !reference.isBefore(start) && !reference.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean areTripDatesValid(Trip trip) {
        try {
            return DateUtils.parseDbDateToLocalDate(trip.getStartDate()) != null
                    && DateUtils.parseDbDateToLocalDate(trip.getEndDate()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTripUpcoming(Trip trip) {
        try {
            LocalDate start = DateUtils.parseDbDateToLocalDate(trip.getStartDate());
            if (start == null)
                return false;
            return DateUtils.todayLocalDate().isBefore(start);
        } catch (Exception e) {
            return false;
        }
    }

    private void showDetailDialog(ScheduleItem item) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_schedule_detail, null);
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        // bottomSheetDialog.getBehavior().setDraggable(false); // Removed to fix swipe
        // issue

        View dragHandle = dialogView.findViewById(R.id.layout_drag_handle);
        /*
         * Removed restrictive touch listener
         * if (dragHandle != null) {
         * dragHandle.setOnTouchListener((v, event) -> {
         * switch (event.getAction()) {
         * case android.view.MotionEvent.ACTION_DOWN:
         * bottomSheetDialog.getBehavior().setDraggable(true);
         * break;
         * case android.view.MotionEvent.ACTION_UP:
         * case android.view.MotionEvent.ACTION_CANCEL:
         * v.post(() -> bottomSheetDialog.getBehavior().setDraggable(false));
         * break;
         * }
         * return false;
         * });
         * }
         */

        EditText etTitle = dialogView.findViewById(R.id.et_detail_title);
        EditText etTime = dialogView.findViewById(R.id.et_detail_time);
        EditText etLocation = dialogView.findViewById(R.id.et_detail_location);
        TextInputLayout layoutLocation = dialogView.findViewById(R.id.layout_detail_location_input);
        EditText etNotes = dialogView.findViewById(R.id.et_detail_notes);
        TextInputLayout layoutNotes = dialogView.findViewById(R.id.layout_detail_notes_input);
        RecyclerView rvImages = dialogView.findViewById(R.id.rv_detail_images);
        View btnDelete = dialogView.findViewById(R.id.btn_detail_delete);
        View btnEdit = dialogView.findViewById(R.id.btn_detail_edit);
        View btnClose = dialogView.findViewById(R.id.btn_detail_close);
        View btnCloseSheet = dialogView.findViewById(R.id.btn_close_sheet);

        etTitle.setText(item.getTitle());

        String timeText = item.getDay();
        if (item.getStartTime() != null && !item.getStartTime().isEmpty()) {
            timeText += " • " + item.getStartTime();
            if (item.getEndTime() != null && !item.getEndTime().isEmpty()) {
                timeText += " - " + item.getEndTime();
            }
        }
        etTime.setText(timeText);

        if (item.getLocation() != null && !item.getLocation().isEmpty()) {
            etLocation.setText(item.getLocation());
            layoutLocation.setVisibility(View.VISIBLE);
            etLocation.setOnClickListener(v -> {
                bottomSheetDialog.dismiss(); // Auto close the drawer
                navigateToMapWithPin(item);
            });
        } else {
            layoutLocation.setVisibility(View.GONE);
        }

        if (item.getNotes() != null && !item.getNotes().isEmpty()) {
            etNotes.setText(item.getNotes());
            layoutNotes.setVisibility(View.VISIBLE);
        } else {
            layoutNotes.setVisibility(View.GONE);
        }

        /*
         * if (item.getExpenseAmount() > 0) {
         * String currency = item.getExpenseCurrency() != null ?
         * item.getExpenseCurrency() : "USD";
         * etExpense.setText(String.format(Locale.getDefault(), "%.2f %s",
         * item.getExpenseAmount(), currency));
         * layoutExpense.setVisibility(View.VISIBLE);
         * } else {
         * layoutExpense.setVisibility(View.GONE);
         * }
         */

        List<String> imagePathList = new ArrayList<>();
        String paths = item.getImagePaths();
        if (paths != null && !paths.isEmpty()) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(paths);
                for (int i = 0; i < jsonArray.length(); i++) {
                    imagePathList.add(jsonArray.getString(i));
                }
            } catch (Exception e) {
                // Try comma separated
                String[] parts = paths.split(",");
                for (String part : parts) {
                    if (!part.trim().isEmpty()) {
                        imagePathList.add(part.trim());
                    }
                }
            }
        }

        if (!imagePathList.isEmpty()) {
            rvImages.setVisibility(View.VISIBLE);
            rvImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            com.example.voyagerbuds.adapters.ScheduleImageAdapter imageAdapter = new com.example.voyagerbuds.adapters.ScheduleImageAdapter(
                    requireContext(), imagePathList, false, null);
            imageAdapter.setOnImageClickListener(this::showFullImageDialog);
            rvImages.setAdapter(imageAdapter);
        } else {
            rvImages.setVisibility(View.GONE);
        }

        // When this dialog is shown from the Schedule fragment, we do not want to allow
        // editing or deleting directly from here. Allow these actions only from Trip
        // Detail screen to prevent accidental modification from the schedule list.
        if (btnDelete != null) {
            btnDelete.setVisibility(View.GONE);
        }
        if (btnEdit != null) {
            btnEdit.setVisibility(View.GONE);
        }

        btnDelete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            deleteSchedule(item);
        });

        btnEdit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            // For now, we can't easily edit from here without duplicating the Add/Edit
            // dialog logic
            // or navigating to TripDetailFragment.
            // Let's show a toast or try to navigate.
            Toast.makeText(getContext(), R.string.edit_from_trip_detail_screen, Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnCloseSheet != null) {
            btnCloseSheet.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();
    }

    private void showItemMenu(View view, ScheduleItem item) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenu().add(0, 1, 0, R.string.edit);
        popup.getMenu().add(0, 2, 0, R.string.delete);

        popup.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == 1) {
                // Edit
                Toast.makeText(getContext(), R.string.edit_from_trip_detail_screen, Toast.LENGTH_SHORT).show();
                return true;
            } else if (menuItem.getItemId() == 2) {
                deleteSchedule(item);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void deleteSchedule(ScheduleItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.schedule_delete_title)
                .setMessage(R.string.schedule_delete_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    databaseHelper.deleteSchedule(item.getId());
                    Toast.makeText(getContext(), R.string.schedule_deleted, Toast.LENGTH_SHORT).show();
                    loadSchedulesForSelectedTrip(true);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void navigateToMapWithPin(ScheduleItem item) {
        String locationName = item.getLocation();
        String title = item.getTitle();

        // Check if we have stored coordinates first
        if (item.getLatitude() != null && item.getLongitude() != null) {
            // Use stored coordinates directly
            Bundle args = new Bundle();
            args.putDouble("pin_lat", item.getLatitude());
            args.putDouble("pin_lng", item.getLongitude());
            args.putString("pin_title", title);
            args.putString("pin_snippet", locationName);

            String time = item.getDay();
            if (item.getStartTime() != null && !item.getStartTime().isEmpty()) {
                time += " • " + item.getStartTime();
                if (item.getEndTime() != null && !item.getEndTime().isEmpty()) {
                    time += " - " + item.getEndTime();
                }
            }
            args.putString("pin_time", time);

            MapFragment mapFragment = new MapFragment();
            mapFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.content_container, mapFragment)
                    .addToBackStack(null)
                    .commit();
            return;
        }

        // Fallback: try to geocode the location name if no coordinates stored
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double lat = address.getLatitude();
                double lng = address.getLongitude();

                MapFragment mapFragment = new MapFragment();
                Bundle args = new Bundle();
                args.putDouble("pin_lat", lat);
                args.putDouble("pin_lng", lng);
                args.putString("pin_title", title);

                // Add extra details
                String snippet = item.getNotes();
                if (snippet == null || snippet.isEmpty()) {
                    snippet = "Event location";
                }
                args.putString("pin_snippet", snippet);

                String time = item.getDay();
                if (item.getStartTime() != null && !item.getStartTime().isEmpty()) {
                    time += " • " + item.getStartTime();
                    if (item.getEndTime() != null && !item.getEndTime().isEmpty()) {
                        time += " - " + item.getEndTime();
                    }
                }
                args.putString("pin_time", time);

                mapFragment.setArguments(args);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.content_container, mapFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), R.string.location_unavailable, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), R.string.geocoding_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void showFullImageDialog(String imagePath) {
        android.app.Dialog fullImageDialog = new android.app.Dialog(requireContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        fullImageDialog.setContentView(R.layout.dialog_full_image);

        com.example.voyagerbuds.views.ZoomableImageView ivFullImage = fullImageDialog.findViewById(R.id.iv_full_image);
        android.widget.ImageButton btnClose = fullImageDialog.findViewById(R.id.btn_close_full_image);
        android.widget.ImageButton btnRotate = fullImageDialog.findViewById(R.id.btn_rotate_full_image);
        android.view.View loadingContainer = fullImageDialog.findViewById(R.id.loading_container);

        loadingContainer.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            android.graphics.Bitmap bitmap = null;
            try {
                android.net.Uri uri;
                if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                    uri = android.net.Uri.parse(imagePath);
                } else {
                    uri = android.net.Uri.fromFile(new java.io.File(imagePath));
                }

                android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                int reqWidth = metrics.widthPixels;
                int reqHeight = metrics.heightPixels;

                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                java.io.InputStream input = requireContext().getContentResolver().openInputStream(uri);
                android.graphics.BitmapFactory.decodeStream(input, null, options);
                if (input != null)
                    input.close();

                options.inSampleSize = com.example.voyagerbuds.utils.ImageUtils.calculateInSampleSize(options, reqWidth,
                        reqHeight);
                options.inJustDecodeBounds = false;

                input = requireContext().getContentResolver().openInputStream(uri);
                bitmap = android.graphics.BitmapFactory.decodeStream(input, null, options);
                if (input != null)
                    input.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            final android.graphics.Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                loadingContainer.setVisibility(View.GONE);
                if (finalBitmap != null) {
                    ivFullImage.setImageBitmap(finalBitmap);
                } else {
                    ivFullImage.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            });
        });

        btnClose.setOnClickListener(v -> fullImageDialog.dismiss());
        btnRotate.setOnClickListener(v -> ivFullImage.rotate());

        fullImageDialog.show();
    }
}
