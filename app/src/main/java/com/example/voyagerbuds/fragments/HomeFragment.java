package com.example.voyagerbuds.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.example.voyagerbuds.utils.DateUtils;
import com.example.voyagerbuds.utils.CurrencyHelper;
import com.example.voyagerbuds.utils.ImageRandomizer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;
import com.example.voyagerbuds.adapters.TripAdapter;
import com.example.voyagerbuds.adapters.TripCardAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment
        implements TripAdapter.OnTripClickListener, TripCardAdapter.OnTripClickListener {

    // private RecyclerView recyclerViewTrips; // Removed
    // private TripAdapter tripAdapter; // Removed
    private DatabaseHelper databaseHelper;
    private List<Trip> tripList;
    // private List<Trip> filteredTripList; // Removed, using specific lists
    private View emptyStateView;
    private ImageView emptyLogo;

    // Hero card views
    private Trip displayedHeroTrip;
    private CardView cardHeroTrip;
    private ImageView imgHeroTrip;
    private TextView tvHeroTripName;
    private TextView tvHeroTripDates;
    private TextView tvHeroTripLocation;
    private TextView tvHeroTripNotes;
    private TextView tvHeroTripSchedule;
    private TextView tvHeroTripExpenses;
    private TextView tvCurrentTripHeader;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddTrip;
    // Network icon and connectivity
    private ImageView networkIcon;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // New Sections
    private RecyclerView recyclerViewUpcomingTrips;
    private TripCardAdapter upcomingTripAdapter;
    private List<Trip> upcomingTripList;
    private TextView tvUpcomingTripsHeader;

    private RecyclerView recyclerViewPastTrips;
    private TripCardAdapter pastTripAdapter;
    private List<Trip> pastTripList;
    private TextView tvPastTripsHeader;

    // Receiver used to detect device date/time changes and refresh trips
    // automatically
    private BroadcastReceiver dateChangeReceiver;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(getContext());
        // location fetching removed to avoid background geocoding
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        emptyStateView = view.findViewById(R.id.tv_empty_state);
        emptyLogo = view.findViewById(R.id.iv_empty_logo);

        // Initialize hero card views
        tvCurrentTripHeader = view.findViewById(R.id.tv_current_trip_header);
        cardHeroTrip = view.findViewById(R.id.card_hero_trip);
        imgHeroTrip = view.findViewById(R.id.img_hero_trip);
        tvHeroTripName = view.findViewById(R.id.tv_hero_trip_name);
        tvHeroTripDates = view.findViewById(R.id.tv_hero_trip_dates);
        tvHeroTripLocation = view.findViewById(R.id.tv_hero_trip_location);
        tvHeroTripNotes = view.findViewById(R.id.tv_hero_trip_notes);
        tvHeroTripSchedule = view.findViewById(R.id.tv_hero_trip_schedule);
        tvHeroTripExpenses = view.findViewById(R.id.tv_hero_trip_expenses);
        fabAddTrip = view.findViewById(R.id.fab_add_trip);

        // Initialize network icon
        networkIcon = view.findViewById(R.id.network_icon);
        if (networkIcon != null) {
            networkIcon.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            });
        }

        // Initialize profile icon
        ImageView profileIcon = view.findViewById(R.id.profile_icon);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).onProfileClicked();
                }
            });
        }

        // Initialize new sections
        recyclerViewUpcomingTrips = view.findViewById(R.id.recycler_view_upcoming_trips);
        tvUpcomingTripsHeader = view.findViewById(R.id.tv_upcoming_trips_header);

        recyclerViewPastTrips = view.findViewById(R.id.recycler_view_past_trips);
        tvPastTripsHeader = view.findViewById(R.id.tv_past_trips_header);

        // Setup Upcoming Trips RecyclerView
        recyclerViewUpcomingTrips
                .setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewUpcomingTrips.setHasFixedSize(true);
        recyclerViewUpcomingTrips.setItemViewCacheSize(20);
        upcomingTripList = new ArrayList<>();
        upcomingTripAdapter = new TripCardAdapter(getContext(), upcomingTripList, this);
        recyclerViewUpcomingTrips.setAdapter(upcomingTripAdapter);

        // Setup Past Trips RecyclerView
        recyclerViewPastTrips
                .setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewPastTrips.setHasFixedSize(true);
        recyclerViewPastTrips.setItemViewCacheSize(20);
        pastTripList = new ArrayList<>();
        pastTripAdapter = new TripCardAdapter(getContext(), pastTripList, this);
        recyclerViewPastTrips.setAdapter(pastTripAdapter);

        // Load trips
        loadTrips();

        // Initialize connectivity manager and register network callback for updates
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (networkCallback == null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    mainHandler.post(() -> updateNetworkIcon(true));
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    mainHandler.post(() -> updateNetworkIcon(false));
                }
            };
        }

        try {
            if (connectivityManager != null && networkCallback != null) {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception ignored) {
        }

        // Set icon to initial state
        updateNetworkIcon(isConnected());

        // Fade-in animation for Home fragment root view
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(400).start();

        // Hero card click listener
        cardHeroTrip.setOnClickListener(v -> {
            if (displayedHeroTrip != null) {
                onTripClick(displayedHeroTrip);
            }
        });

        // FAB Add Trip button
        if (fabAddTrip != null) {
            fabAddTrip.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).showCreateTripFragment();
                }
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dateChangeReceiver == null) {
            dateChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Re-evaluate trips when system date/time changes
                    loadTrips();
                }
            };
        }
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_DATE_CHANGED);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            requireContext().registerReceiver(dateChangeReceiver, filter);
        } catch (Exception ignored) {
        }

        loadTrips();
    }

    private void loadTrips() {
        // Get the current logged-in user's ID
        int userId = com.example.voyagerbuds.utils.UserSessionManager.getCurrentUserId(requireContext());
        if (userId == -1) {
            // No user logged in, show empty list
            tripList = new ArrayList<>();
            upcomingTripList.clear();
            pastTripList.clear();
            updateTripDisplay(null);
            return;
        }
        tripList = databaseHelper.getAllTrips(userId);

        // Sort by start_date ascending so earliest upcoming trip appears first
        Collections.sort(tripList, new Comparator<Trip>() {
            @Override
            public int compare(Trip o1, Trip o2) {
                try {
                    java.time.LocalDate d1 = DateUtils.parseDbDateToLocalDate(o1.getStartDate());
                    java.time.LocalDate d2 = DateUtils.parseDbDateToLocalDate(o2.getStartDate());
                    if (d1 == null || d2 == null)
                        return 0;
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    return 0;
                }
            }
        });

        // Split trips into Current, Upcoming, and Past
        upcomingTripList.clear();
        pastTripList.clear();
        Trip currentTrip = null;

        java.time.LocalDate currentDate = DateUtils.todayLocalDate();

        for (Trip trip : tripList) {
            try {
                java.time.LocalDate startDate = DateUtils.parseDbDateToLocalDate(trip.getStartDate());
                java.time.LocalDate endDate = DateUtils.parseDbDateToLocalDate(trip.getEndDate());

                if (startDate != null && endDate != null) {
                    if (!currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)) {
                        // Current Trip
                        if (currentTrip == null) {
                            currentTrip = trip;
                        }
                    } else if (currentDate.isBefore(startDate)) {
                        // Upcoming Trip
                        upcomingTripList.add(trip);
                    } else {
                        // Past Trip
                        pastTripList.add(trip);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Ensure upcoming list sorted ascending by start date (earliest first)
        Collections.sort(upcomingTripList, new Comparator<Trip>() {
            @Override
            public int compare(Trip o1, Trip o2) {
                try {
                    java.time.LocalDate d1 = DateUtils.parseDbDateToLocalDate(o1.getStartDate());
                    java.time.LocalDate d2 = DateUtils.parseDbDateToLocalDate(o2.getStartDate());
                    if (d1 == null || d2 == null)
                        return 0;
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    return 0;
                }
            }
        });

        // Sort past trips by start date descending (most recent first)
        Collections.sort(pastTripList, new Comparator<Trip>() {
            @Override
            public int compare(Trip o1, Trip o2) {
                try {
                    java.time.LocalDate d1 = DateUtils.parseDbDateToLocalDate(o1.getStartDate());
                    java.time.LocalDate d2 = DateUtils.parseDbDateToLocalDate(o2.getStartDate());
                    if (d1 == null || d2 == null)
                        return 0;
                    return d2.compareTo(d1);
                } catch (Exception e) {
                    return 0;
                }
            }
        });

        // Update UI
        updateTripDisplay(currentTrip);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (dateChangeReceiver != null) {
                requireContext().unregisterReceiver(dateChangeReceiver);
            }
        } catch (Exception ignored) {
        }
        // Unregister network callback when fragment is paused to avoid leaks
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isConnected() {
        if (connectivityManager == null)
            return false;
        try {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null)
                return false;
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateNetworkIcon(boolean connected) {
        if (networkIcon == null)
            return;
        int color = ContextCompat.getColor(requireContext(),
                connected ? R.color.main_color_voyager : R.color.icon_tint_gray);
        networkIcon.setImageResource(connected ? R.drawable.ic_wifi : R.drawable.ic_wifi_off);
        networkIcon.setImageTintList(ColorStateList.valueOf(color));
    }

    private Trip findCurrentTrip() {
        int userId = com.example.voyagerbuds.utils.UserSessionManager.getCurrentUserId(requireContext());
        if (userId == -1)
            return null;
        List<Trip> allTrips = databaseHelper.getAllTrips(userId);
        java.time.LocalDate currentDate = DateUtils.todayLocalDate();

        for (Trip trip : allTrips) {
            try {
                java.time.LocalDate startDate = DateUtils.parseDbDateToLocalDate(trip.getStartDate());
                java.time.LocalDate endDate = DateUtils.parseDbDateToLocalDate(trip.getEndDate());
                if (startDate != null && endDate != null) {
                    if (!currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)) {
                        return trip;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void updateTripDisplay(Trip currentTrip) {
        Trip heroTrip = currentTrip;
        boolean isCurrent = true;

        // If no current trip, show the next upcoming trip as the hero trip
        if (heroTrip == null && !upcomingTripList.isEmpty()) {
            heroTrip = upcomingTripList.get(0);
            isCurrent = false;
            // Remove from upcoming list so it doesn't show twice
            upcomingTripList.remove(0);
        }

        displayedHeroTrip = heroTrip;

        // 1. Hero Trip Section (Current or Next)
        if (heroTrip != null) {
            displayHeroTrip(heroTrip, isCurrent);
            if (tvCurrentTripHeader != null) {
                tvCurrentTripHeader
                        .setText(isCurrent ? getString(R.string.current_trip) : getString(R.string.next_trip));
                tvCurrentTripHeader.setVisibility(View.VISIBLE);
            }
            emptyStateView.setVisibility(View.GONE);
        } else {
            cardHeroTrip.setVisibility(View.GONE);
            if (tvCurrentTripHeader != null)
                tvCurrentTripHeader.setVisibility(View.GONE);

            if (upcomingTripList.isEmpty() && pastTripList.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                if (emptyLogo != null) {
                    emptyLogo.setImageResource(R.drawable.undraw_trip);
                    emptyLogo.setAlpha(1.0f);
                }
            } else {
                emptyStateView.setVisibility(View.GONE);
            }
        }

        // 2. Upcoming Trips Section
        if (!upcomingTripList.isEmpty()) {
            tvUpcomingTripsHeader.setVisibility(View.VISIBLE);
            recyclerViewUpcomingTrips.setVisibility(View.VISIBLE);
            upcomingTripAdapter.notifyDataSetChanged();
        } else {
            tvUpcomingTripsHeader.setVisibility(View.GONE);
            recyclerViewUpcomingTrips.setVisibility(View.GONE);
        }

        // 3. Past Trips Section
        if (!pastTripList.isEmpty()) {
            tvPastTripsHeader.setVisibility(View.VISIBLE);
            recyclerViewPastTrips.setVisibility(View.VISIBLE);
            pastTripAdapter.notifyDataSetChanged();
        } else {
            tvPastTripsHeader.setVisibility(View.GONE);
            recyclerViewPastTrips.setVisibility(View.GONE);
        }
    }

    private void displayHeroTrip(Trip trip, boolean isCurrentTrip) {
        cardHeroTrip.setVisibility(View.VISIBLE);
        if (tvCurrentTripHeader != null) {
            tvCurrentTripHeader.setVisibility(View.VISIBLE);
        }

        // Set random background image for hero trip
        if (imgHeroTrip != null) {
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
                                .into(imgHeroTrip);
                    } catch (Exception e) {
                        imgHeroTrip.setImageResource(R.drawable.voyagerbuds_nobg);
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
                            .into(imgHeroTrip);
                } catch (Exception e) {
                    // Fallback if Glide fails
                    imgHeroTrip.setImageResource(backgroundImage);
                }
            }
        }

        // Set trip name
        tvHeroTripName.setText(trip.getTripName() != null ? trip.getTripName() : getString(R.string.default_trip_name));

        // Set location
        if (tvHeroTripLocation != null) {
            tvHeroTripLocation.setText(
                    trip.getDestination() != null ? trip.getDestination() : getString(R.string.unknown_location));
        }

        // Set notes
        if (tvHeroTripNotes != null) {
            tvHeroTripNotes.setText(
                    trip.getNotes() != null && !trip.getNotes().isEmpty() ? trip.getNotes()
                            : getString(R.string.no_notes_added));
        }

        // Set schedule summary
        if (tvHeroTripSchedule != null) {
            List<com.example.voyagerbuds.models.ScheduleItem> schedules = databaseHelper
                    .getSchedulesForTrip(trip.getTripId());
            if (schedules != null && !schedules.isEmpty()) {
                StringBuilder scheduleSummary = new StringBuilder();
                // Show up to 3 items
                int maxItems = 3;
                for (int i = 0; i < Math.min(schedules.size(), maxItems); i++) {
                    if (i > 0)
                        scheduleSummary.append(", ");
                    scheduleSummary.append(schedules.get(i).getTitle());
                }
                if (schedules.size() > maxItems) {
                    scheduleSummary.append(", +").append(schedules.size() - maxItems).append(" more");
                }
                tvHeroTripSchedule.setText(scheduleSummary.toString());
            } else {
                tvHeroTripSchedule.setText(getString(R.string.no_schedule_items_yet));
            }
        }

        // Set total expenses
        if (tvHeroTripExpenses != null) {
            java.util.Map<String, Double> totalsByCurrency = databaseHelper
                    .getTotalExpensesByCurrency(trip.getTripId());

            if (totalsByCurrency.isEmpty()) {
                tvHeroTripExpenses.setText(CurrencyHelper.formatCurrency(requireContext(), 0.0));
            } else if (totalsByCurrency.size() == 1) {
                // Single currency - display normally
                java.util.Map.Entry<String, Double> entry = totalsByCurrency.entrySet().iterator().next();
                tvHeroTripExpenses.setText(String.format(java.util.Locale.getDefault(),
                        "%s %.2f", entry.getKey(), entry.getValue()));
            } else {
                // Multiple currencies - show all
                StringBuilder sb = new StringBuilder();
                java.util.List<String> currencies = new java.util.ArrayList<>(totalsByCurrency.keySet());
                java.util.Collections.sort(currencies);

                for (int i = 0; i < currencies.size(); i++) {
                    String currency = currencies.get(i);
                    Double amount = totalsByCurrency.get(currency);
                    sb.append(String.format(java.util.Locale.getDefault(), "%s %.2f", currency, amount));
                    if (i < currencies.size() - 1) {
                        sb.append(" + ");
                    }
                }
                tvHeroTripExpenses.setText(sb.toString());
            }
        }

        // Format and display dates
        String dateDisplay = formatTripDatesSimple(trip.getStartDate(), trip.getEndDate());
        if (tvHeroTripDates != null) {
            tvHeroTripDates.setText(dateDisplay);
        }
    }

    private boolean isCurrentTrip(Trip trip) {
        try {
            java.time.LocalDate currentDate = DateUtils.todayLocalDate();
            java.time.LocalDate startDate = DateUtils.parseDbDateToLocalDate(trip.getStartDate());
            java.time.LocalDate endDate = DateUtils.parseDbDateToLocalDate(trip.getEndDate());

            if (startDate != null && endDate != null) {
                return !currentDate.isBefore(startDate) && !currentDate.isAfter(endDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String formatTripDates(String startDateStr, String endDateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = inputFormat.parse(startDateStr);
            Date endDate = inputFormat.parse(endDateStr);
            if (startDate != null && endDate != null) {
                return DateUtils.formatDateRangeHome(getContext(), startDate, endDate);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return startDateStr + " - " + endDateStr;
    }

    private String formatTripDatesDetailed(String startDateStr, String endDateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = inputFormat.parse(startDateStr);
            Date endDate = inputFormat.parse(endDateStr);
            if (startDate != null && endDate != null) {
                return DateUtils.formatDateRangeSimple(getContext(), startDate, endDate);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return startDateStr + " - " + endDateStr;
    }

    private String formatTripDatesSimple(String startDateStr, String endDateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = inputFormat.parse(startDateStr);
            Date endDate = inputFormat.parse(endDateStr);
            if (startDate != null && endDate != null) {
                return DateUtils.formatDateRangeSimple(getContext(), startDate, endDate);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return startDateStr + " - " + endDateStr;
    }

    @Override
    public void onTripClick(Trip trip) {
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
    }

    // getCurrentLocation removed from HomeFragment to avoid background location
    // work and permissions

    // getAddressFromLocation removed

    // onRequestPermissionsResult intentionally removed; HomeFragment no longer
    // requests location

}
