package com.example.voyagerbuds.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.utils.UserSessionManager;
import com.example.voyagerbuds.utils.ImageRandomizer;
import com.example.voyagerbuds.utils.CurrencyHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private android.widget.LinearLayout layoutMostVisitedContainer;

    // UI Components - Expense Statistics
    private TextView tvTotalExpenses;

    // UI Components - Emergency Alert
    private TextView tvLastActivity;
    private TextView tvEmergencyDescription;
    private SwitchCompat switchEmergencyAlert;

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

        // Fetch exchange rate if needed
        CurrencyHelper.fetchExchangeRateIfNeeded(requireContext());
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
        layoutMostVisitedContainer = view.findViewById(R.id.layout_most_visited_container);

        // Initialize UI components - Expense Statistics
        tvTotalExpenses = view.findViewById(R.id.tv_total_expenses);

        // Initialize UI components - Emergency Alert
        tvLastActivity = view.findViewById(R.id.tv_last_activity);
        tvEmergencyDescription = view.findViewById(R.id.tv_emergency_description);
        switchEmergencyAlert = view.findViewById(R.id.switch_emergency_alert);

        // Set up emergency alert switch listener
        switchEmergencyAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveEmergencyAlertState(isChecked);
            updateLastActivityTime();
            updateEmergencyDescription(isChecked);
        });

        // Load dashboard data
        loadDashboardData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update last activity time to now since user is viewing the dashboard
        updateLastActivityTime();
        // Refresh data when returning to fragment
        loadDashboardData();
    }

    // ======================== Data Loading Methods ========================

    private void loadDashboardData() {
        loadTripStatistics();
        loadExpenseStatistics();
        loadLastActivityTime();
        loadEmergencyAlertState();
    }

    /**
     * Load and display trip statistics (Total Trips, Places Visited, Most Visited)
     */
    private void loadTripStatistics() {
        if (currentUserId == -1) {
            tvTotalTrips.setText("0");
            tvPlacesVisited.setText("0");
            layoutMostVisitedContainer.removeAllViews();
            return;
        }

        // Get all trips for current user
        List<Trip> trips = databaseHelper.getAllTrips(currentUserId);

        // Total trips count
        int totalTrips = trips.size();
        tvTotalTrips.setText(String.valueOf(totalTrips));

        // Count unique destinations (places visited) and find most visited
        Set<String> uniqueDestinations = new HashSet<>();
        Map<String, Integer> destinationCounts = new HashMap<>();

        for (Trip trip : trips) {
            String dest = trip.getDestination();
            if (dest != null && !dest.isEmpty()) {
                uniqueDestinations.add(dest);
                destinationCounts.put(dest, destinationCounts.getOrDefault(dest, 0) + 1);
            }
        }

        int placesVisited = uniqueDestinations.size();
        tvPlacesVisited.setText(String.valueOf(placesVisited));

        // Find top 3 most visited places
        List<Map.Entry<String, Integer>> sortedDestinations = new java.util.ArrayList<>(destinationCounts.entrySet());
        Collections.sort(sortedDestinations, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        layoutMostVisitedContainer.removeAllViews();
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedDestinations) {
            if (count >= 3)
                break;
            addMostVisitedPlaceView(entry.getKey(), entry.getValue());
            count++;
        }

        if (count == 0) {
            // Add placeholder if no trips
            TextView placeholder = new TextView(getContext());
            placeholder.setText("No trips yet");
            placeholder.setTextColor(getResources().getColor(R.color.text_medium));
            placeholder.setPadding(16, 16, 16, 16);
            layoutMostVisitedContainer.addView(placeholder);
        }
    }

    private void addMostVisitedPlaceView(String placeName, int visitCount) {
        // Create CardView
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(requireContext());
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(getResources().getDimension(R.dimen.card_corner_radius));
        card.setCardElevation(0);
        card.setCardBackgroundColor(getResources().getColor(R.color.card_background));

        // Create Inner Layout
        android.widget.LinearLayout innerLayout = new android.widget.LinearLayout(requireContext());
        innerLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        innerLayout.setPadding(16, 16, 16, 16);
        innerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Round corners for image (simple way using cardview wrapper)
        androidx.cardview.widget.CardView imgCard = new androidx.cardview.widget.CardView(requireContext());
        android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(120, 120); // 48dp
                                                                                                                      // approx
        cardParams.setMargins(0, 0, 16, 0);
        imgCard.setLayoutParams(cardParams);
        imgCard.setRadius(16);
        imgCard.setCardElevation(0);

        // Image (Randomized)
        android.widget.ImageView image = new android.widget.ImageView(requireContext());
        android.widget.FrameLayout.LayoutParams imgParams = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        image.setLayoutParams(imgParams);
        image.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);

        // Generate a consistent random image based on the place name hash
        int backgroundImage = ImageRandomizer.getConsistentRandomBackground(placeName.hashCode());

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image);

        try {
            Glide.with(this)
                    .load(backgroundImage)
                    .apply(options)
                    .into(image);
        } catch (Exception e) {
            image.setImageResource(backgroundImage);
        }

        imgCard.addView(image);
        innerLayout.addView(imgCard);

        // Text Container
        android.widget.LinearLayout textLayout = new android.widget.LinearLayout(requireContext());
        textLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        textLayout.setLayoutParams(
                new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(requireContext());
        title.setText(placeName);
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_dark));
        textLayout.addView(title);

        TextView subtitle = new TextView(requireContext());
        String visitText = visitCount + " "
                + (visitCount == 1 ? getString(R.string.visit_singular) : getString(R.string.visit_plural));
        subtitle.setText(visitText);
        subtitle.setTextSize(14);
        subtitle.setTextColor(getResources().getColor(R.color.text_medium));
        textLayout.addView(subtitle);

        innerLayout.addView(textLayout);

        card.addView(innerLayout);
        layoutMostVisitedContainer.addView(card);
    }

    /**
     * Load and display expense statistics
     */
    private void loadExpenseStatistics() {
        if (currentUserId == -1) {
            tvTotalExpenses.setText(CurrencyHelper.formatAmountByLanguage(requireContext(), 0, "USD") + " / " +
                    CurrencyHelper.formatAmountByLanguage(requireContext(), 0, "USD"));
            return;
        }

        List<Trip> trips = databaseHelper.getAllTrips(currentUserId);

        // Always aggregate in USD for consistent calculations
        double grandTotalSpentUSD = 0;
        double grandTotalBudgetUSD = 0;

        for (Trip trip : trips) {
            String tripCurrency = trip.getBudgetCurrency();
            if (tripCurrency == null || tripCurrency.isEmpty())
                tripCurrency = "USD";

            // Convert budget to USD
            double budgetInUSD = CurrencyHelper.convertToUSD(requireContext(), trip.getBudget(), tripCurrency);
            grandTotalBudgetUSD += budgetInUSD;

            android.util.Log.d("DashboardFragment", "Trip: " + trip.getDestination() +
                    ", Budget: " + trip.getBudget() + " " + tripCurrency +
                    " -> " + budgetInUSD + " USD");

            List<com.example.voyagerbuds.models.Expense> expenses = databaseHelper.getExpensesForTrip(trip.getTripId());

            for (com.example.voyagerbuds.models.Expense expense : expenses) {
                String currency = expense.getCurrency();
                if (currency == null || currency.isEmpty()) {
                    currency = tripCurrency;
                }

                // Convert expense to USD
                double expenseInUSD = CurrencyHelper.convertToUSD(requireContext(), expense.getAmount(), currency);
                grandTotalSpentUSD += expenseInUSD;

                android.util.Log.d("DashboardFragment", "Expense: " + expense.getCategory() +
                        ", Amount: " + expense.getAmount() + " " + currency +
                        " -> " + expenseInUSD + " USD");
            }
        }

        android.util.Log.d("DashboardFragment", "Total Spent USD: " + grandTotalSpentUSD +
                ", Total Budget USD: " + grandTotalBudgetUSD);

        // Display in USD, then formatAmountByLanguage will convert to user's preferred
        // currency based on language
        tvTotalExpenses
                .setText(CurrencyHelper.formatAmountByLanguage(requireContext(), grandTotalSpentUSD, "USD") + " / " +
                        CurrencyHelper.formatAmountByLanguage(requireContext(), grandTotalBudgetUSD, "USD"));
    }

    /**
     * Update a progress bar and cost text with percentage calculation
     * (Unused but kept for reference if needed later)
     */
    private void updateProgressBar(ProgressBar progressBar, TextView costView,
            double amount, double total) {
        // Implementation removed as it is currently unused
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
     * Load and set emergency alert switch state and settings
     */
    private void loadEmergencyAlertState() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs",
                android.content.Context.MODE_PRIVATE);

        boolean isAlertEnabled = prefs.getBoolean("emergency_alert_enabled", true);
        switchEmergencyAlert.setChecked(isAlertEnabled);

        updateEmergencyDescription(isAlertEnabled);
    }

    /**
     * Update the emergency description text based on enabled state
     */
    private void updateEmergencyDescription(boolean isEnabled) {
        if (!isEnabled) {
            tvEmergencyDescription.setText(R.string.emergency_alerts_disabled);
            tvEmergencyDescription.setTextColor(getResources().getColor(R.color.text_medium));
            return;
        }

        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs",
                android.content.Context.MODE_PRIVATE);

        // Update description based on timeout setting
        int timeoutIndex = prefs.getInt("emergency_timeout_index", 1); // Default 24h
        String timeoutString;
        switch (timeoutIndex) {
            case 0:
                timeoutString = "12h";
                break;
            case 1:
                timeoutString = "24h";
                break;
            case 2:
                timeoutString = "48h";
                break;
            case 3:
                timeoutString = "72h";
                break;
            default:
                timeoutString = "24h";
                break;
        }
        String description = getString(R.string.emergency_alert_timeout, timeoutString);
        tvEmergencyDescription.setText(description);
        tvEmergencyDescription.setTextColor(getResources().getColor(R.color.text_medium));
    }

    // ======================== Utility Methods ========================

    /**
     * Get last activity timestamp from SharedPreferences
     */
    private long getLastActivityTime() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs",
                android.content.Context.MODE_PRIVATE);
        return prefs.getLong("last_activity_time", System.currentTimeMillis());
    }

    /**
     * Update and save current activity timestamp to SharedPreferences
     */
    private void updateLastActivityTime() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs",
                android.content.Context.MODE_PRIVATE);
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
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs",
                android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean("emergency_alert_enabled", true);
    }

    /**
     * Save emergency alert state to SharedPreferences
     */
    private void saveEmergencyAlertState(boolean isEnabled) {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs",
                android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("emergency_alert_enabled", isEnabled);
        editor.apply();
    }

    /**
     * Format currency amount based on current locale
     */
    private String formatCurrency(double amount) {
        if (getContext() == null)
            return String.format("$%.0f", amount);
        return CurrencyHelper.formatCurrency(requireContext(), amount);
    }

    /**
     * Format last activity time in a user-friendly way with proper localization
     */
    private String formatLastActivityTime(long timestamp) {
        String language = com.example.voyagerbuds.utils.LocaleHelper.getLanguage(requireContext());
        java.util.Locale locale = "vi".equals(language) ? new java.util.Locale("vi", "VN") : java.util.Locale.US;

        // Use locale-specific date format
        java.text.SimpleDateFormat dateFormat;
        if ("vi".equals(language)) {
            // Vietnamese format: "06/12/2025 lúc 9:27 SA"
            dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy 'lúc' h:mm a", locale);
        } else {
            // English format: "Dec 06, 2025 at 9:27 AM"
            dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", locale);
        }

        java.util.Date date = new java.util.Date(timestamp);
        String formattedDate = dateFormat.format(date);

        // Use localized string resource
        return getString(R.string.last_activity, formattedDate);
    }
}