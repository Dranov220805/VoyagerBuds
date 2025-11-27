package com.example.voyagerbuds.fragments;

import android.annotation.SuppressLint;
import android.view.animation.AnimationUtils;
import androidx.transition.TransitionManager;
import androidx.transition.AutoTransition;
import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
import com.example.voyagerbuds.fragments.TripGalleryFragment;
import com.example.voyagerbuds.utils.ImageUtils;
import com.example.voyagerbuds.utils.DateValidatorWithinRange;
import com.example.voyagerbuds.utils.ImageRandomizer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.voyagerbuds.adapters.ExpenseAdapter;
import com.example.voyagerbuds.adapters.ExpenseDateAdapter;
import com.example.voyagerbuds.models.Expense;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.TimeZone;

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
    private View rootView; // Store root view for gallery refresh

    // Expense fields
    private RecyclerView rvExpenseDates;
    private RecyclerView rvExpenses;
    private TextView tvExpenseDateHeader;
    private TextView tvEmptyExpenses;
    private ExpenseDateAdapter expenseDateAdapter;
    private ExpenseAdapter expenseAdapter;
    private List<Date> tripDates = new ArrayList<>();
    private List<Expense> allExpenses = new ArrayList<>();
    private Date selectedExpenseDate;
    private Date previousExpenseDate;
    private int lastTabPosition = 0;

    private java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors
            .newSingleThreadExecutor();
    private android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

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
        rootView = view; // Store for later use

        // Initialize Views
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        TextView tvTitle = view.findViewById(R.id.tv_trip_title);
        TextView tvDates = view.findViewById(R.id.tv_trip_dates);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        android.widget.ImageView imgTripCover = view.findViewById(R.id.img_trip_cover);

        recyclerView = view.findViewById(R.id.recycler_view_schedule);
        tvEmptyState = view.findViewById(R.id.tv_empty_schedule);
        fabAddSchedule = view.findViewById(R.id.fab_add_schedule);
        layoutSchedule = view.findViewById(R.id.layout_schedule);
        layoutExpenses = view.findViewById(R.id.layout_expenses);
        layoutNotes = view.findViewById(R.id.layout_notes);

        rvExpenseDates = view.findViewById(R.id.recycler_view_expense_dates);
        rvExpenses = view.findViewById(R.id.recycler_view_expenses);
        tvExpenseDateHeader = view.findViewById(R.id.tv_expense_date_header);
        tvEmptyExpenses = view.findViewById(R.id.tv_empty_expenses);

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

            // Load random background image for trip cover
            if (imgTripCover != null) {
                String photoUrl = trip.getPhotoUrl();
                int backgroundImage = 0;
                boolean isCustomUri = false;

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    backgroundImage = ImageRandomizer.getDrawableFromName(photoUrl);
                    if (backgroundImage == 0) {
                        // It's a custom URI
                        isCustomUri = true;
                        RequestOptions options = new RequestOptions()
                                .centerCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.voyagerbuds_nobg)
                                .error(R.drawable.voyagerbuds_nobg);

                        try {
                            Glide.with(this)
                                    .load(android.net.Uri.parse(photoUrl))
                                    .apply(options)
                                    .into(imgTripCover);
                        } catch (Exception e) {
                            imgTripCover.setImageResource(R.drawable.voyagerbuds_nobg);
                        }
                    }
                } else {
                    // No photoUrl, use trip ID based image
                    backgroundImage = ImageRandomizer.getConsistentRandomBackground(trip.getTripId());
                }

                // Only load drawable if it's not a custom URI
                if (!isCustomUri && backgroundImage != 0) {
                    RequestOptions options = new RequestOptions()
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.voyagerbuds_nobg)
                            .error(R.drawable.voyagerbuds_nobg);

                    try {
                        Glide.with(this)
                                .load(backgroundImage)
                                .apply(options)
                                .into(imgTripCover);
                    } catch (Exception e) {
                        // Fallback if Glide fails
                        imgTripCover.setImageResource(backgroundImage);
                    }
                }
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
                boolean movingRight = position > lastTabPosition;
                lastTabPosition = position;

                // Animate views
                View[] views = { layoutSchedule, layoutExpenses, layoutNotes };
                for (int i = 0; i < views.length; i++) {
                    if (i == position) {
                        // Enter animation
                        views[i].setVisibility(View.VISIBLE);
                        views[i].startAnimation(AnimationUtils.loadAnimation(getContext(),
                                movingRight ? R.anim.slide_in_right : R.anim.slide_in_left));
                    } else {
                        // Exit animation
                        if (views[i].getVisibility() == View.VISIBLE) {
                            android.view.animation.Animation exitAnim = AnimationUtils.loadAnimation(getContext(),
                                    movingRight ? R.anim.slide_out_left : R.anim.slide_out_right);
                            final View viewToHide = views[i];
                            exitAnim.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(android.view.animation.Animation animation) {
                                }

                                @Override
                                public void onAnimationEnd(android.view.animation.Animation animation) {
                                    viewToHide.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(android.view.animation.Animation animation) {
                                }
                            });
                            views[i].startAnimation(exitAnim);
                        }
                    }
                }

                if (position == 0) {
                    fabAddSchedule.show();
                    fabAddSchedule.setImageResource(R.drawable.ic_add);
                    fabAddSchedule.setOnClickListener(v -> showAddEditDialog(null));
                } else if (position == 1) {
                    fabAddSchedule.show();
                    fabAddSchedule.setImageResource(R.drawable.ic_add);
                    fabAddSchedule.setOnClickListener(v -> showAddExpenseDialog());
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

        // Load Gallery Preview
        loadGalleryPreview(view);

        // Setup Expenses
        setupExpenses();

        return view;
    }

    private void loadGalleryPreview(View view) {
        if (view == null || !isAdded() || getContext() == null) {
            return;
        }

        android.widget.LinearLayout llGalleryPreview = view.findViewById(R.id.ll_gallery_preview);
        TextView tvEmptyGallery = view.findViewById(R.id.tv_empty_gallery);
        TextView btnViewAll = view.findViewById(R.id.btn_view_all_gallery);

        if (llGalleryPreview == null || tvEmptyGallery == null || btnViewAll == null) {
            return;
        }

        btnViewAll.setOnClickListener(v -> {
            TripGalleryFragment galleryFragment = TripGalleryFragment.newInstance((int) tripId);
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.content_container, galleryFragment)
                    .addToBackStack(null)
                    .commit();
        });

        executorService.execute(() -> {
            List<ScheduleItem> schedules = databaseHelper.getSchedulesForTrip((int) tripId);
            List<Expense> expenses = databaseHelper.getExpensesForTrip((int) tripId);
            List<String> allImages = new ArrayList<>();

            // Add images from schedules
            for (ScheduleItem schedule : schedules) {
                if (schedule.getImagePaths() != null && !schedule.getImagePaths().isEmpty()) {
                    try {
                        JSONArray jsonArray = new JSONArray(schedule.getImagePaths());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            allImages.add(jsonArray.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Add images from expenses
            for (Expense expense : expenses) {
                if (expense.getImagePaths() != null && !expense.getImagePaths().isEmpty()) {
                    try {
                        JSONArray jsonArray = new JSONArray(expense.getImagePaths());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            allImages.add(jsonArray.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            Collections.reverse(allImages);
            List<String> previewImages = allImages.subList(0, Math.min(allImages.size(), 5));

            mainHandler.post(() -> {
                // Check if fragment is still valid
                if (!isAdded() || getContext() == null || llGalleryPreview == null || tvEmptyGallery == null) {
                    return;
                }

                llGalleryPreview.removeAllViews();
                if (previewImages.isEmpty()) {
                    llGalleryPreview.addView(tvEmptyGallery);
                    tvEmptyGallery.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyGallery.setVisibility(View.GONE);
                    for (String path : previewImages) {
                        android.widget.ImageView imageView = new android.widget.ImageView(getContext());
                        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                                (int) (120 * getResources().getDisplayMetrics().density),
                                (int) (160 * getResources().getDisplayMetrics().density));
                        params.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
                        imageView.setLayoutParams(params);
                        imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        imageView.setBackgroundResource(R.drawable.rounded_corner_bg);
                        imageView.setClipToOutline(true);

                        executorService.execute(() -> {
                            android.graphics.Bitmap bitmap = null;
                            try {
                                Uri uri;
                                if (path.startsWith("content://") || path.startsWith("file://")) {
                                    uri = Uri.parse(path);
                                } else {
                                    uri = Uri.fromFile(new File(path));
                                }
                                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                                options.inJustDecodeBounds = true;
                                java.io.InputStream input = requireContext().getContentResolver().openInputStream(uri);
                                android.graphics.BitmapFactory.decodeStream(input, null, options);
                                if (input != null)
                                    input.close();

                                options.inSampleSize = com.example.voyagerbuds.utils.ImageUtils
                                        .calculateInSampleSize(options, 300, 300);
                                options.inJustDecodeBounds = false;

                                input = requireContext().getContentResolver().openInputStream(uri);
                                bitmap = android.graphics.BitmapFactory.decodeStream(input, null, options);
                                if (input != null)
                                    input.close();

                                bitmap = com.example.voyagerbuds.utils.ImageUtils
                                        .rotateImageIfRequired(requireContext(), bitmap, uri);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            android.graphics.Bitmap finalBitmap = bitmap;
                            mainHandler.post(() -> {
                                if (finalBitmap != null) {
                                    imageView.setImageBitmap(finalBitmap);
                                }
                            });
                        });

                        imageView.setOnClickListener(v -> {
                            showFullImageDialog(path);
                        });

                        llGalleryPreview.addView(imageView);
                    }
                }
            });
        });
    }

    private void notifyGalleryRefresh() {
        // Send broadcast to notify TripGalleryFragment to refresh
        Intent intent = new Intent(TripGalleryFragment.ACTION_GALLERY_REFRESH);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
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

        // Track if changes have been made
        final boolean[] hasUnsavedChanges = { false };

        // Configure BottomSheet Behavior
        bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        // bottomSheetDialog.getBehavior().setDraggable(false); // Removed to fix swipe
        // issue

        View dragHandle = dialogView.findViewById(R.id.layout_drag_handle);
        View btnClose = dialogView.findViewById(R.id.btn_close_sheet);

        /*
         * Removed restrictive touch listener
         * // Enable dragging only when touching the handle
         * dragHandle.setOnTouchListener((v, event) -> {
         * switch (event.getAction()) {
         * case MotionEvent.ACTION_DOWN:
         * bottomSheetDialog.getBehavior().setDraggable(true);
         * break;
         * case MotionEvent.ACTION_UP:
         * case MotionEvent.ACTION_CANCEL:
         * // We keep it draggable until the gesture ends, but we can't easily reset it
         * // here
         * // because the behavior might still be processing the drag.
         * // However, setting it to false here might stop the fling.
         * // A better approach for "only handle" is usually complex, but let's try
         * this:
         * // If we set it to false, the next touch on content won't drag.
         * // We delay it slightly or just set it.
         * v.post(() -> bottomSheetDialog.getBehavior().setDraggable(false));
         * break;
         * }
         * return false; // Let the touch propagate to the behavior
         * });
         */

        btnClose.setOnClickListener(v -> {
            if (hasUnsavedChanges[0]) {
                showDiscardScheduleDialog(() -> bottomSheetDialog.dismiss());
            } else {
                bottomSheetDialog.dismiss();
            }
        });

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etDay = dialogView.findViewById(R.id.et_schedule_day);
        EditText etStartTime = dialogView.findViewById(R.id.et_schedule_start_time);
        EditText etEndTime = dialogView.findViewById(R.id.et_schedule_end_time);
        EditText etTitle = dialogView.findViewById(R.id.et_schedule_title);
        EditText etNotes = dialogView.findViewById(R.id.et_schedule_notes);
        EditText etLocation = dialogView.findViewById(R.id.et_schedule_location);
        ImageButton btnMyLocation = dialogView.findViewById(R.id.btn_my_location);

        // New fields
        EditText etParticipants = dialogView.findViewById(R.id.et_schedule_participants);
        EditText etNotifyBefore = dialogView.findViewById(R.id.et_schedule_notify_before);
        Button btnAddImage = dialogView.findViewById(R.id.btn_add_image);
        RecyclerView rvImages = dialogView.findViewById(R.id.rv_schedule_images);
        Button btnSave = dialogView.findViewById(R.id.btn_save_schedule);

        if (editing != null) {
            tempImagePaths = parseImagePaths(editing.getImagePaths());
        } else {
            tempImagePaths = new ArrayList<>();
        }

        tempImageAdapter = new ScheduleImageAdapter(getContext(), tempImagePaths, true, position -> {
            tempImagePaths.remove(position);
            tempImageAdapter.notifyItemRemoved(position);
            hasUnsavedChanges[0] = true;
        });
        tempImageAdapter.setOnImageRotationListener(this::rotateImage);
        rvImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(tempImageAdapter);

        // Track changes in all input fields
        TextWatcher changeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges[0] = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        etDay.addTextChangedListener(changeWatcher);
        etStartTime.addTextChangedListener(changeWatcher);
        etEndTime.addTextChangedListener(changeWatcher);
        etTitle.addTextChangedListener(changeWatcher);
        etNotes.addTextChangedListener(changeWatcher);
        etLocation.addTextChangedListener(changeWatcher);
        etParticipants.addTextChangedListener(changeWatcher);
        etNotifyBefore.addTextChangedListener(changeWatcher);

        btnAddImage.setOnClickListener(v -> {
            hasUnsavedChanges[0] = true;
            showImageSourceDialog();
        });

        if (editing != null) {
            etDay.setText(editing.getDay());
            etStartTime.setText(editing.getStartTime());
            etEndTime.setText(editing.getEndTime());
            etTitle.setText(editing.getTitle());
            etNotes.setText(editing.getNotes());
            etLocation.setText(editing.getLocation());
            etParticipants.setText(editing.getParticipants());
            if (editing.getNotifyBeforeMinutes() > 0) {
                etNotifyBefore.setText(formatNotificationTime(editing.getNotifyBeforeMinutes()));
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
            // Use Material Date Picker with trip date constraints
            showScheduleDatePicker(etDay, editing);
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
            showNotifyBeforeMenu(etNotifyBefore);
        });

        btnSave.setOnClickListener(v -> {
            String day = etDay.getText().toString().trim();
            String start = etStartTime.getText().toString().trim();
            String end = etEndTime.getText().toString().trim();
            String title = etTitle.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String participants = etParticipants.getText().toString().trim();
            String notifyBeforeStr = etNotifyBefore.getText().toString().trim();

            int notifyBeforeMinutes = 0;
            if (!notifyBeforeStr.isEmpty()) {
                notifyBeforeMinutes = parseNotificationTime(notifyBeforeStr);
            }

            if (title.isEmpty()) {
                Toast.makeText(getContext(), R.string.schedule_title_required, Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading
            View loadingView = dialogView.findViewById(R.id.loading_container);
            if (loadingView != null)
                loadingView.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);

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
                newItem.setImagePaths(serializeImagePaths(tempImagePaths));
                newItem.setNotifyBeforeMinutes(notifyBeforeMinutes);
                newItem.setCreatedAt(System.currentTimeMillis());
                newItem.setUpdatedAt(System.currentTimeMillis());

                executorService.execute(() -> {
                    try {
                        long id = databaseHelper.addSchedule(newItem);
                        newItem.setId((int) id);
                        com.example.voyagerbuds.utils.NotificationHelper.scheduleNotification(requireContext(),
                                newItem);
                        mainHandler.post(() -> {
                            if (loadingView != null)
                                loadingView.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(getContext(), R.string.schedule_added, Toast.LENGTH_SHORT).show();
                            loadSchedules();
                            loadGalleryPreview(rootView);
                            notifyGalleryRefresh();
                            hasUnsavedChanges[0] = false;
                            bottomSheetDialog.dismiss();
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (loadingView != null)
                                loadingView.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(getContext(), "Error saving schedule: " + e.getMessage(), Toast.LENGTH_LONG)
                                    .show();
                        });
                    }
                });
            } else {
                editing.setDay(day);
                editing.setStartTime(start);
                editing.setEndTime(end);
                editing.setTitle(title);
                editing.setNotes(notes);
                editing.setLocation(location);
                editing.setParticipants(participants);
                editing.setImagePaths(serializeImagePaths(tempImagePaths));
                editing.setNotifyBeforeMinutes(notifyBeforeMinutes);
                editing.setUpdatedAt(System.currentTimeMillis());

                executorService.execute(() -> {
                    try {
                        databaseHelper.updateSchedule(editing);
                        com.example.voyagerbuds.utils.NotificationHelper.scheduleNotification(requireContext(),
                                editing);
                        mainHandler.post(() -> {
                            if (loadingView != null)
                                loadingView.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(getContext(), R.string.schedule_updated, Toast.LENGTH_SHORT).show();
                            loadSchedules();
                            loadGalleryPreview(rootView);
                            notifyGalleryRefresh();
                            hasUnsavedChanges[0] = false;
                            bottomSheetDialog.dismiss();
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (loadingView != null)
                                loadingView.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(getContext(), "Error updating schedule: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        });

        // Handle back button or outside touch dismiss
        bottomSheetDialog.setOnCancelListener(dialog -> {
            if (hasUnsavedChanges[0]) {
                showDiscardScheduleDialog(() -> bottomSheetDialog.dismiss());
            }
        });

        bottomSheetDialog.show();
    }

    private void showDiscardScheduleDialog(Runnable onConfirm) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.discard_schedule_title)
                .setMessage(R.string.discard_schedule_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    onConfirm.run();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    private void showNotifyBeforeMenu(EditText targetField) {
        String[] options = new String[] {
                getString(R.string.notify_10_minutes),
                getString(R.string.notify_30_minutes),
                getString(R.string.notify_1_hour),
                getString(R.string.notify_custom)
        };

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                requireContext());

        builder.setTitle(R.string.notify_before)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // 10 minutes
                            targetField.setText(formatNotificationTime(10));
                            break;
                        case 1: // 30 minutes
                            targetField.setText(formatNotificationTime(30));
                            break;
                        case 2: // 1 hour
                            targetField.setText(formatNotificationTime(60));
                            break;
                        case 3: // Custom
                            showCustomNotificationPicker(targetField);
                            break;
                    }
                })
                .show();
    }

    private void showCustomNotificationPicker(EditText targetField) {
        View pickerView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_notification, null);

        NumberPicker numberPicker = pickerView.findViewById(R.id.number_picker);
        NumberPicker unitPicker = pickerView.findViewById(R.id.unit_picker);

        // Setup number picker (1-60 for minutes/hours, 1-30 for days)
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(60);
        numberPicker.setValue(30);
        numberPicker.setWrapSelectorWheel(false);

        // Setup unit picker (minutes, hours, days)
        String[] units = new String[] {
                getString(R.string.minutes),
                getString(R.string.hours),
                getString(R.string.days)
        };
        unitPicker.setMinValue(0);
        unitPicker.setMaxValue(units.length - 1);
        unitPicker.setDisplayedValues(units);
        unitPicker.setValue(0); // Default to minutes
        unitPicker.setWrapSelectorWheel(false);

        // Adjust number picker range based on unit selection
        unitPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (newVal == 2) { // days
                numberPicker.setMaxValue(30);
                if (numberPicker.getValue() > 30) {
                    numberPicker.setValue(30);
                }
            } else {
                numberPicker.setMaxValue(60);
            }
        });

        // Apply entrance animation
        pickerView.setAlpha(0f);
        pickerView.setTranslationY(50f);
        pickerView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                requireContext());

        androidx.appcompat.app.AlertDialog dialog = builder
                .setView(pickerView)
                .setPositiveButton(R.string.done, (d, which) -> {
                    int number = numberPicker.getValue();
                    int unit = unitPicker.getValue();
                    int totalMinutes;

                    switch (unit) {
                        case 0: // minutes
                            totalMinutes = number;
                            break;
                        case 1: // hours
                            totalMinutes = number * 60;
                            break;
                        case 2: // days
                            totalMinutes = number * 60 * 24;
                            break;
                        default:
                            totalMinutes = number;
                    }

                    targetField.setText(formatNotificationTime(totalMinutes));
                })
                .setNegativeButton(R.string.cancel, null)
                .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_rounded))
                .create();

        // Show dialog
        dialog.show();

        // Style the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                ContextCompat.getColor(requireContext(), R.color.main_color_voyager));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_medium));
    }

    private String formatNotificationTime(int minutes) {
        if (minutes < 60) {
            return minutes + " " + getString(R.string.minutes);
        } else if (minutes < 1440) { // Less than a day
            int hours = minutes / 60;
            return hours + " " + getString(R.string.hours);
        } else {
            int days = minutes / 1440;
            return days + " " + getString(R.string.days);
        }
    }

    private int parseNotificationTime(String formatted) {
        try {
            String[] parts = formatted.trim().split(" ");
            if (parts.length >= 2) {
                int number = Integer.parseInt(parts[0]);
                String unit = parts[1].toLowerCase();

                if (unit.contains(getString(R.string.hours).toLowerCase()) || unit.contains("hour")
                        || unit.contains("giờ")) {
                    return number * 60;
                } else if (unit.contains(getString(R.string.days).toLowerCase()) || unit.contains("day")
                        || unit.contains("ngày")) {
                    return number * 60 * 24;
                } else {
                    return number; // minutes
                }
            }
            // Try parsing as plain number (legacy format)
            return Integer.parseInt(formatted);
        } catch (Exception e) {
            return 0;
        }
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
                    loadGalleryPreview(rootView);
                    notifyGalleryRefresh();
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
                .setIcon(R.drawable.ic_warning)
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

    @SuppressLint("StringFormatInvalid")
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
        // bottomSheetDialog.getBehavior().setDraggable(false); // Removed to fix swipe
        // issue

        View dragHandle = dialogView.findViewById(R.id.layout_drag_handle);

        /*
         * Removed restrictive touch listener
         * // Enable dragging only when touching the handle
         * dragHandle.setOnTouchListener((v, event) -> {
         * switch (event.getAction()) {
         * case MotionEvent.ACTION_DOWN:
         * bottomSheetDialog.getBehavior().setDraggable(true);
         * break;
         * case MotionEvent.ACTION_UP:
         * case MotionEvent.ACTION_CANCEL:
         * // We keep it draggable until the gesture ends, but we can't easily reset it
         * // here
         * // because the behavior might still be processing the drag.
         * // However, setting it to false here might stop the fling.
         * // A better approach for "only handle" is usually complex, but let's try
         * this:
         * // If we set it to false, the next touch on content won't drag.
         * // We delay it slightly or just set it.
         * v.post(() -> bottomSheetDialog.getBehavior().setDraggable(false));
         * break;
         * }
         * return false; // Let the touch propagate to the behavior
         * });
         */

        EditText etTitle = dialogView.findViewById(R.id.et_detail_title);
        EditText etTime = dialogView.findViewById(R.id.et_detail_time);
        EditText etLocation = dialogView.findViewById(R.id.et_detail_location);
        TextInputLayout layoutLocation = dialogView.findViewById(R.id.layout_detail_location_input);
        EditText etNotes = dialogView.findViewById(R.id.et_detail_notes);
        TextInputLayout layoutNotes = dialogView.findViewById(R.id.layout_detail_notes_input);
        TextInputLayout layoutParticipants = dialogView.findViewById(R.id.layout_detail_participants_input);
        EditText etParticipants = dialogView.findViewById(R.id.et_detail_participants);
        TextInputLayout layoutNotify = dialogView.findViewById(R.id.layout_detail_notify_input);
        EditText etNotify = dialogView.findViewById(R.id.et_detail_notify);
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
            detailAdapter.setOnImageClickListener(this::showFullImageDialog);
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

                /*
                 * if (item.getExpenseAmount() > 0) {
                 * String currency = item.getExpenseCurrency() != null ?
                 * item.getExpenseCurrency() : "USD";
                 * String budget = String.format(Locale.getDefault(), "%.2f %s",
                 * item.getExpenseAmount(), currency);
                 * args.putString("pin_budget", budget);
                 * }
                 */

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

    private void rotateImage(int position, String imagePath) {
        executorService.execute(() -> {
            try {
                android.net.Uri uri;
                if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                    uri = android.net.Uri.parse(imagePath);
                } else {
                    uri = android.net.Uri.fromFile(new java.io.File(imagePath));
                }

                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(
                        requireContext().getContentResolver().openInputStream(uri));

                if (bitmap != null) {
                    android.graphics.Matrix matrix = new android.graphics.Matrix();
                    matrix.postRotate(90);
                    android.graphics.Bitmap rotatedBitmap = android.graphics.Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    // Save rotated bitmap to a new file
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String imageFileName = "ROTATED_" + timeStamp + "_";
                    File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    File rotatedFile = File.createTempFile(imageFileName, ".jpg", storageDir);

                    java.io.FileOutputStream out = new java.io.FileOutputStream(rotatedFile);
                    rotatedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();

                    String newPath = Uri.fromFile(rotatedFile).toString();

                    mainHandler.post(() -> {
                        if (position >= 0 && position < tempImagePaths.size()) {
                            tempImagePaths.set(position, newPath);
                            tempImageAdapter.notifyItemChanged(position);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler
                        .post(() -> Toast.makeText(getContext(), "Failed to rotate image", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupExpenses() {
        if (trip == null)
            return;

        // Generate dates
        tripDates.clear();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(trip.getStartDate());
            Date end = sdf.parse(trip.getEndDate());

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);

            while (!calendar.getTime().after(end)) {
                tripDates.add(calendar.getTime());
                calendar.add(Calendar.DATE, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tripDates.isEmpty())
            return;

        selectedExpenseDate = tripDates.get(0);

        // Setup Date Adapter
        rvExpenseDates.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        expenseDateAdapter = new ExpenseDateAdapter(getContext(), tripDates, (date, position) -> {
            previousExpenseDate = selectedExpenseDate;
            selectedExpenseDate = date;
            updateExpenseList();
        });
        rvExpenseDates.setAdapter(expenseDateAdapter);

        // Setup Expense Adapter
        rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExpenses
                .setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_slide_in));
        expenseAdapter = new ExpenseAdapter(getContext(), new ArrayList<>(), expense -> {
            showExpenseDetail(expense);
        });
        rvExpenses.setAdapter(expenseAdapter);

        loadExpenses();
    }

    private void loadExpenses() {
        executorService.execute(() -> {
            allExpenses = databaseHelper.getExpensesForTrip((int) tripId);
            mainHandler.post(this::updateExpenseList);
        });
    }

    private void updateExpenseList() {
        if (selectedExpenseDate == null)
            return;

        SimpleDateFormat headerFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
        tvExpenseDateHeader.setText(headerFormat.format(selectedExpenseDate));

        List<Expense> filteredExpenses = new ArrayList<>();
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(selectedExpenseDate);

        for (Expense expense : allExpenses) {
            // Convert timestamp to Date
            Date expenseDate = new Date((long) expense.getSpentAt() * 1000);
            cal2.setTime(expenseDate);

            if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {
                filteredExpenses.add(expense);
            }
        }

        if (filteredExpenses.isEmpty()) {
            rvExpenses.setVisibility(View.GONE);
            tvEmptyExpenses.setVisibility(View.VISIBLE);
        } else {
            rvExpenses.setVisibility(View.VISIBLE);
            tvEmptyExpenses.setVisibility(View.GONE);

            // Determine direction (kept for reference or future use, but not used for fade)
            // boolean movingRight = true;
            // if (previousExpenseDate != null && selectedExpenseDate != null) {
            // movingRight = selectedExpenseDate.after(previousExpenseDate);
            // }

            // Animate list change with Fade
            rvExpenses.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        expenseAdapter.updateExpenses(filteredExpenses);
                        rvExpenses.animate()
                                .alpha(1f)
                                .setDuration(150)
                                .start();
                    })
                    .start();
        }
    }

    private void showExpenseDetail(Expense expense) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_expense_detail, null);
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        EditText etCategory = dialogView.findViewById(R.id.et_detail_category);
        EditText etAmount = dialogView.findViewById(R.id.et_detail_amount);
        EditText etCurrency = dialogView.findViewById(R.id.et_detail_currency);
        EditText etDate = dialogView.findViewById(R.id.et_detail_date);
        EditText etNotes = dialogView.findViewById(R.id.et_detail_notes);
        TextInputLayout layoutNotes = dialogView.findViewById(R.id.layout_detail_notes_input);
        RecyclerView rvImages = dialogView.findViewById(R.id.rv_detail_images);
        View btnDelete = dialogView.findViewById(R.id.btn_detail_delete);
        View btnEdit = dialogView.findViewById(R.id.btn_detail_edit);
        View btnClose = dialogView.findViewById(R.id.btn_detail_close);
        View btnCloseSheet = dialogView.findViewById(R.id.btn_close_sheet);

        etCategory.setText(expense.getCategory());
        etAmount.setText(String.format(Locale.getDefault(), "%.2f", expense.getAmount()));
        etCurrency.setText(expense.getCurrency());

        // Format date
        if (expense.getSpentAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            etDate.setText(sdf.format(new Date(expense.getSpentAt() * 1000L)));
        }

        // Parse and display note
        String noteDisplay = "";
        String rawNote = expense.getNote();
        if (rawNote != null && !rawNote.isEmpty()) {
            try {
                JSONObject json = new JSONObject(rawNote);
                if (json.has("text")) {
                    noteDisplay = json.getString("text");
                }
            } catch (JSONException e) {
                noteDisplay = rawNote;
            }
        }

        if (noteDisplay != null && !noteDisplay.isEmpty()) {
            etNotes.setText(noteDisplay);
            layoutNotes.setVisibility(View.VISIBLE);
        } else {
            layoutNotes.setVisibility(View.GONE);
        }

        // Handle images
        List<String> imagePathList = new ArrayList<>();
        String paths = expense.getImagePaths();
        if (paths != null && !paths.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(paths);
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
            ScheduleImageAdapter imageAdapter = new ScheduleImageAdapter(
                    requireContext(), imagePathList, false, null);
            imageAdapter.setOnImageClickListener(this::showFullImageDialog);
            rvImages.setAdapter(imageAdapter);
        } else {
            rvImages.setVisibility(View.GONE);
        }

        btnDelete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            deleteExpense(expense);
        });

        btnEdit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showEditExpenseDialog(expense);
        });

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnCloseSheet != null) {
            btnCloseSheet.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();
    }

    private void deleteExpense(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_expense_title)
                .setMessage(R.string.delete_expense_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    executorService.execute(() -> {
                        databaseHelper.deleteExpense(expense.getExpenseId());
                        mainHandler.post(() -> {
                            Toast.makeText(getContext(), R.string.expense_deleted, Toast.LENGTH_SHORT).show();
                            loadExpenses();
                            loadGalleryPreview(rootView);
                            notifyGalleryRefresh();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditExpenseDialog(Expense expense) {
        showAddEditExpenseDialog(expense);
    }

    private void showAddExpenseDialog() {
        showAddEditExpenseDialog(null);
    }

    private void showAddEditExpenseDialog(@Nullable Expense editing) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_expense, null);
        bottomSheetDialog.setContentView(dialogView);

        // Track if changes have been made
        final boolean[] hasUnsavedChanges = { false };

        bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        View btnClose = dialogView.findViewById(R.id.btn_close_sheet);
        btnClose.setOnClickListener(v -> {
            if (hasUnsavedChanges[0]) {
                showDiscardExpenseDialog(() -> bottomSheetDialog.dismiss());
            } else {
                bottomSheetDialog.dismiss();
            }
        });

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etName = dialogView.findViewById(R.id.et_expense_name);
        EditText etAmount = dialogView.findViewById(R.id.et_expense_amount);
        Spinner spinnerCurrency = dialogView.findViewById(R.id.spinner_expense_currency);
        EditText etDate = dialogView.findViewById(R.id.et_expense_date);
        EditText etNote = dialogView.findViewById(R.id.et_expense_note);
        Button btnAddImage = dialogView.findViewById(R.id.btn_add_expense_image);
        RecyclerView rvImages = dialogView.findViewById(R.id.rv_expense_images);
        Button btnSave = dialogView.findViewById(R.id.btn_save_expense);

        // Setup Currency Spinner
        String[] currencies = new String[] { "USD", "VND" };
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(currencyAdapter);

        // Setup Images
        if (editing != null) {
            // Load existing images
            tempImagePaths = new ArrayList<>();
            String paths = editing.getImagePaths();
            if (paths != null && !paths.isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray(paths);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        tempImagePaths.add(jsonArray.getString(i));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (tempImagePaths == null) {
                tempImagePaths = new ArrayList<>();
            } else {
                tempImagePaths.clear();
            }
        }

        tempImageAdapter = new ScheduleImageAdapter(getContext(), tempImagePaths, true, position -> {
            tempImagePaths.remove(position);
            tempImageAdapter.notifyItemRemoved(position);
            hasUnsavedChanges[0] = true;
        });
        tempImageAdapter.setOnImageRotationListener(this::rotateImage);
        rvImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(tempImageAdapter);

        // Track changes in all input fields
        TextWatcher changeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasUnsavedChanges[0] = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        etName.addTextChangedListener(changeWatcher);
        etAmount.addTextChangedListener(changeWatcher);
        etDate.addTextChangedListener(changeWatcher);
        etNote.addTextChangedListener(changeWatcher);
        spinnerCurrency.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                hasUnsavedChanges[0] = true;
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnAddImage.setOnClickListener(v -> {
            hasUnsavedChanges[0] = true;
            showImageSourceDialog();
        });

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Populate fields if editing
        if (editing != null) {
            tvDialogTitle.setText(R.string.edit_expense);
            etName.setText(editing.getCategory());
            etAmount.setText(String.format(Locale.getDefault(), "%.2f", editing.getAmount()));

            // Set currency
            String currency = editing.getCurrency();
            if ("VND".equals(currency)) {
                spinnerCurrency.setSelection(1);
            } else {
                spinnerCurrency.setSelection(0);
            }

            // Set date
            if (editing.getSpentAt() > 0) {
                etDate.setText(sdf.format(new Date(editing.getSpentAt() * 1000L)));
            }

            // Extract note text from JSON
            String noteText = "";
            String rawNote = editing.getNote();
            if (rawNote != null && !rawNote.isEmpty()) {
                try {
                    JSONObject json = new JSONObject(rawNote);
                    if (json.has("text")) {
                        noteText = json.getString("text");
                    }
                } catch (JSONException e) {
                    noteText = rawNote;
                }
            }
            etNote.setText(noteText);
        } else {
            tvDialogTitle.setText(R.string.add_expense);

            // Auto-select currency based on language
            String language = Locale.getDefault().getLanguage();
            if ("vi".equals(language)) {
                spinnerCurrency.setSelection(1); // VND
            } else {
                spinnerCurrency.setSelection(0); // USD
            }

            // Default Date
            if (selectedExpenseDate != null) {
                etDate.setText(sdf.format(selectedExpenseDate));
            } else {
                etDate.setText(sdf.format(new Date()));
            }
        }

        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                cal.setTime(sdf.parse(etDate.getText().toString()));
            } catch (Exception e) {
                cal.setTime(new Date());
            }
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dp = new DatePickerDialog(requireContext(), android.R.style.Theme_DeviceDefault_Dialog,
                    (view1, y, m, d) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(y, m, d);
                        etDate.setText(sdf.format(picked.getTime()));
                    }, year, month, day);
            dp.show();
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String currency = spinnerCurrency.getSelectedItem().toString();
            String dateStr = etDate.getText().toString().trim();
            String noteText = etNote.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            if (amountStr.isEmpty()) {
                etAmount.setError("Amount is required");
                return;
            }

            // Parse amount - handle both comma and period as decimal separators
            double amount;
            try {
                // Replace comma with period for proper parsing
                String normalizedAmount = amountStr.replace(",", ".");
                // Remove any grouping separators (spaces, apostrophes)
                normalizedAmount = normalizedAmount.replaceAll("[\\s']", "");
                amount = Double.parseDouble(normalizedAmount);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount format");
                return;
            }

            int spentAt = 0;
            try {
                Date date = sdf.parse(dateStr);
                spentAt = (int) (date.getTime() / 1000);
            } catch (Exception e) {
                e.printStackTrace();
                spentAt = (int) (new Date().getTime() / 1000);
            }

            // Serialize description (text, images) to JSON
            JSONObject noteJson = new JSONObject();
            JSONArray imagesArray = new JSONArray();
            try {
                noteJson.put("text", noteText);
                for (String path : tempImagePaths) {
                    imagesArray.put(path);
                }
                noteJson.put("images", imagesArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Show loading
            View loadingView = dialogView.findViewById(R.id.loading_container);
            if (loadingView != null)
                loadingView.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);

            if (editing == null) {
                // Add new expense
                Expense expense = new Expense();
                expense.setTripId((int) tripId);
                expense.setAmount(amount);
                expense.setCategory(name);
                expense.setCurrency(currency);
                expense.setNote(noteJson.toString());
                expense.setSpentAt(spentAt);
                expense.setImagePaths(imagesArray.toString());

                executorService.execute(() -> {
                    try {
                        databaseHelper.addExpense(expense);
                        mainHandler.post(() -> {
                            if (loadingView != null)
                                loadingView.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            hasUnsavedChanges[0] = false;
                            bottomSheetDialog.dismiss();
                            loadExpenses();
                            loadGalleryPreview(rootView);
                            notifyGalleryRefresh();
                            Toast.makeText(getContext(), R.string.expense_added, Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (loadingView != null)
                                loadingView.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(getContext(), "Error saving expense: " + e.getMessage(), Toast.LENGTH_LONG)
                                    .show();
                        });
                    }
                });
            } else {
                // Update existing expense
                editing.setAmount(amount);
                editing.setCategory(name);
                editing.setCurrency(currency);
                editing.setNote(noteJson.toString());
                editing.setSpentAt(spentAt);
                editing.setImagePaths(imagesArray.toString());

                executorService.execute(() -> {
                    try {
                        databaseHelper.updateExpense(editing);
                        mainHandler.post(() -> {
                            if (loadingView != null)
                                loadingView.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            hasUnsavedChanges[0] = false;
                            bottomSheetDialog.dismiss();
                            loadExpenses();
                            loadGalleryPreview(rootView);
                            notifyGalleryRefresh();
                            Toast.makeText(getContext(), R.string.expense_updated, Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            if (loadingView != null)
                                loadingView.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            Toast.makeText(getContext(), "Error updating expense: " + e.getMessage(), Toast.LENGTH_LONG)
                                    .show();
                        });
                    }
                });
            }
        });

        // Handle back button or outside touch dismiss
        bottomSheetDialog.setOnCancelListener(dialog -> {
            if (hasUnsavedChanges[0]) {
                showDiscardExpenseDialog(() -> bottomSheetDialog.dismiss());
            }
        });

        bottomSheetDialog.show();
    }

    private void showDiscardExpenseDialog(Runnable onConfirm) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.discard_expense_title)
                .setMessage(R.string.discard_expense_message)
                .setPositiveButton(R.string.discard, (dialog, which) -> {
                    onConfirm.run();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    /**
     * Show Material Date Picker with constraints to only allow dates within the
     * trip's date range
     * Dates outside the trip range will be shown in red and cannot be selected
     */
    private void showScheduleDatePicker(EditText targetEditText, ScheduleItem editing) {
        if (trip == null) {
            Toast.makeText(getContext(), "Trip data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse trip start and end dates
            LocalDate tripStart = LocalDate.parse(trip.getStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate tripEnd = LocalDate.parse(trip.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);

            // Convert to UTC milliseconds for MaterialDatePicker
            long startMillis = tripStart.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
            long endMillis = tripEnd.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();

            // Build calendar constraints
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setStart(startMillis);
            constraintsBuilder.setEnd(endMillis);

            // Add validator to make dates outside range invalid (red and untouchable)
            DateValidatorWithinRange validator = new DateValidatorWithinRange(startMillis, endMillis);
            constraintsBuilder.setValidator(validator);

            // Build date picker
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
            builder.setTitleText(getString(R.string.select_schedule_date));
            builder.setCalendarConstraints(constraintsBuilder.build());

            // Set initial selection if editing
            if (editing != null && editing.getDay() != null && !editing.getDay().isEmpty()) {
                try {
                    LocalDate currentDate = LocalDate.parse(editing.getDay(), DateTimeFormatter.ISO_LOCAL_DATE);
                    long currentMillis = currentDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
                    builder.setSelection(currentMillis);
                } catch (Exception e) {
                    // If parsing fails, no initial selection
                }
            }

            MaterialDatePicker<Long> picker = builder.build();

            picker.addOnPositiveButtonClickListener(selection -> {
                // Convert UTC millis back to local date
                Instant instant = Instant.ofEpochMilli(selection);
                LocalDate selectedLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

                // Format as yyyy-MM-dd for database
                String formattedDate = selectedLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
                targetEditText.setText(formattedDate);
            });

            picker.show(getParentFragmentManager(), "SCHEDULE_DATE_PICKER");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading trip dates", Toast.LENGTH_SHORT).show();
        }
    }
}
