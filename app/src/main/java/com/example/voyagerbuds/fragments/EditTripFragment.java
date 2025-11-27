package com.example.voyagerbuds.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.utils.UserSessionManager;
import com.example.voyagerbuds.utils.ImageRandomizer;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditTripFragment extends Fragment {

    private static final String ARG_TRIP_ID = "trip_id";
    private long tripId;
    private Trip trip;
    private DatabaseHelper databaseHelper;

    private TextInputEditText etTripName, etStartDate, etEndDate, etDestination, etBudget, etParticipants, etNotes;
    private Button btnSave, btnCancel;
    private Spinner spinnerBudgetCurrency;
    private android.widget.ImageView imgTripPhoto;
    private Button btnChangePhoto, btnResetPhoto;
    private ActivityResultLauncher<String> pickImageLauncher;
    private String newPhotoUrl = null;
    private String budgetCurrency = "USD"; // Default currency

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public static EditTripFragment newInstance(long tripId) {
        EditTripFragment fragment = new EditTripFragment();
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

        // Initialize image picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    // Ignore
                }
                newPhotoUrl = uri.toString();
                loadImage(newPhotoUrl);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_trip, container, false);

        // Initialize Views
        etTripName = view.findViewById(R.id.et_trip_name);
        etStartDate = view.findViewById(R.id.et_start_date);
        etEndDate = view.findViewById(R.id.et_end_date);
        etDestination = view.findViewById(R.id.et_destination);
        etBudget = view.findViewById(R.id.et_budget);
        etParticipants = view.findViewById(R.id.et_participants);
        etNotes = view.findViewById(R.id.et_notes);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
        spinnerBudgetCurrency = view.findViewById(R.id.spinner_budget_currency);
        imgTripPhoto = view.findViewById(R.id.img_trip_photo);
        btnChangePhoto = view.findViewById(R.id.btn_change_photo);
        btnResetPhoto = view.findViewById(R.id.btn_reset_photo);

        // Setup currency spinner with USD and VND only
        String[] currencies = new String[] { "USD", "VND" };
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBudgetCurrency.setAdapter(currencyAdapter);

        // Detect default currency based on user language
        detectDefaultCurrency();

        // Load Trip Data
        loadTripData();

        // Setup Listeners
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        btnSave.setOnClickListener(v -> saveTrip());
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        if (btnResetPhoto != null) {
            btnResetPhoto.setOnClickListener(v -> {
                // Reset to default random image
                newPhotoUrl = ImageRandomizer.getDefaultImageName(trip.getTripId());
                loadImage(newPhotoUrl);
            });
        }

        return view;
    }

    private void loadTripData() {
        trip = databaseHelper.getTripById((int) tripId);
        if (trip != null) {
            etTripName.setText(trip.getTripName());
            etStartDate.setText(trip.getStartDate());
            etEndDate.setText(trip.getEndDate());
            etDestination.setText(trip.getDestination());
            etBudget.setText(String.valueOf(trip.getBudget()));
            etParticipants.setText(trip.getParticipants());
            etNotes.setText(trip.getNotes());

            // Load currency
            budgetCurrency = trip.getBudgetCurrency();
            if (budgetCurrency == null || budgetCurrency.isEmpty()) {
                detectDefaultCurrency();
            } else {
                updateCurrencySpinner();
            }

            // Load trip image
            loadImage(trip.getPhotoUrl());
        } else {
            Toast.makeText(getContext(), getString(R.string.toast_error_loading_trip), Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        }
    }

    private void loadImage(String photoUrl) {
        if (imgTripPhoto == null)
            return;

        if (photoUrl != null && !photoUrl.isEmpty()) {
            int drawableId = ImageRandomizer.getDrawableFromName(photoUrl);

            if (drawableId != 0) {
                // It's a default drawable resource
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.voyagerbuds_nobg)
                        .error(R.drawable.voyagerbuds_nobg);

                Glide.with(this)
                        .load(drawableId)
                        .apply(options)
                        .into(imgTripPhoto);
            } else {
                // It's a custom URI
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.voyagerbuds_nobg)
                        .error(R.drawable.voyagerbuds_nobg);

                Glide.with(this)
                        .load(Uri.parse(photoUrl))
                        .apply(options)
                        .into(imgTripPhoto);
            }
        } else {
            imgTripPhoto.setImageResource(R.drawable.voyagerbuds_nobg);
        }
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        // Try to parse existing date
        try {
            String dateStr = editText.getText().toString();
            if (!TextUtils.isEmpty(dateStr)) {
                calendar.setTime(dateFormat.parse(dateStr));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    editText.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void saveTrip() {
        String name = etTripName.getText().toString().trim();
        String start = etStartDate.getText().toString().trim();
        String end = etEndDate.getText().toString().trim();
        String dest = etDestination.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        String participants = etParticipants.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            etTripName.setError(getString(R.string.error_trip_name_required));
            return;
        }
        if (TextUtils.isEmpty(start)) {
            etStartDate.setError(getString(R.string.error_start_date_required));
            return;
        }
        if (TextUtils.isEmpty(end)) {
            etEndDate.setError(getString(R.string.error_end_date_required));
            return;
        }
        if (TextUtils.isEmpty(dest)) {
            etDestination.setError(getString(R.string.error_destination_required));
            return;
        }

        // Check date validity
        if (start.compareTo(end) > 0) {
            etEndDate.setError(getString(R.string.error_end_before_start));
            return;
        }

        // Check for overlapping trips (excluding current trip)
        int userId = UserSessionManager.getCurrentUserId(getContext());
        if (userId == -1) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!databaseHelper.isDateRangeAvailable(userId, start, end, (int) tripId)) {
            Toast.makeText(getContext(), getString(R.string.toast_trip_date_conflict), Toast.LENGTH_LONG).show();
            return;
        }

        // Update Trip Object
        trip.setTripName(name);
        trip.setStartDate(start);
        trip.setEndDate(end);
        trip.setDestination(dest);
        trip.setNotes(notes);
        trip.setParticipants(participants);
        trip.setIsGroupTrip(!TextUtils.isEmpty(participants) ? 1 : 0);

        try {
            trip.setBudget(Double.parseDouble(budgetStr));
        } catch (NumberFormatException e) {
            trip.setBudget(0.0);
        }

        // Update currency from spinner
        budgetCurrency = spinnerBudgetCurrency.getSelectedItem().toString();
        trip.setBudgetCurrency(budgetCurrency);

        // Update Geolocation in background
        btnSave.setEnabled(false);
        btnSave.setText(R.string.saving);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            double lat = trip.getMapLatitude();
            double lon = trip.getMapLongitude();

            // Only re-geocode if destination changed or no coords exist
            // Simple check: if destination string changed (we don't have old dest here
            // easily unless we stored it,
            // but re-fetching is safer to ensure accuracy)
            if (dest != null && !dest.isEmpty()) {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(dest, 1);
                    if ((addresses == null || addresses.isEmpty()) && !dest.toLowerCase().contains("vietnam")) {
                        addresses = geocoder.getFromLocationName(dest + ", Vietnam", 1);
                    }

                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        lat = address.getLatitude();
                        lon = address.getLongitude();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            double finalLat = lat;
            double finalLon = lon;

            handler.post(() -> {
                trip.setMapLatitude(finalLat);
                trip.setMapLongitude(finalLon);
                trip.setUpdatedAt(System.currentTimeMillis());

                // Update photo URL if changed
                if (newPhotoUrl != null) {
                    trip.setPhotoUrl(newPhotoUrl);
                }

                int result = databaseHelper.updateTrip(trip);

                if (result > 0) {
                    Toast.makeText(getContext(), getString(R.string.toast_trip_updated_success), Toast.LENGTH_SHORT)
                            .show();
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(), getString(R.string.toast_failed_update_trip), Toast.LENGTH_SHORT)
                            .show();
                    btnSave.setEnabled(true);
                    btnSave.setText(R.string.save_changes);
                }
            });
        });
    }

    /**
     * Detect default currency based on user's language/locale
     */
    private void detectDefaultCurrency() {
        String language = com.example.voyagerbuds.utils.LocaleHelper.getLanguage(requireContext());
        if ("vi".equals(language)) {
            budgetCurrency = "VND";
            spinnerBudgetCurrency.setSelection(1); // VND
        } else {
            budgetCurrency = "USD";
            spinnerBudgetCurrency.setSelection(0); // USD
        }
    }

    /**
     * Update currency spinner selection
     */
    private void updateCurrencySpinner() {
        if (spinnerBudgetCurrency != null && budgetCurrency != null) {
            if ("VND".equals(budgetCurrency)) {
                spinnerBudgetCurrency.setSelection(1);
            } else {
                spinnerBudgetCurrency.setSelection(0);
            }
        }
    }
}
