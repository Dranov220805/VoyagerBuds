package com.example.voyagerbuds.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;
import com.example.voyagerbuds.adapters.ScheduleAdapter;
import com.example.voyagerbuds.adapters.ScheduleDayAdapter;
import com.example.voyagerbuds.adapters.ScheduleImageAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.ScheduleDayGroup;
import com.example.voyagerbuds.models.ScheduleItem;
import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.utils.DateUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TripDetailFragment extends Fragment {

    private static final String ARG_TRIP_ID = "trip_id";
    private static final String FLEXIBLE_DAY_KEY = "__flexible__";
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private long tripId;
    private DatabaseHelper databaseHelper;
    private Trip trip;
    private FusedLocationProviderClient fusedLocationClient;
    private EditText etLocationRef; // Reference to update location from callback

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private Uri photoUri;
    private List<String> tempImagePaths;
    private ScheduleImageAdapter tempImageAdapter;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    // Ignore
                }
                if (tempImagePaths != null) {
                    tempImagePaths.add(uri.toString());
                    if (tempImageAdapter != null)
                        tempImageAdapter.notifyItemInserted(tempImagePaths.size() - 1);
                }
            }
        });

        takePhotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && photoUri != null) {
                if (tempImagePaths != null) {
                    tempImagePaths.add(photoUri.toString());
                    if (tempImageAdapter != null)
                        tempImageAdapter.notifyItemInserted(tempImagePaths.size() - 1);
                }
            }
        });
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
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                java.util.Date start = sdf.parse(trip.getStartDate());
                java.util.Date end = sdf.parse(trip.getEndDate());
                if (start != null && end != null) {
                    tvDates.setText(DateUtils.formatDateRangeSimple(getContext(), start, end));
                } else {
                    tvDates.setText(trip.getStartDate() + " - " + trip.getEndDate());
                }
            } catch (Exception e) {
                tvDates.setText(trip.getStartDate() + " - " + trip.getEndDate());
            }
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_schedule, null);
        bottomSheetDialog.setContentView(dialogView);

        // Configure BottomSheet Behavior
        bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog.getBehavior().setDraggable(false); // Disable default drag to prevent accidental dismissal

        View dragHandle = dialogView.findViewById(R.id.layout_drag_handle);
        View btnClose = dialogView.findViewById(R.id.btn_close_sheet);

        // Enable dragging only when touching the handle
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    bottomSheetDialog.getBehavior().setDraggable(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // We keep it draggable until the gesture ends, but we can't easily reset it
                    // here
                    // because the behavior might still be processing the drag.
                    // However, setting it to false here might stop the fling.
                    // A better approach for "only handle" is usually complex, but let's try this:
                    // If we set it to false, the next touch on content won't drag.
                    // We delay it slightly or just set it.
                    v.post(() -> bottomSheetDialog.getBehavior().setDraggable(false));
                    break;
            }
            return false; // Let the touch propagate to the behavior
        });

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etDay = dialogView.findViewById(R.id.et_schedule_day);
        EditText etStartTime = dialogView.findViewById(R.id.et_schedule_start_time);
        EditText etEndTime = dialogView.findViewById(R.id.et_schedule_end_time);
        EditText etTitle = dialogView.findViewById(R.id.et_schedule_title);
        EditText etNotes = dialogView.findViewById(R.id.et_schedule_notes);
        EditText etLocation = dialogView.findViewById(R.id.et_schedule_location);
        ImageButton btnMyLocation = dialogView.findViewById(R.id.btn_my_location);

        // New fields
        EditText etExpenseAmount = dialogView.findViewById(R.id.et_schedule_expense_amount);
        Spinner spinnerCurrency = dialogView.findViewById(R.id.spinner_schedule_currency);
        EditText etParticipants = dialogView.findViewById(R.id.et_schedule_participants);
        EditText etNotifyBefore = dialogView.findViewById(R.id.et_schedule_notify_before);
        Button btnAddImage = dialogView.findViewById(R.id.btn_add_image);
        RecyclerView rvImages = dialogView.findViewById(R.id.rv_schedule_images);
        Button btnSave = dialogView.findViewById(R.id.btn_save_schedule);

        // Setup Currency Spinner
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new String[] { "USD", "VND" });
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(currencyAdapter);

        if (editing != null) {
            tempImagePaths = parseImagePaths(editing.getImagePaths());
        } else {
            tempImagePaths = new ArrayList<>();
        }

        tempImageAdapter = new ScheduleImageAdapter(getContext(), tempImagePaths, true, position -> {
            tempImagePaths.remove(position);
            tempImageAdapter.notifyItemRemoved(position);
        });
        rvImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(tempImageAdapter);

        btnAddImage.setOnClickListener(v -> showImageSourceDialog());

        if (editing != null) {
            etDay.setText(editing.getDay());
            etStartTime.setText(editing.getStartTime());
            etEndTime.setText(editing.getEndTime());
            etTitle.setText(editing.getTitle());
            etNotes.setText(editing.getNotes());
            etLocation.setText(editing.getLocation());
            etParticipants.setText(editing.getParticipants());
            if (editing.getNotifyBeforeMinutes() > 0) {
                etNotifyBefore.setText(String.valueOf(editing.getNotifyBeforeMinutes()));
            }
            if (editing.getExpenseAmount() > 0) {
                etExpenseAmount.setText(String.valueOf(editing.getExpenseAmount()));
            }
            if (editing.getExpenseCurrency() != null) {
                int spinnerPosition = currencyAdapter.getPosition(editing.getExpenseCurrency());
                if (spinnerPosition >= 0) {
                    spinnerCurrency.setSelection(spinnerPosition);
                }
            }
            tvDialogTitle.setText(R.string.schedule_edit_title);
        } else {
            tvDialogTitle.setText(R.string.schedule_add_event);
        }

        btnMyLocation.setOnClickListener(v -> {
            etLocationRef = etLocation;
            getLocationAndSetAddress();
        });

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

        etNotifyBefore.setOnClickListener(v -> {
            int currentMinutes = 0;
            try {
                String val = etNotifyBefore.getText().toString();
                if (!val.isEmpty()) {
                    currentMinutes = Integer.parseInt(val);
                }
            } catch (NumberFormatException e) {
                // Ignore
            }

            int hour = currentMinutes / 60;
            int minute = currentMinutes % 60;

            TimePickerDialog tp = new TimePickerDialog(requireContext(),
                    (view14, h, m) -> {
                        int totalMinutes = h * 60 + m;
                        etNotifyBefore.setText(String.valueOf(totalMinutes));
                    },
                    hour, minute, true);
            tp.setTitle(getString(R.string.notify_before));
            tp.show();
        });

        btnSave.setOnClickListener(v -> {
            String day = etDay.getText().toString().trim();
            String start = etStartTime.getText().toString().trim();
            String end = etEndTime.getText().toString().trim();
            String title = etTitle.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String participants = etParticipants.getText().toString().trim();
            String expenseStr = etExpenseAmount.getText().toString().trim();
            String notifyBeforeStr = etNotifyBefore.getText().toString().trim();
            String currency = spinnerCurrency.getSelectedItem().toString();

            double expenseAmount = 0;
            if (!expenseStr.isEmpty()) {
                try {
                    expenseAmount = Double.parseDouble(expenseStr);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            int notifyBeforeMinutes = 0;
            if (!notifyBeforeStr.isEmpty()) {
                try {
                    notifyBeforeMinutes = Integer.parseInt(notifyBeforeStr);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

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
                newItem.setLocation(location);
                newItem.setParticipants(participants);
                newItem.setExpenseAmount(expenseAmount);
                newItem.setExpenseCurrency(currency);
                newItem.setImagePaths(serializeImagePaths(tempImagePaths));
                newItem.setNotifyBeforeMinutes(notifyBeforeMinutes);
                newItem.setCreatedAt(System.currentTimeMillis());
                newItem.setUpdatedAt(System.currentTimeMillis());
                long id = databaseHelper.addSchedule(newItem);
                newItem.setId((int) id);
                com.example.voyagerbuds.utils.NotificationHelper.scheduleNotification(requireContext(),
                        newItem);
                Toast.makeText(getContext(), R.string.schedule_added, Toast.LENGTH_SHORT).show();
            } else {
                editing.setDay(day);
                editing.setStartTime(start);
                editing.setEndTime(end);
                editing.setTitle(title);
                editing.setNotes(notes);
                editing.setLocation(location);
                editing.setParticipants(participants);
                editing.setExpenseAmount(expenseAmount);
                editing.setExpenseCurrency(currency);
                editing.setImagePaths(serializeImagePaths(tempImagePaths));
                editing.setNotifyBeforeMinutes(notifyBeforeMinutes);
                editing.setUpdatedAt(System.currentTimeMillis());
                databaseHelper.updateSchedule(editing);
                com.example.voyagerbuds.utils.NotificationHelper.scheduleNotification(requireContext(),
                        editing);
                Toast.makeText(getContext(), R.string.schedule_updated, Toast.LENGTH_SHORT).show();
            }

            loadSchedules();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void deleteSchedule(ScheduleItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.schedule_delete_title)
                .setMessage(R.string.schedule_delete_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    com.example.voyagerbuds.utils.NotificationHelper.cancelNotification(requireContext(), item.getId());
                    databaseHelper.deleteSchedule(item.getId());
                    Toast.makeText(getContext(), R.string.schedule_deleted, Toast.LENGTH_SHORT).show();
                    loadSchedules();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void editTrip() {
        if (trip == null) {
            Toast.makeText(getContext(), getString(R.string.toast_trip_not_found), Toast.LENGTH_SHORT).show();
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
                .setTitle(R.string.delete_trip_title)
                .setMessage(getString(R.string.delete_trip_confirm_message, trip.getTripName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleteTrip();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
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
            Toast.makeText(getContext(), getString(R.string.toast_trip_deleted_success), Toast.LENGTH_SHORT).show();

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
            Toast.makeText(getContext(), getString(R.string.toast_failed_delete_trip), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void getLocationAndSetAddress() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Toast.makeText(getContext(), R.string.getting_location, Toast.LENGTH_SHORT).show();
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        getAddressFromLocation(location);
                    } else {
                        Toast.makeText(getContext(), R.string.location_unavailable, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                if (etLocationRef != null) {
                    etLocationRef.setText(addressText);
                }
                Toast.makeText(getContext(), String.format(getString(R.string.location_found), addressText),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.geocoding_error, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), R.string.geocoding_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(getContext(), R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 2002) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                photoUri = createImageFile();
                if (photoUri != null) {
                    takePhotoLauncher.launch(photoUri);
                }
            } else {
                Toast.makeText(getContext(), R.string.permission_required, Toast.LENGTH_SHORT).show();
            }
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

    private void showDetailDialog(ScheduleItem item) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_schedule_detail, null);
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog.getBehavior().setDraggable(false);

        View dragHandle = dialogView.findViewById(R.id.layout_drag_handle);

        // Enable dragging only when touching the handle
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    bottomSheetDialog.getBehavior().setDraggable(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // We keep it draggable until the gesture ends, but we can't easily reset it
                    // here
                    // because the behavior might still be processing the drag.
                    // However, setting it to false here might stop the fling.
                    // A better approach for "only handle" is usually complex, but let's try this:
                    // If we set it to false, the next touch on content won't drag.
                    // We delay it slightly or just set it.
                    v.post(() -> bottomSheetDialog.getBehavior().setDraggable(false));
                    break;
            }
            return false; // Let the touch propagate to the behavior
        });

        EditText etTitle = dialogView.findViewById(R.id.et_detail_title);
        EditText etTime = dialogView.findViewById(R.id.et_detail_time);
        EditText etLocation = dialogView.findViewById(R.id.et_detail_location);
        EditText etNotes = dialogView.findViewById(R.id.et_detail_notes);
        EditText etParticipants = dialogView.findViewById(R.id.et_detail_participants);
        EditText etExpense = dialogView.findViewById(R.id.et_detail_expense);
        EditText etNotify = dialogView.findViewById(R.id.et_detail_notify);

        View layoutLocation = dialogView.findViewById(R.id.layout_detail_location_input);
        View layoutNotes = dialogView.findViewById(R.id.layout_detail_notes_input);
        View layoutParticipants = dialogView.findViewById(R.id.layout_detail_participants_input);
        View layoutExpense = dialogView.findViewById(R.id.layout_detail_expense_input);
        View layoutNotify = dialogView.findViewById(R.id.layout_detail_notify_input);

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

        if (item.getParticipants() != null && !item.getParticipants().isEmpty()) {
            etParticipants.setText(item.getParticipants());
            layoutParticipants.setVisibility(View.VISIBLE);
        } else {
            layoutParticipants.setVisibility(View.GONE);
        }

        if (item.getExpenseAmount() > 0) {
            String currency = item.getExpenseCurrency() != null ? item.getExpenseCurrency() : "USD";
            etExpense.setText(String.format(Locale.getDefault(), "%.2f %s", item.getExpenseAmount(), currency));
            layoutExpense.setVisibility(View.VISIBLE);
        } else {
            layoutExpense.setVisibility(View.GONE);
        }

        if (item.getNotifyBeforeMinutes() > 0) {
            int mins = item.getNotifyBeforeMinutes();
            String text;
            if (mins == 1440)
                text = "1 day";
            else if (mins == 60)
                text = "1 hour";
            else if (mins % 60 == 0)
                text = (mins / 60) + " hours";
            else
                text = mins + " minutes";

            etNotify.setText(text);
            layoutNotify.setVisibility(View.VISIBLE);
        } else {
            layoutNotify.setVisibility(View.GONE);
        }

        List<String> detailImagePaths = parseImagePaths(item.getImagePaths());
        if (!detailImagePaths.isEmpty()) {
            ScheduleImageAdapter detailAdapter = new ScheduleImageAdapter(getContext(), detailImagePaths, false, null);
            rvImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvImages.setAdapter(detailAdapter);
            rvImages.setVisibility(View.VISIBLE);
        } else {
            rvImages.setVisibility(View.GONE);
        }

        // Hide Edit/Delete buttons in detail dialog as they are now in long-press menu
        // But user might still want them here for convenience.
        // The prompt said "If user hold the event, it will have a smaller menu for
        // (edit event, delete event)"
        // It didn't explicitly say to remove them from detail dialog.
        // However, usually detail dialogs are read-only if there's a separate edit
        // flow.
        // Let's keep them for now as it's better UX to have multiple ways.

        btnDelete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            deleteSchedule(item);
        });

        btnEdit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showAddEditDialog(item);
        });

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnCloseSheet != null) {
            btnCloseSheet.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();
    }

    private void navigateToMapWithPin(ScheduleItem item) {
        String locationName = item.getLocation();
        String title = item.getTitle();

        // Geocode the location name to get coordinates
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

                if (item.getExpenseAmount() > 0) {
                    String currency = item.getExpenseCurrency() != null ? item.getExpenseCurrency() : "USD";
                    String budget = String.format(Locale.getDefault(), "%.2f %s", item.getExpenseAmount(), currency);
                    args.putString("pin_budget", budget);
                }

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

    private void showItemMenu(View view, ScheduleItem item) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenu().add(0, 1, 0, R.string.edit);
        popup.getMenu().add(0, 2, 0, R.string.delete);

        popup.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == 1) {
                showAddEditDialog(item);
                return true;
            } else if (menuItem.getItemId() == 2) {
                deleteSchedule(item);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private Uri createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider",
                    image);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showImageSourceDialog() {
        String[] options = { getString(R.string.camera), getString(R.string.gallery) };
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_image_source)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        if (ContextCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[] { Manifest.permission.CAMERA }, 2002);
                        } else {
                            photoUri = createImageFile();
                            if (photoUri != null) {
                                takePhotoLauncher.launch(photoUri);
                            }
                        }
                    } else {
                        // Gallery
                        pickImageLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private List<String> parseImagePaths(String json) {
        List<String> paths = new ArrayList<>();
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    paths.add(jsonArray.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }

    private String serializeImagePaths(List<String> paths) {
        JSONArray jsonArray = new JSONArray();
        for (String path : paths) {
            jsonArray.put(path);
        }
        return jsonArray.toString();
    }
}
