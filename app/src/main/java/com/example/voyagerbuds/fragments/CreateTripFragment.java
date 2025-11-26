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
    private String budgetCurrency;

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
        showTripNameFragment(false);

        return view;
    }

    private void showTripNameFragment(boolean isBack) {
        currentStep = 1;
        updateProgress();

        TripNameFragment fragment = new TripNameFragment();
        fragment.setListener(name -> {
            tripName = name;
            showTripDatesFragment(false);
        });

        if (tripName != null) {
            fragment.setTripName(tripName);
        }

        replaceChildFragment(fragment, isBack);
    }

    private void showTripDatesFragment(boolean isBack) {
        currentStep = 2;
        updateProgress();

        TripDatesFragment fragment = new TripDatesFragment();
        fragment.setListener(new TripDatesFragment.OnTripDatesEnteredListener() {
            @Override
            public void onTripDatesEntered(String start, String end) {
                startDate = start;
                endDate = end;
                showTripDestinationFragment(false);
            }

            @Override
            public void onBack() {
                showTripNameFragment(true);
            }
        });

        if (startDate != null && endDate != null) {
            fragment.setDates(startDate, endDate);
        }

        replaceChildFragment(fragment, isBack);
    }

    private void showTripDestinationFragment(boolean isBack) {
        currentStep = 3;
        updateProgress();

        TripDestinationFragment fragment = new TripDestinationFragment();
        fragment.setListener(new TripDestinationFragment.OnTripDestinationEnteredListener() {
            @Override
            public void onTripDestinationEntered(String dest, String tripNotes, String friendList,
                    String budgetAmount, String currency) {
                destination = dest;
                notes = tripNotes;
                friends = friendList;
                budget = budgetAmount;
                budgetCurrency = currency;
                saveTrip();
            }

            @Override
            public void onBack() {
                showTripDatesFragment(true);
            }
        });

        if (destination != null) {
            fragment.setDestination(destination, notes, friends, budget, budgetCurrency);
        }

        replaceChildFragment(fragment, isBack);
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
                // Get the current logged-in user's ID from Firebase
                int userId = com.example.voyagerbuds.utils.UserSessionManager.getCurrentUserId(requireContext());
                if (userId == -1) {
                    if (progressBar != null)
                        progressBar.setIndeterminate(false);
                    Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
                    return;
                }

                Trip trip = new Trip();
                trip.setUserId(userId);
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
                        trip.setBudgetCurrency(budgetCurrency != null ? budgetCurrency : "USD");
                    } catch (NumberFormatException e) {
                        trip.setBudget(0.0);
                        trip.setBudgetCurrency("USD");
                    }
                } else {
                    trip.setBudgetCurrency(budgetCurrency != null ? budgetCurrency : "USD");
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

    private void replaceChildFragment(Fragment fragment, boolean isBack) {
        int enterAnim = isBack ? R.anim.slide_in_left : R.anim.slide_in_right;
        int exitAnim = isBack ? R.anim.slide_out_right : R.anim.slide_out_left;

        getChildFragmentManager().beginTransaction()
                .setCustomAnimations(enterAnim, exitAnim)
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
                showTripNameFragment(true);
            } else if (currentStep == 3) {
                showTripDatesFragment(true);
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
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    if (listener != null) {
                        listener.onCancelTripCreation();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    public boolean isTripCreationComplete() {
        return isTripCreationComplete;
    }
}
