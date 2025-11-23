package com.example.voyagerbuds.fragments;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.fragments.createtrip.TripDatesFragment;
import com.example.voyagerbuds.fragments.createtrip.TripDestinationFragment;
import com.example.voyagerbuds.fragments.createtrip.TripNameFragment;
import com.example.voyagerbuds.models.Trip;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateTripFragment extends Fragment {

    private DatabaseHelper databaseHelper;
    private ProgressBar progressBar;
    private TextView tvProgress;

    // Trip data
    private String tripName;
    private String startDate;
    private String endDate;
    private String destination;
    private String notes;
    private String friends;
    private String budget;

    private int currentStep = 1;
    private static final int TOTAL_STEPS = 3;
    private boolean isTripCreationComplete = false;

    public interface OnTripCreatedListener {
        void onTripCreated(long tripId);

        void onCancelTripCreation();
    }

    private OnTripCreatedListener listener;

    public void setListener(OnTripCreatedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_trip, container, false);

        databaseHelper = new DatabaseHelper(getContext());
        progressBar = view.findViewById(R.id.progress_bar);
        tvProgress = view.findViewById(R.id.tv_progress);

        progressBar.setMax(TOTAL_STEPS);
        updateProgress();

        // Show first fragment
        showTripNameFragment();

        return view;
    }

    private void showTripNameFragment() {
        currentStep = 1;
        updateProgress();

        TripNameFragment fragment = new TripNameFragment();
        fragment.setListener(name -> {
            tripName = name;
            showTripDatesFragment();
        });

        if (tripName != null) {
            fragment.setTripName(tripName);
        }

        replaceChildFragment(fragment);
    }

    private void showTripDatesFragment() {
        currentStep = 2;
        updateProgress();

        TripDatesFragment fragment = new TripDatesFragment();
        fragment.setListener(new TripDatesFragment.OnTripDatesEnteredListener() {
            @Override
            public void onTripDatesEntered(String start, String end) {
                startDate = start;
                endDate = end;
                showTripDestinationFragment();
            }

            @Override
            public void onBack() {
                showTripNameFragment();
            }
        });

        if (startDate != null && endDate != null) {
            fragment.setDates(startDate, endDate);
        }

        replaceChildFragment(fragment);
    }

    private void showTripDestinationFragment() {
        currentStep = 3;
        updateProgress();

        TripDestinationFragment fragment = new TripDestinationFragment();
        fragment.setListener(new TripDestinationFragment.OnTripDestinationEnteredListener() {
            @Override
            public void onTripDestinationEntered(String dest, String tripNotes, String friendList,
                    String budgetAmount) {
                destination = dest;
                notes = tripNotes;
                friends = friendList;
                budget = budgetAmount;
                saveTrip();
            }

            @Override
            public void onBack() {
                showTripDatesFragment();
            }
        });

        if (destination != null) {
            fragment.setDestination(destination, notes, friends, budget);
        }

        replaceChildFragment(fragment);
    }

    private void saveTrip() {
        // Show loading or disable UI while saving
        if (progressBar != null)
            progressBar.setIndeterminate(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            double lat = 0.0;
            double lon = 0.0;

            if (destination != null && !destination.isEmpty()) {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                try {
                    // Try to find the location
                    List<Address> addresses = geocoder.getFromLocationName(destination, 1);

                    // If not found, and doesn't contain "Vietnam", try appending it
                    if ((addresses == null || addresses.isEmpty()) && !destination.toLowerCase().contains("vietnam")) {
                        addresses = geocoder.getFromLocationName(destination + ", Vietnam", 1);
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
                Trip trip = new Trip();
                trip.setUserId(1); // TODO: Get from logged-in user
                trip.setTripName(tripName);
                trip.setStartDate(startDate);
                trip.setEndDate(endDate);
                trip.setDestination(destination);
                trip.setNotes(notes);

                // Set coordinates
                trip.setMapLatitude(finalLat);
                trip.setMapLongitude(finalLon);

                if (friends != null && !friends.isEmpty()) {
                    trip.setParticipants(friends);
                    trip.setIsGroupTrip(1);
                }

                if (budget != null && !budget.isEmpty()) {
                    try {
                        trip.setBudget(Double.parseDouble(budget));
                    } catch (NumberFormatException e) {
                        trip.setBudget(0.0);
                    }
                }

                trip.setCreatedAt(System.currentTimeMillis());
                trip.setUpdatedAt(System.currentTimeMillis());
                trip.setSyncStatus("pending");

                long result = databaseHelper.addTrip(trip);

                if (progressBar != null)
                    progressBar.setIndeterminate(false);

                if (result > 0) {
                    isTripCreationComplete = true;
                    Toast.makeText(getContext(), R.string.trip_created, Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onTripCreated(result);
                    }
                } else {
                    Toast.makeText(getContext(), R.string.trip_creation_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void replaceChildFragment(Fragment fragment) {
        getChildFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.create_trip_content_container, fragment)
                .commit();
    }

    private void updateProgress() {
        if (progressBar != null && tvProgress != null) {
            progressBar.setProgress(currentStep);
            tvProgress.setText(getString(R.string.step_progress, currentStep, TOTAL_STEPS));
        }
    }

    public boolean handleBackPress() {
        if (isTripCreationComplete) {
            return false; // Allow normal back behavior
        }

        if (currentStep > 1) {
            if (currentStep == 2) {
                showTripNameFragment();
            } else if (currentStep == 3) {
                showTripDatesFragment();
            }
            return true; // Back press handled
        } else {
            // Show confirmation dialog when trying to exit from first step
            showConfirmationDialog();
            return true; // Back press handled
        }
    }

    public void showConfirmationDialog() {
        if (isTripCreationComplete) {
            if (listener != null) {
                listener.onCancelTripCreation();
            }
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.discard_trip_title)
                .setMessage(R.string.discard_trip_message)
                .setPositiveButton(R.string.discard, (dialog, which) -> {
                    if (listener != null) {
                        listener.onCancelTripCreation();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public boolean isTripCreationComplete() {
        return isTripCreationComplete;
    }
}
