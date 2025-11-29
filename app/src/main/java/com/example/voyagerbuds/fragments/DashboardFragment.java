package com.example.voyagerbuds.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.utils.UserSessionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DashboardFragment displays travel analytics and statistics.
 * Shows trip count, places visited, expense breakdown, and emergency alerts.
 */
public class DashboardFragment extends Fragment {

    private DatabaseHelper databaseHelper;
    private int currentUserId = -1;

    // UI Components - Trip Statistics
    private TextView tvTotalTrips;
    private TextView tvPlacesVisited;

    // UI Components - Expense Statistics
    private TextView tvTotalExpenses;
    private ProgressBar pbTransport;
    private ProgressBar pbAccommodation;
    private ProgressBar pbFood;
    private TextView tvTransportCost;
    private TextView tvAccommodationCost;
    private TextView tvFoodCost;

    // UI Components - Emergency Alert
    private TextView tvLastActivity;
    private SwitchCompat switchEmergencyAlert;

    // UI Components - Group Trips
    private TextView tvGroupTripTitle;
    private TextView tvGroupTripMembers;
    private android.widget.Button btnViewGroup;
    private android.widget.Button btnCreateGroup;
    private android.widget.Button btnJoinGroup;
    private android.view.View groupTripCard;
    private Trip currentGroupTrip;

    public DashboardFragment() {
        // Required empty public constructor
    }

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
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
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components - Trip Statistics
        tvTotalTrips = view.findViewById(R.id.tv_total_trips);
        tvPlacesVisited = view.findViewById(R.id.tv_places_visited);
        
        // Initialize UI components - Expense Statistics
        tvTotalExpenses = view.findViewById(R.id.tv_total_expenses);
        pbTransport = view.findViewById(R.id.pb_transport);
        pbAccommodation = view.findViewById(R.id.pb_accommodation);
        pbFood = view.findViewById(R.id.pb_food);
        tvTransportCost = view.findViewById(R.id.tv_transport_cost);
        tvAccommodationCost = view.findViewById(R.id.tv_accommodation_cost);
        tvFoodCost = view.findViewById(R.id.tv_food_cost);
        
        // Initialize UI components - Emergency Alert
        tvLastActivity = view.findViewById(R.id.tv_last_activity);
        switchEmergencyAlert = view.findViewById(R.id.switch_emergency_alert);
        
        // Set up emergency alert switch listener
        switchEmergencyAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveEmergencyAlertState(isChecked);
            updateLastActivityTime();
        });
        
        // Initialize UI components - Group Trips
        groupTripCard = view.findViewById(R.id.group_trip_card);
        tvGroupTripTitle = view.findViewById(R.id.tv_group_trip_title);
        tvGroupTripMembers = view.findViewById(R.id.tv_group_trip_members);
        btnViewGroup = view.findViewById(R.id.btn_view_group);
        btnCreateGroup = view.findViewById(R.id.btn_create_group);
        btnJoinGroup = view.findViewById(R.id.btn_join_group);
        
        // Set up group trips click listeners
        if (btnViewGroup != null) {
            btnViewGroup.setOnClickListener(v -> navigateToGroupTrip());
        }
        if (btnCreateGroup != null) {
            btnCreateGroup.setOnClickListener(v -> navigateToCreateTrip());
        }
        if (btnJoinGroup != null) {
            btnJoinGroup.setOnClickListener(v -> showJoinGroupDialog());
        }
        
        // Load dashboard data
        loadDashboardData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to fragment
        loadDashboardData();
    }

    // ======================== Data Loading Methods ========================

    private void loadDashboardData() {
        loadTripStatistics();
        loadExpenseStatistics();
        loadLastActivityTime();
        loadEmergencyAlertState();
        loadGroupTrips();
    }

    /**
     * Load and display trip statistics (Total Trips, Places Visited)
     */
    private void loadTripStatistics() {
        if (currentUserId == -1) {
            tvTotalTrips.setText("0");
            tvPlacesVisited.setText("0");
            return;
        }

        // Get all trips for current user
        List<Trip> trips = databaseHelper.getAllTrips(currentUserId);
        
        // Total trips count
        int totalTrips = trips.size();
        tvTotalTrips.setText(String.valueOf(totalTrips));
        
        // Count unique destinations (places visited)
        Set<String> uniqueDestinations = new HashSet<>();
        for (Trip trip : trips) {
            if (trip.getDestination() != null && !trip.getDestination().isEmpty()) {
                uniqueDestinations.add(trip.getDestination());
            }
        }
        int placesVisited = uniqueDestinations.size();
        tvPlacesVisited.setText(String.valueOf(placesVisited));
    }

    /**
     * Load and display expense statistics
     */
    private void loadExpenseStatistics() {
        if (currentUserId == -1) {
            tvTotalExpenses.setText("$0");
            pbTransport.setProgress(0);
            pbAccommodation.setProgress(0);
            pbFood.setProgress(0);
            return;
        }

        List<Trip> trips = databaseHelper.getAllTrips(currentUserId);
        if (trips.isEmpty()) {
            tvTotalExpenses.setText("$0");
            pbTransport.setProgress(0);
            pbAccommodation.setProgress(0);
            pbFood.setProgress(0);
            return;
        }

        // Calculate total expenses and category breakdown
        double totalTransport = 0;
        double totalAccommodation = 0;
        double totalFood = 0;
        double grandTotal = 0;

        for (Trip trip : trips) {
            List<com.example.voyagerbuds.models.Expense> expenses = 
                databaseHelper.getExpensesForTrip(trip.getTripId());
            
            for (com.example.voyagerbuds.models.Expense expense : expenses) {
                double amount = expense.getAmount();
                grandTotal += amount;
                
                String category = expense.getCategory();
                if (category != null) {
                    switch (category.toLowerCase()) {
                        case "transport":
                        case "transportation":
                            totalTransport += amount;
                            break;
                        case "accommodation":
                        case "hotel":
                        case "lodging":
                            totalAccommodation += amount;
                            break;
                        case "food":
                        case "dining":
                        case "restaurant":
                            totalFood += amount;
                            break;
                    }
                }
            }
        }

        // Update total expenses display
        tvTotalExpenses.setText(formatCurrency(grandTotal));

        // Update progress bars with percentages
        updateProgressBar(pbTransport, tvTransportCost, totalTransport, grandTotal);
        updateProgressBar(pbAccommodation, tvAccommodationCost, totalAccommodation, grandTotal);
        updateProgressBar(pbFood, tvFoodCost, totalFood, grandTotal);
    }

    /**
     * Update a progress bar and cost text with percentage calculation
     */
    private void updateProgressBar(ProgressBar progressBar, TextView costView, 
                                   double amount, double total) {
        if (total == 0) {
            progressBar.setProgress(0);
            costView.setText("$0");
        } else {
            int percentage = (int) ((amount / total) * 100);
            progressBar.setProgress(percentage);
            costView.setText(formatCurrency(amount));
        }
    }

    /**
     * Load and display last activity time
     */
    private void loadLastActivityTime() {
        // Get last activity from SharedPreferences
        long lastActivityTime = getLastActivityTime();
        String formattedTime = formatLastActivityTime(lastActivityTime);
        tvLastActivity.setText(formattedTime);
    }

    /**
     * Load and set emergency alert switch state
     */
    private void loadEmergencyAlertState() {
        boolean isAlertEnabled = isEmergencyAlertEnabled();
        switchEmergencyAlert.setChecked(isAlertEnabled);
    }

    /**
     * Load and display group trips
     */
    private void loadGroupTrips() {
        if (currentUserId == -1) {
            if (groupTripCard != null) {
                groupTripCard.setVisibility(android.view.View.GONE);
            }
            return;
        }

        List<Trip> trips = databaseHelper.getAllTrips(currentUserId);
        
        // Filter group trips where is_group_trip = 1
        Trip firstGroupTrip = null;
        for (Trip trip : trips) {
            if (trip.getIsGroupTrip() == 1) {
                firstGroupTrip = trip;
                break;
            }
        }

        if (firstGroupTrip == null) {
            // No group trips, hide the card
            if (groupTripCard != null) {
                groupTripCard.setVisibility(android.view.View.GONE);
            }
        } else {
            // Display group trip card
            if (groupTripCard != null) {
                groupTripCard.setVisibility(android.view.View.VISIBLE);
            }
            
            if (tvGroupTripTitle != null) {
                tvGroupTripTitle.setText(firstGroupTrip.getTripName());
            }
            
            // Parse participants and count members
            int memberCount = countParticipants(firstGroupTrip.getParticipants());
            int tripDuration = calculateTripDuration(firstGroupTrip.getStartDate(), firstGroupTrip.getEndDate());
            
            String memberText = memberCount + " " + (memberCount == 1 ? "member" : "members") + " • " + tripDuration + " " + (tripDuration == 1 ? "day" : "days");
            if (tvGroupTripMembers != null) {
                tvGroupTripMembers.setText(memberText);
            }
            
            // Store reference to current group trip for navigation
            currentGroupTrip = firstGroupTrip;
        }
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
     * Calculate trip duration in days
     */
    private int calculateTripDuration(String startDate, String endDate) {
        if (startDate == null || endDate == null) {
            return 1;
        }
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date start = dateFormat.parse(startDate);
            java.util.Date end = dateFormat.parse(endDate);
            
            if (start != null && end != null) {
                long differenceInTime = end.getTime() - start.getTime();
                long differenceInDays = differenceInTime / (1000 * 60 * 60 * 24) + 1; // +1 to include both start and end dates
                return (int) Math.max(1, differenceInDays);
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Navigate to group trip detail
     */
    private void navigateToGroupTrip() {
        if (currentGroupTrip != null) {
            // Show trip code for sharing
            showTripCodeDialog(currentGroupTrip);
        }
    }

    /**
     * Show trip code dialog for sharing the group trip
     */
    private void showTripCodeDialog(Trip trip) {
        com.example.voyagerbuds.database.dao.TripDao tripDao = new com.example.voyagerbuds.database.dao.TripDao(null);
        String tripCode = tripDao.generateTripCode(trip.getTripName(), (int) trip.getTripId());

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Trip Code")
                .setMessage("Share this code with friends:\n\n" + tripCode)
                .setPositiveButton("Copy to Clipboard", (dialog, which) -> {
                    copyToClipboard(tripCode);
                    Toast.makeText(getContext(), "Code copied to clipboard!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Navigate to trip detail
                    TripDetailFragment fragment = TripDetailFragment.newInstance(trip.getTripId());
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right)
                            .replace(R.id.content_container, fragment)
                            .addToBackStack(null)
                            .commit();
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Copy text to clipboard
     */
    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Trip Code", text);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Navigate to create trip screen
     */
    private void navigateToCreateTrip() {
        if (getActivity() instanceof com.example.voyagerbuds.activities.HomeActivity) {
            ((com.example.voyagerbuds.activities.HomeActivity) getActivity()).showCreateTripFragment();
        }
    }

    /**
     * Show join group dialog (placeholder for future implementation)
     */
    private void showJoinGroupDialog() {
        JoinGroupFragment joinFragment = new JoinGroupFragment();
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right)
                .replace(R.id.content_container, joinFragment)
                .addToBackStack(null)
                .commit();
    }

    // ======================== Utility Methods ========================

    /**
     * Get last activity timestamp from SharedPreferences
     */
    private long getLastActivityTime() {
        android.content.SharedPreferences prefs = 
            requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        return prefs.getLong("last_activity_time", System.currentTimeMillis());
    }

    /**
     * Update and save current activity timestamp to SharedPreferences
     */
    private void updateLastActivityTime() {
        android.content.SharedPreferences prefs = 
            requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_activity_time", System.currentTimeMillis());
        editor.apply();
        
        // Refresh the display
        loadLastActivityTime();
    }

    /**
     * Check if emergency alert is enabled
     */
    private boolean isEmergencyAlertEnabled() {
        android.content.SharedPreferences prefs = 
            requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean("emergency_alert_enabled", true);
    }

    /**
     * Save emergency alert state to SharedPreferences
     */
    private void saveEmergencyAlertState(boolean isEnabled) {
        android.content.SharedPreferences prefs = 
            requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("emergency_alert_enabled", isEnabled);
        editor.apply();
    }

    /**
     * Format currency amount
     */
    private String formatCurrency(double amount) {
        return String.format("$%.0f", amount);
    }

    /**
     * Format last activity time in a user-friendly way
     */
    private String formatLastActivityTime(long timestamp) {
        java.text.SimpleDateFormat dateFormat = 
            new java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault());
        java.util.Date date = new java.util.Date(timestamp);
        return "Last Activity: " + dateFormat.format(date);
    }
}