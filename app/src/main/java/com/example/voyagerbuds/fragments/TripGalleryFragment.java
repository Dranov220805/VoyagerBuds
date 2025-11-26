package com.example.voyagerbuds.fragments;

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
    private int tripId;
    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private List<Object> galleryItems = new ArrayList<>();
    private List<GalleryItem> selectedItems = new ArrayList<>();
    private boolean isSelectionMode = false;
    private MaterialToolbar toolbar;
    private TextView tvEmptyState;

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
        List<ScheduleItem> schedules = databaseHelper.getSchedulesForTrip(tripId);
        List<com.example.voyagerbuds.models.Expense> expenses = databaseHelper.getExpensesForTrip(tripId);
        List<GalleryItem> allImages = new ArrayList<>();

        // Add images from schedules
        for (ScheduleItem schedule : schedules) {
            if (schedule.getImagePaths() != null && !schedule.getImagePaths().isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray(schedule.getImagePaths());
                    String dateStr = schedule.getDay(); // Using day as label

                    for (int i = 0; i < jsonArray.length(); i++) {
                        String path = jsonArray.getString(i);
                        allImages.add(new GalleryItem(path, schedule.getId(), 0, dateStr));
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
                    String dateStr = sdf.format(new java.util.Date(expense.getSpentAt() * 1000L));

                    for (int i = 0; i < jsonArray.length(); i++) {
                        String path = jsonArray.getString(i);
                        allImages.add(new GalleryItem(path, expense.getExpenseId(), 1, dateStr));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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

        if (galleryItems.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
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
            // TODO: Open full screen view
            Toast.makeText(requireContext(), "Image: " + item.getImagePath(), Toast.LENGTH_SHORT).show();
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
        Map<Integer, List<String>> imagesToDelete = new HashMap<>();
        for (GalleryItem item : selectedItems) {
            if (!imagesToDelete.containsKey(item.getScheduleId())) {
                imagesToDelete.put(item.getScheduleId(), new ArrayList<>());
            }
            imagesToDelete.get(item.getScheduleId()).add(item.getImagePath());
        }

        for (Map.Entry<Integer, List<String>> entry : imagesToDelete.entrySet()) {
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

        stopSelectionMode();
        loadGalleryItems();
        Toast.makeText(requireContext(), "Items deleted", Toast.LENGTH_SHORT).show();
    }
}
