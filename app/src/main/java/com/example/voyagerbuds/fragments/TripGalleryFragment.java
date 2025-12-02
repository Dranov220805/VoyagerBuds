package com.example.voyagerbuds.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.adapters.GalleryAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.GalleryItem;
import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.models.Expense;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripGalleryFragment extends Fragment implements GalleryAdapter.OnItemClickListener {

    private static final String ARG_TRIP_ID = "trip_id";
    public static final String ACTION_GALLERY_REFRESH = "com.example.voyagerbuds.GALLERY_REFRESH";

    private int tripId;
    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private List<Object> galleryItems = new ArrayList<>();
    private List<GalleryItem> selectedItems = new ArrayList<>();
    private boolean isSelectionMode = false;
    private MaterialToolbar toolbar;
    private TextView tvEmptyState;
    private BroadcastReceiver galleryRefreshReceiver;

    public static TripGalleryFragment newInstance(int tripId) {
        TripGalleryFragment fragment = new TripGalleryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TRIP_ID, tripId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tripId = getArguments().getInt(ARG_TRIP_ID);
        }
        databaseHelper = new DatabaseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.recycler_view_gallery);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        setupRecyclerView();
        loadGalleryItems();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register broadcast receiver
        galleryRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadGalleryItems();
            }
        };
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(galleryRefreshReceiver, new IntentFilter(ACTION_GALLERY_REFRESH));

        // Reload gallery items when returning to this fragment
        loadGalleryItems();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister broadcast receiver
        if (galleryRefreshReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(galleryRefreshReceiver);
        }
    }

    private void setupRecyclerView() {
        int spanCount = 3;
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spanCount);
        adapter = new GalleryAdapter(requireContext(), galleryItems, this);
        layoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup(spanCount));

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void loadGalleryItems() {
        galleryItems.clear();

        if (tripId == -1) {
            Toast.makeText(requireContext(), "No current trip selected", Toast.LENGTH_SHORT).show();
            tvEmptyState.setText("No trip selected");
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        android.util.Log.d("TripGalleryFragment", "Loading gallery for trip ID: " + tripId);

        List<ScheduleItem> schedules = databaseHelper.getSchedulesForTrip(tripId);
        List<com.example.voyagerbuds.models.Expense> expenses = databaseHelper.getExpensesForTrip(tripId);
        List<com.example.voyagerbuds.models.Capture> captures = databaseHelper.getCapturesForTripOrdered(tripId);

        android.util.Log.d("TripGalleryFragment", "Found: " + schedules.size() + " schedules, " +
                expenses.size() + " expenses, " + captures.size() + " captures");

        List<GalleryItem> allImages = new ArrayList<>();

        // Add images from schedules
        for (ScheduleItem schedule : schedules) {
            if (schedule.getImagePaths() != null && !schedule.getImagePaths().isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray(schedule.getImagePaths());
                    String dateStr = schedule.getDay(); // Using day as label
                    long dateMillis = 0;
                    try {
                        java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .parse(dateStr);
                        if (d != null)
                            dateMillis = d.getTime();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < jsonArray.length(); i++) {
                        String path = jsonArray.getString(i);
                        allImages.add(new GalleryItem(path, schedule.getId(), 0, dateStr, dateMillis));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Add images from expenses
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        for (com.example.voyagerbuds.models.Expense expense : expenses) {
            if (expense.getImagePaths() != null && !expense.getImagePaths().isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray(expense.getImagePaths());
                    long dateMillis = expense.getSpentAt() * 1000L;
                    String dateStr = sdf.format(new java.util.Date(dateMillis));

                    for (int i = 0; i < jsonArray.length(); i++) {
                        String path = jsonArray.getString(i);
                        allImages.add(new GalleryItem(path, expense.getExpenseId(), 1, dateStr, dateMillis));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Add images from captures
        for (com.example.voyagerbuds.models.Capture capture : captures) {
            if (capture.getMediaPath() != null && !capture.getMediaPath().isEmpty()) {
                long dateMillis = capture.getCapturedAt();
                String dateStr = sdf.format(new java.util.Date(dateMillis));
                allImages.add(new GalleryItem(capture.getMediaPath(), capture.getCaptureId(), 2, dateStr, dateMillis));
            }
        }

        // Sort by date
        java.util.Collections.sort(allImages, (a, b) -> a.getDayLabel().compareTo(b.getDayLabel()));

        // Group by Day
        String currentDay = "";
        for (GalleryItem item : allImages) {
            if (!item.getDayLabel().equals(currentDay)) {
                currentDay = item.getDayLabel();
                galleryItems.add(currentDay);
            }
            galleryItems.add(item);
        }

        android.util.Log.d("TripGalleryFragment", "Total gallery items (with headers): " + galleryItems.size());
        android.util.Log.d("TripGalleryFragment", "Total images: " + allImages.size());

        if (galleryItems.isEmpty()) {
            android.util.Log.d("TripGalleryFragment", "Showing empty state");
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            android.util.Log.d("TripGalleryFragment", "Showing gallery with " + galleryItems.size() + " items");
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateItems(galleryItems);
        }
    }

    @Override
    public void onItemClick(GalleryItem item, int position) {
        if (isSelectionMode) {
            toggleSelection(item);
        } else {
            // Convert all GalleryItems to Captures for the viewer
            List<com.example.voyagerbuds.models.Capture> captures = new ArrayList<>();
            int startPos = 0;
            int index = 0;

            for (Object obj : galleryItems) {
                if (obj instanceof GalleryItem) {
                    GalleryItem gi = (GalleryItem) obj;
                    com.example.voyagerbuds.models.Capture c = new com.example.voyagerbuds.models.Capture();
                    c.setCaptureId(gi.getItemId());
                    c.setMediaPath(gi.getImagePath());
                    c.setMediaType("photo");
                    c.setCapturedAt(gi.getDate());

                    // Set description based on type
                    if (gi.getItemType() == 0)
                        c.setDescription("From Schedule");
                    else if (gi.getItemType() == 1)
                        c.setDescription("From Expense");
                    else
                        c.setDescription(""); // Capture description would need DB lookup if not passed, but for now
                                              // empty is fine or we could fetch it.

                    captures.add(c);

                    if (gi == item) {
                        startPos = index;
                    }
                    index++;
                }
            }

            FullImageFragment fragment = FullImageFragment.newInstance(captures, startPos);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.content_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onItemLongClick(GalleryItem item, int position) {
        if (!isSelectionMode) {
            startSelectionMode();
        }
        toggleSelection(item);
    }

    private void startSelectionMode() {
        isSelectionMode = true;
        adapter.setSelectionMode(true);
        toolbar.getMenu().findItem(R.id.action_delete).setVisible(true);
        toolbar.setTitle("Select items");
    }

    private void stopSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        for (Object obj : galleryItems) {
            if (obj instanceof GalleryItem) {
                ((GalleryItem) obj).setSelected(false);
            }
        }
        adapter.setSelectionMode(false);
        toolbar.getMenu().findItem(R.id.action_delete).setVisible(false);
        toolbar.setTitle("Trip Gallery");
    }

    private void toggleSelection(GalleryItem item) {
        item.setSelected(!item.isSelected());
        if (item.isSelected()) {
            selectedItems.add(item);
        } else {
            selectedItems.remove(item);
        }
        adapter.notifyDataSetChanged();

        if (selectedItems.isEmpty()) {
            stopSelectionMode();
        } else {
            toolbar.setTitle(selectedItems.size() + " selected");
        }
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteSelectedItems();
            return true;
        }
        return false;
    }

    private void deleteSelectedItems() {
        Map<Integer, List<String>> scheduleImagesToDelete = new HashMap<>();
        Map<Integer, List<String>> expenseImagesToDelete = new HashMap<>();
        List<Integer> capturesToDelete = new ArrayList<>();

        for (GalleryItem item : selectedItems) {
            if (item.getItemType() == 0) {
                // Schedule image
                if (!scheduleImagesToDelete.containsKey(item.getItemId())) {
                    scheduleImagesToDelete.put(item.getItemId(), new ArrayList<>());
                }
                scheduleImagesToDelete.get(item.getItemId()).add(item.getImagePath());
            } else if (item.getItemType() == 1) {
                // Expense image
                if (!expenseImagesToDelete.containsKey(item.getItemId())) {
                    expenseImagesToDelete.put(item.getItemId(), new ArrayList<>());
                }
                expenseImagesToDelete.get(item.getItemId()).add(item.getImagePath());
            } else if (item.getItemType() == 2) {
                // Capture image
                capturesToDelete.add(item.getItemId());
            }
        }

        // Delete schedule images
        for (Map.Entry<Integer, List<String>> entry : scheduleImagesToDelete.entrySet()) {
            int scheduleId = entry.getKey();
            List<String> pathsToRemove = entry.getValue();

            ScheduleItem schedule = databaseHelper.getScheduleById(scheduleId);
            if (schedule != null) {
                try {
                    JSONArray currentArray = new JSONArray(schedule.getImagePaths());
                    JSONArray newArray = new JSONArray();
                    for (int i = 0; i < currentArray.length(); i++) {
                        String path = currentArray.getString(i);
                        if (!pathsToRemove.contains(path)) {
                            newArray.put(path);
                        }
                    }
                    databaseHelper.updateScheduleImages(scheduleId, newArray.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Delete expense images
        for (Map.Entry<Integer, List<String>> entry : expenseImagesToDelete.entrySet()) {
            int expenseId = entry.getKey();
            List<String> pathsToRemove = entry.getValue();

            Expense expense = databaseHelper.getExpenseById(expenseId);
            if (expense != null) {
                try {
                    JSONArray currentArray = new JSONArray(expense.getImagePaths());
                    JSONArray newArray = new JSONArray();
                    for (int i = 0; i < currentArray.length(); i++) {
                        String path = currentArray.getString(i);
                        if (!pathsToRemove.contains(path)) {
                            newArray.put(path);
                        }
                    }
                    databaseHelper.updateExpenseImages(expenseId, newArray.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Delete captures
        for (int captureId : capturesToDelete) {
            databaseHelper.deleteCapture(captureId);
        }

        stopSelectionMode();
        loadGalleryItems();
        Toast.makeText(requireContext(), "Items deleted", Toast.LENGTH_SHORT).show();
    }
}
