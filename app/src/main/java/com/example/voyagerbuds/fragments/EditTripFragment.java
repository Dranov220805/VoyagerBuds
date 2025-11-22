package com.example.voyagerbuds.fragments;

import android.app.DatePickerDialog;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;
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
    private Button btnSave;

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

        // Load Trip Data
        loadTripData();

        // Setup Listeners
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        btnSave.setOnClickListener(v -> saveTrip());

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
        } else {
            Toast.makeText(getContext(), "Error loading trip", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
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
            etTripName.setError("Trip name is required");
            return;
        }
        if (TextUtils.isEmpty(start)) {
            etStartDate.setError("Start date is required");
            return;
        }
        if (TextUtils.isEmpty(end)) {
            etEndDate.setError("End date is required");
            return;
        }
        if (TextUtils.isEmpty(dest)) {
            etDestination.setError("Destination is required");
            return;
        }

        // Check date validity
        if (start.compareTo(end) > 0) {
            etEndDate.setError("End date cannot be before start date");
            return;
        }

        // Check for overlapping trips (excluding current trip)
        if (!databaseHelper.isDateRangeAvailable(start, end, (int) tripId)) {
            Toast.makeText(getContext(), "You already have a trip during these dates", Toast.LENGTH_LONG).show();
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

        // Update Geolocation in background
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

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

                int result = databaseHelper.updateTrip(trip);

                if (result > 0) {
                    Toast.makeText(getContext(), "Trip updated successfully", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(), "Failed to update trip", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                }
            });
        });
    }
}
