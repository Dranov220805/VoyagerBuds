package com.example.voyagerbuds.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;
import com.example.voyagerbuds.adapters.ScheduleAdapter;
import com.example.voyagerbuds.adapters.ScheduleDayAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.ScheduleDayGroup;
import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.models.Trip;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TripDetailFragment extends Fragment {

    private static final String ARG_TRIP_ID = "trip_id";
    private static final String FLEXIBLE_DAY_KEY = "__flexible__";
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private long tripId;
    private DatabaseHelper databaseHelper;
    private Trip trip;

    private RecyclerView recyclerView;
    private ScheduleDayAdapter dayAdapter;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddSchedule;
    private View layoutSchedule;
    private View layoutExpenses;
    private View layoutNotes;

    public static TripDetailFragment newInstance(long tripId) {
        TripDetailFragment fragment = new TripDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TRIP_ID, tripId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tripId = getArguments().getLong(ARG_TRIP_ID);
        }
        databaseHelper = new DatabaseHelper(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_detail, container, false);

        // Initialize Views
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        TextView tvTitle = view.findViewById(R.id.tv_trip_title);
        TextView tvDates = view.findViewById(R.id.tv_trip_dates);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        recyclerView = view.findViewById(R.id.recycler_view_schedule);
        tvEmptyState = view.findViewById(R.id.tv_empty_schedule);
        fabAddSchedule = view.findViewById(R.id.fab_add_schedule);
        layoutSchedule = view.findViewById(R.id.layout_schedule);
        layoutExpenses = view.findViewById(R.id.layout_expenses);
        layoutNotes = view.findViewById(R.id.layout_notes);

        // Setup Toolbar
        toolbar.setNavigationOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                editTrip();
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteConfirmationDialog();
                return true;
            }
            return false;
        });

        // Load Trip Data
        trip = databaseHelper.getTripById((int) tripId);
        if (trip != null) {
            tvTitle.setText(trip.getTripName());
            String dateRange = trip.getStartDate() + " - " + trip.getEndDate();
            tvDates.setText(dateRange);
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dayAdapter = new ScheduleDayAdapter(requireContext(), new ArrayList<>(),
                new ScheduleAdapter.OnScheduleActionListener() {
                    @Override
                    public void onEdit(ScheduleItem item) {
                        showAddEditDialog(item);
                    }

                    @Override
                    public void onDelete(ScheduleItem item) {
                        deleteSchedule(item);
                    }
                });
        recyclerView.setAdapter(dayAdapter);

        // Setup FAB
        fabAddSchedule.setOnClickListener(v -> showAddEditDialog(null));

        // Load Schedules
        loadSchedules();

        // Handle Tabs
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                layoutSchedule.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                layoutExpenses.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                layoutNotes.setVisibility(position == 2 ? View.VISIBLE : View.GONE);

                if (position == 0) {
                    fabAddSchedule.show();
                } else {
                    fabAddSchedule.hide();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        return view;
    }

    private void loadSchedules() {
        List<ScheduleItem> scheduleItems = databaseHelper.getSchedulesForTrip((int) tripId);
        if (scheduleItems.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            dayAdapter.updateGroups(new ArrayList<>());
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            List<ScheduleDayGroup> groups = groupSchedulesByDay(scheduleItems);
            dayAdapter.updateGroups(groups);
        }
    }

    private void showAddEditDialog(@Nullable ScheduleItem editing) {
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
                        newItem.setTripId((int) tripId);
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

                    loadSchedules();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteSchedule(ScheduleItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.schedule_delete_title)
                .setMessage(R.string.schedule_delete_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    databaseHelper.deleteSchedule(item.getId());
                    Toast.makeText(getContext(), R.string.schedule_deleted, Toast.LENGTH_SHORT).show();
                    loadSchedules();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void editTrip() {
        if (trip == null) {
            Toast.makeText(getContext(), "Trip not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to EditTripFragment with trip data
        EditTripFragment editFragment = EditTripFragment.newInstance(tripId);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right)
                .replace(R.id.content_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showDeleteConfirmationDialog() {
        if (trip == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Trip")
                .setMessage(
                        "Are you sure you want to delete \"" + trip.getTripName() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTrip();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTrip() {
        if (trip == null) {
            return;
        }

        try {
            databaseHelper.deleteTrip((int) tripId);
            Toast.makeText(getContext(), "Trip deleted successfully", Toast.LENGTH_SHORT).show();

            // Navigate back to home fragment
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() instanceof HomeActivity) {
                // If no back stack, manually show home fragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.content_container, new HomeFragment())
                        .commit();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to delete trip", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
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
}
