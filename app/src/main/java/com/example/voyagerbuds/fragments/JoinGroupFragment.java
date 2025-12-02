package com.example.voyagerbuds.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.utils.UserSessionManager;

/**
 * Fragment for joining group trips using trip codes.
 * Users can enter a trip code to find and join existing group trips.
 */
public class JoinGroupFragment extends Fragment {

    private DatabaseHelper databaseHelper;
    private int currentUserId = -1;

    // UI Components - Input
    private EditText etTripCode;
    private Button btnSearchTrip;

    // UI Components - Results
    private TextView tvResultsTitle;
    private CardView cardTripResult;
    private TextView tvResultTripName;
    private TextView tvResultDestination;
    private TextView tvResultDates;
    private TextView tvResultMembers;
    private Button btnJoinTrip;

    // UI Components - Error/Empty
    private TextView tvErrorMessage;
    private LinearLayout layoutEmptyState;

    // Current trip data
    private Trip foundTrip;

    public JoinGroupFragment() {
        // Required empty public constructor
    }

    public static JoinGroupFragment newInstance() {
        return new JoinGroupFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(getContext());

        // Get current user ID from session manager
        currentUserId = UserSessionManager.getCurrentUserId(requireContext());
        if (currentUserId == -1) {
            currentUserId = 1; // Default user ID for testing
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_join_group, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components - Input
        etTripCode = view.findViewById(R.id.et_trip_code);
        btnSearchTrip = view.findViewById(R.id.btn_search_trip);

        // Initialize UI components - Results
        tvResultsTitle = view.findViewById(R.id.tv_results_title);
        cardTripResult = view.findViewById(R.id.card_trip_result);
        tvResultTripName = view.findViewById(R.id.tv_result_trip_name);
        tvResultDestination = view.findViewById(R.id.tv_result_destination);
        tvResultDates = view.findViewById(R.id.tv_result_dates);
        tvResultMembers = view.findViewById(R.id.tv_result_members);
        btnJoinTrip = view.findViewById(R.id.btn_join_trip);

        // Initialize UI components - Error/Empty
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        // Set up click listeners
        btnSearchTrip.setOnClickListener(v -> searchTripByCode());
        btnJoinTrip.setOnClickListener(v -> joinTrip());
    }

    /**
     * Search for a trip by code
     */
    private void searchTripByCode() {
        String code = etTripCode.getText().toString().trim().toUpperCase();

        if (TextUtils.isEmpty(code)) {
            Toast.makeText(getContext(), "Please enter a trip code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract trip ID from code (format: TRIPNAME_TRIPID_HASH)
        String[] parts = code.split("_");
        if (parts.length < 2) {
            showError("Invalid trip code format");
            return;
        }

        try {
            int tripId = Integer.parseInt(parts[1]);
            Trip trip = databaseHelper.getTripById(tripId);

            if (trip != null && trip.getIsGroupTrip() == 1) {
                // Verify user is not already in this trip
                if (isUserAlreadyInTrip(trip)) {
                    showError("You are already part of this group trip");
                    return;
                }

                // Display the trip
                foundTrip = trip;
                displayTripResult(trip);
            } else {
                showError("Trip not found or is not a group trip");
            }
        } catch (NumberFormatException e) {
            showError("Invalid trip code format");
        }
    }

    /**
     * Check if current user is already in this trip's participants
     */
    private boolean isUserAlreadyInTrip(Trip trip) {
        String participants = trip.getParticipants();
        if (participants == null || participants.isEmpty()) {
            return false;
        }
        // Check if user's name is in participants (simple check)
        // In a real app, you'd want to store user IDs instead of names
        return participants.contains(String.valueOf(currentUserId));
    }

    /**
     * Display the found trip in the result card
     */
    private void displayTripResult(Trip trip) {
        tvResultsTitle.setVisibility(View.VISIBLE);
        cardTripResult.setVisibility(View.VISIBLE);
        tvErrorMessage.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);

        // Display trip information
        tvResultTripName.setText(trip.getTripName());
        tvResultDestination.setText(trip.getDestination() != null ? trip.getDestination() : "Unknown");
        tvResultDates.setText(trip.getStartDate() + " to " + trip.getEndDate());

        // Calculate member count
        int memberCount = countParticipants(trip.getParticipants());
        String memberText = memberCount + " " + (memberCount == 1 ? "member" : "members");
        tvResultMembers.setText(memberText);
    }

    /**
     * Count participants from comma-separated string
     */
    private int countParticipants(String participants) {
        if (participants == null || participants.isEmpty()) {
            return 1; // At least the organizer
        }
        String[] parts = participants.split(",");
        return parts.length + 1; // +1 for organizer
    }

    /**
     * Join the found trip
     */
    private void joinTrip() {
        if (foundTrip == null) {
            Toast.makeText(getContext(), "No trip selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add current user to participants
        String currentParticipants = foundTrip.getParticipants();
        String userIdentifier = "User_" + currentUserId; // Simple identifier using user ID

        String updatedParticipants;
        if (currentParticipants == null || currentParticipants.isEmpty()) {
            updatedParticipants = userIdentifier;
        } else {
            updatedParticipants = currentParticipants + "," + userIdentifier;
        }

        // Update the trip in database
        foundTrip.setParticipants(updatedParticipants);
        foundTrip.setUpdatedAt(System.currentTimeMillis());

        int rowsUpdated = databaseHelper.updateTrip(foundTrip);
        boolean success = rowsUpdated > 0;

        if (success) {
            Toast.makeText(getContext(), "Successfully joined the group trip!", Toast.LENGTH_SHORT).show();

            // Navigate back to dashboard or trip detail
            navigateBack();
        } else {
            Toast.makeText(getContext(), "Failed to join trip. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        tvResultsTitle.setVisibility(View.GONE);
        cardTripResult.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        tvErrorMessage.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
    }

    /**
     * Navigate back to dashboard
     */
    private void navigateBack() {
        DashboardFragment dashboardFragment = new DashboardFragment();
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right,
                        R.anim.slide_in_right,
                        R.anim.slide_out_left)
                .replace(R.id.content_container, dashboardFragment)
                .commit();
    }
}
