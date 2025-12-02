package com.example.voyagerbuds.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.adapters.AlbumSectionAdapter;
import com.example.voyagerbuds.adapters.TripSelectionAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.AlbumDay;
import com.example.voyagerbuds.models.AlbumSection;
import com.example.voyagerbuds.models.Capture;
import com.example.voyagerbuds.models.Trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AlbumFragment - Displays captures organized by trip, day, and time
 * Handles UTC timezone conversion for proper date sorting
 * Uses background threading for performance optimization
 */
public class AlbumFragment extends Fragment {

    private RecyclerView recyclerViewAlbum;
    private LinearLayout emptyState;
    private ProgressBar loadingIndicator;
    private TextView tvSelectedTrip;
    private ImageButton btnSelectTrip;

    private DatabaseHelper databaseHelper;
    private AlbumSectionAdapter adapter;
    private List<AlbumSection> albumSections;
    private List<Trip> allTrips;
    private Trip selectedTrip;

    private int currentUserId;

    // Background thread executor
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isLoading = false;

    public AlbumFragment() {
        // Required empty public constructor
    }

    public static AlbumFragment newInstance() {
        return new AlbumFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(getContext());
        albumSections = new ArrayList<>();

        // Get actual logged-in user ID
        currentUserId = com.example.voyagerbuds.utils.UserSessionManager.getCurrentUserId(getContext());
        android.util.Log.d("AlbumFragment", "Current user ID: " + currentUserId);

        // Initialize executor and handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);

        // Initialize views
        recyclerViewAlbum = view.findViewById(R.id.recycler_view_album);
        emptyState = view.findViewById(R.id.empty_state);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        tvSelectedTrip = view.findViewById(R.id.tv_selected_trip);
        btnSelectTrip = view.findViewById(R.id.btn_select_trip);

        // Initialize trips list
        allTrips = new ArrayList<>();

        // Setup trip selector
        btnSelectTrip.setOnClickListener(v -> showTripSelectionDialog());

        // Setup RecyclerView with optimizations
        adapter = new AlbumSectionAdapter(getContext(), albumSections,
                (captureId, mediaPath) -> {
                    // Handle capture click - show full screen preview
                    showCapturePreview(captureId, mediaPath);
                });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerViewAlbum.setLayoutManager(layoutManager);
        recyclerViewAlbum.setAdapter(adapter);
        recyclerViewAlbum.setHasFixedSize(true);
        recyclerViewAlbum.setItemViewCacheSize(20);
        recyclerViewAlbum.setDrawingCacheEnabled(true);
        recyclerViewAlbum.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // Load data asynchronously
        loadAlbumData();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shutdown executor to prevent memory leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAlbumData();
    }

    /**
     * Public method to refresh album data (called when new captures are added)
     */
    public void refreshAlbum() {
        android.util.Log.d("AlbumFragment", "refreshAlbum() called");
        loadAlbumData();
    }

    /**
     * Load all trips and select current trip by default
     */
    private void loadAlbumData() {
        if (isLoading) {
            android.util.Log.d("AlbumFragment", "Already loading, skipping...");
            return;
        }

        isLoading = true;
        android.util.Log.d("AlbumFragment", "Loading trips asynchronously...");

        // Show loading indicator on UI thread
        mainHandler.post(() -> {
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.VISIBLE);
            }
            if (recyclerViewAlbum != null) {
                recyclerViewAlbum.setVisibility(View.GONE);
            }
            if (emptyState != null) {
                emptyState.setVisibility(View.GONE);
            }
        });

        // Load trips in background thread
        executorService.execute(() -> {
            try {
                // Get all trips
                allTrips = databaseHelper.getAllTrips(currentUserId);
                android.util.Log.d("AlbumFragment", "Found " + allTrips.size() + " trips");

                // Find current trip (trip that includes today's date)
                Trip currentTrip = findCurrentTrip(allTrips);

                // Update UI on main thread
                mainHandler.post(() -> {
                    isLoading = false;
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.GONE);
                    }

                    if (allTrips.isEmpty()) {
                        showEmptyState();
                    } else {
                        // Select current trip or first trip
                        Trip tripToSelect = currentTrip != null ? currentTrip : allTrips.get(0);
                        selectTrip(tripToSelect);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("AlbumFragment", "Error loading trips", e);
                mainHandler.post(() -> {
                    isLoading = false;
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                    showEmptyState();
                });
            }
        });
    }

    /**
     * Find the current trip (trip that is currently happening - includes today's
     * date)
     */
    private Trip findCurrentTrip(List<Trip> trips) {
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());

        // Get today at midnight for comparison
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayMillis = today.getTimeInMillis();

        for (Trip trip : trips) {
            try {
                Date startDate = sdf.parse(trip.getStartDate());
                Date endDate = sdf.parse(trip.getEndDate());

                if (startDate != null && endDate != null) {
                    // Set end date to end of day
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(endDate);
                    endCal.set(Calendar.HOUR_OF_DAY, 23);
                    endCal.set(Calendar.MINUTE, 59);
                    endCal.set(Calendar.SECOND, 59);
                    endCal.set(Calendar.MILLISECOND, 999);

                    // Check if today is between start and end date (inclusive)
                    if (todayMillis >= startDate.getTime() && todayMillis <= endCal.getTimeInMillis()) {
                        android.util.Log.d("AlbumFragment", "Found current trip: " + trip.getTripName());
                        return trip;
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        android.util.Log.d("AlbumFragment", "No current trip found");
        return null;
    }

    /**
     * Select a specific trip and load its captures
     */
    private void selectTrip(Trip trip) {
        selectedTrip = trip;
        tvSelectedTrip.setText(trip.getTripName());

        // Load captures for selected trip
        loadCapturesForTrip(trip);
    }

    /**
     * Load captures for a specific trip
     */
    private void loadCapturesForTrip(Trip trip) {
        // Show loading
        mainHandler.post(() -> {
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(View.VISIBLE);
            }
        });

        executorService.execute(() -> {
            try {
                List<AlbumSection> sections = loadTripDataInBackground(trip);

                mainHandler.post(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.GONE);
                    }

                    albumSections.clear();
                    albumSections.addAll(sections);

                    if (albumSections.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                        adapter.updateSections(albumSections);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("AlbumFragment", "Error loading trip data", e);
                mainHandler.post(() -> {
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                    showEmptyState();
                });
            }
        });
    }

    /**
     * Show trip selection dialog
     */
    private void showTripSelectionDialog() {
        if (allTrips.isEmpty()) {
            Toast.makeText(getContext(), "No trips available", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_trip, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        // Make background transparent to show rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        RecyclerView rvTripList = dialogView.findViewById(R.id.rv_trip_list);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        rvTripList.setLayoutManager(new LinearLayoutManager(getContext()));
        TripSelectionAdapter adapter = new TripSelectionAdapter(allTrips, trip -> {
            selectTrip(trip);
            dialog.dismiss();
        });
        rvTripList.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Background thread method to load and organize captures for a specific trip
     */
    private List<AlbumSection> loadTripDataInBackground(Trip trip) {
        List<AlbumSection> sections = new ArrayList<>();
        List<Capture> captures = databaseHelper.getCapturesForTripOrdered(trip.getTripId());

        // Add images from Schedules
        List<com.example.voyagerbuds.models.ScheduleItem> schedules = databaseHelper
                .getSchedulesForTrip(trip.getTripId());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (com.example.voyagerbuds.models.ScheduleItem schedule : schedules) {
            if (schedule.getImagePaths() != null && !schedule.getImagePaths().isEmpty()) {
                try {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(schedule.getImagePaths());
                    long scheduleTime = 0;
                    try {
                        Date date = sdf.parse(schedule.getDay());
                        if (date != null)
                            scheduleTime = date.getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < jsonArray.length(); i++) {
                        String path = jsonArray.getString(i);
                        Capture c = new Capture();
                        c.setCaptureId(-1); // Placeholder ID
                        c.setTripId(trip.getTripId());
                        c.setUserId(currentUserId);
                        c.setMediaPath(path);
                        c.setMediaType("photo");
                        c.setDescription("From Schedule: " + schedule.getTitle());
                        c.setCapturedAt(scheduleTime);
                        captures.add(c);
                    }
                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Add images from Expenses
        List<com.example.voyagerbuds.models.Expense> expenses = databaseHelper.getExpensesForTrip(trip.getTripId());
        for (com.example.voyagerbuds.models.Expense expense : expenses) {
            if (expense.getImagePaths() != null && !expense.getImagePaths().isEmpty()) {
                try {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(expense.getImagePaths());
                    long expenseTime = expense.getSpentAt() * 1000L;

                    for (int i = 0; i < jsonArray.length(); i++) {
                        String path = jsonArray.getString(i);
                        Capture c = new Capture();
                        c.setCaptureId(-1); // Placeholder ID
                        c.setTripId(trip.getTripId());
                        c.setUserId(currentUserId);
                        c.setMediaPath(path);
                        c.setMediaType("photo");
                        c.setDescription("From Expense: " + expense.getCategory());
                        c.setCapturedAt(expenseTime);
                        captures.add(c);
                    }
                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        android.util.Log.d("AlbumFragment",
                "Trip '" + trip.getTripName() + "' has " + captures.size() + " items");

        if (!captures.isEmpty()) {
            List<AlbumDay> days = organizeCapturesByDay(trip, captures);
            AlbumSection section = new AlbumSection(trip, days);
            sections.add(section);
        }

        return sections;
    }

    /**
     * Organize captures by day within a trip, handling UTC timezone
     */
    private List<AlbumDay> organizeCapturesByDay(Trip trip, List<Capture> captures) {
        List<AlbumDay> days = new ArrayList<>();

        try {
            // Parse trip dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date tripStartDate = dateFormat.parse(trip.getStartDate());

            if (tripStartDate == null) {
                // Fallback: group all captures in one day
                AlbumDay singleDay = new AlbumDay("All Captures", System.currentTimeMillis(), captures);
                days.add(singleDay);
                return days;
            }

            // Get user's timezone
            TimeZone userTimeZone = TimeZone.getDefault();
            Calendar tripStartCal = Calendar.getInstance(userTimeZone);
            tripStartCal.setTime(tripStartDate);
            tripStartCal.set(Calendar.HOUR_OF_DAY, 0);
            tripStartCal.set(Calendar.MINUTE, 0);
            tripStartCal.set(Calendar.SECOND, 0);
            tripStartCal.set(Calendar.MILLISECOND, 0);

            // Group captures by day
            Map<Integer, List<Capture>> capturesByDay = new HashMap<>();

            for (Capture capture : captures) {
                long capturedTimestamp = capture.getCapturedAt();

                // Convert captured timestamp to user's timezone
                Calendar captureCal = Calendar.getInstance(userTimeZone);
                captureCal.setTimeInMillis(capturedTimestamp);

                // Calculate day number relative to trip start
                long diffInMillis = captureCal.getTimeInMillis() - tripStartCal.getTimeInMillis();
                int dayNumber = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;

                // Ensure day number is at least 1
                if (dayNumber < 1)
                    dayNumber = 1;

                if (!capturesByDay.containsKey(dayNumber)) {
                    capturesByDay.put(dayNumber, new ArrayList<>());
                }
                capturesByDay.get(dayNumber).add(capture);
            }

            // Create AlbumDay objects
            List<Integer> dayNumbers = new ArrayList<>(capturesByDay.keySet());
            java.util.Collections.sort(dayNumbers);

            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

            for (int dayNumber : dayNumbers) {
                List<Capture> dayCaptures = capturesByDay.get(dayNumber);

                // Calculate the actual date for this day
                Calendar dayCal = (Calendar) tripStartCal.clone();
                dayCal.add(Calendar.DAY_OF_MONTH, dayNumber - 1);

                String dateLabel = "Day " + dayNumber + " - " + displayFormat.format(dayCal.getTime());

                // Sort captures within the day by time
                dayCaptures.sort((c1, c2) -> Long.compare(c1.getCapturedAt(), c2.getCapturedAt()));

                AlbumDay albumDay = new AlbumDay(dateLabel, dayCal.getTimeInMillis(), dayCaptures);
                days.add(albumDay);
            }

        } catch (ParseException e) {
            e.printStackTrace();
            // Fallback: group all captures in one day
            AlbumDay singleDay = new AlbumDay("All Captures", System.currentTimeMillis(), captures);
            days.add(singleDay);
        }

        return days;
    }

    private void showCapturePreview(int captureId, String mediaPath) {
        List<Capture> allCaptures = new ArrayList<>();
        int startPos = 0;

        for (AlbumSection section : albumSections) {
            for (AlbumDay day : section.getDays()) {
                for (Capture c : day.getCaptures()) {
                    allCaptures.add(c);
                    // Match by ID if valid, otherwise by path (for schedule/expense images with
                    // placeholder ID -1)
                    if (c.getCaptureId() == captureId && (captureId != -1 || c.getMediaPath().equals(mediaPath))) {
                        startPos = allCaptures.size() - 1;
                    }
                }
            }
        }

        FullImageFragment fragment = FullImageFragment.newInstance(allCaptures, startPos);

        // Navigate to activity level to replace the main content container
        // Use activity's fragment manager to ensure we're replacing the top-level
        // container
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        recyclerViewAlbum.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyState.setVisibility(View.GONE);
        recyclerViewAlbum.setVisibility(View.VISIBLE);
    }

}
