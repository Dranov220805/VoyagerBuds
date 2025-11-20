package com.example.voyagerbuds.fragments;

import android.os.Bundle;
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
            public void onTripDestinationEntered(String dest, String tripNotes, String friendList, String budgetAmount) {
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
        Trip trip = new Trip();
        trip.setUserId(1); // TODO: Get from logged-in user
        trip.setTripName(tripName);
        trip.setStartDate(startDate);
        trip.setEndDate(endDate);
        trip.setDestination(destination);
        
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

        if (result > 0) {
            isTripCreationComplete = true;
            Toast.makeText(getContext(), R.string.trip_created, Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onTripCreated(result);
            }
        } else {
            Toast.makeText(getContext(), R.string.trip_creation_failed, Toast.LENGTH_SHORT).show();
        }
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
