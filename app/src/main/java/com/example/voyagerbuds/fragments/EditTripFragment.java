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

public class EditTripFragment extends Fragment {

    private static final String ARG_TRIP_ID = "trip_id";
    private long tripId;
    private Trip trip;

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
    private boolean isUpdateComplete = false;

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

        // Load existing trip data
        trip = databaseHelper.getTripById((int) tripId);
        if (trip != null) {
            tripName = trip.getTripName();
            startDate = trip.getStartDate();
            endDate = trip.getEndDate();
            destination = trip.getDestination();
            notes = trip.getNotes();
            friends = trip.getParticipants();
            budget = trip.getBudget() > 0 ? String.valueOf(trip.getBudget()) : "";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_trip, container, false);

        progressBar = view.findViewById(R.id.progress_bar);
        tvProgress = view.findViewById(R.id.tv_progress);

        progressBar.setMax(TOTAL_STEPS);
        updateProgress();

        // Show first fragment with existing data
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
                updateTrip();
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

    private void updateTrip() {
        if (trip == null) {
            Toast.makeText(getContext(), "Trip not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update trip object with new values
        trip.setTripName(tripName);
        trip.setStartDate(startDate);
        trip.setEndDate(endDate);
        trip.setDestination(destination);
        trip.setNotes(notes);

        if (friends != null && !friends.isEmpty()) {
            trip.setParticipants(friends);
            trip.setIsGroupTrip(1);
        } else {
            trip.setParticipants(null);
            trip.setIsGroupTrip(0);
        }

        if (budget != null && !budget.isEmpty()) {
            try {
                trip.setBudget(Double.parseDouble(budget));
            } catch (NumberFormatException e) {
                trip.setBudget(0.0);
            }
        } else {
            trip.setBudget(0.0);
        }

        trip.setUpdatedAt(System.currentTimeMillis());

        int result = databaseHelper.updateTrip(trip);

        if (result > 0) {
            isUpdateComplete = true;
            Toast.makeText(getContext(), "Trip updated successfully", Toast.LENGTH_SHORT).show();

            // Navigate back to trip detail
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        } else {
            Toast.makeText(getContext(), "Failed to update trip", Toast.LENGTH_SHORT).show();
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
        if (isUpdateComplete) {
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

    private void showConfirmationDialog() {
        if (isUpdateComplete) {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Discard Changes?")
                .setMessage("Are you sure you want to discard your changes?")
                .setPositiveButton("Discard", (dialog, which) -> {
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
