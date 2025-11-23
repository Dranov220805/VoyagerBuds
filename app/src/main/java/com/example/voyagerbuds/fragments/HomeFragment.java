package com.example.voyagerbuds.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;
import com.example.voyagerbuds.adapters.MemoryAdapter;
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

    // New Sections
    private RecyclerView recyclerViewUpcomingTrips;
    private TripCardAdapter upcomingTripAdapter;
    private List<Trip> upcomingTripList;
    private TextView tvUpcomingTripsHeader;

    private RecyclerView recyclerViewPastTrips;
    private TripCardAdapter pastTripAdapter;
    private List<Trip> pastTripList;
    private TextView tvPastTripsHeader;

    private RecyclerView recyclerViewMemories;
    private MemoryAdapter memoryAdapter;
    private List<MemoryAdapter.MemoryItem> memoryList;
    private TextView tvMemoriesHeader;

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

        // Initialize new sections
        recyclerViewUpcomingTrips = view.findViewById(R.id.recycler_view_upcoming_trips);
        tvUpcomingTripsHeader = view.findViewById(R.id.tv_upcoming_trips_header);

        recyclerViewPastTrips = view.findViewById(R.id.recycler_view_past_trips);
        tvPastTripsHeader = view.findViewById(R.id.tv_past_trips_header);

        recyclerViewMemories = view.findViewById(R.id.recycler_view_memories);
        tvMemoriesHeader = view.findViewById(R.id.tv_memories_header);

        // Setup Upcoming Trips RecyclerView
        recyclerViewUpcomingTrips
                .setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        upcomingTripList = new ArrayList<>();
        upcomingTripAdapter = new TripCardAdapter(getContext(), upcomingTripList, this);
        recyclerViewUpcomingTrips.setAdapter(upcomingTripAdapter);

        // Setup Past Trips RecyclerView
        recyclerViewPastTrips
                .setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        pastTripList = new ArrayList<>();
        pastTripAdapter = new TripCardAdapter(getContext(), pastTripList, this);
        recyclerViewPastTrips.setAdapter(pastTripAdapter);

        // Setup Memories RecyclerView
        recyclerViewMemories.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        memoryList = new ArrayList<>();
        // Add dummy memories for now
        memoryList.add(new MemoryAdapter.MemoryItem("Eiffel Tower Sparkle", R.drawable.voyagerbuds));
        memoryList.add(new MemoryAdapter.MemoryItem("Mona Lisa Moment", R.drawable.voyagerbuds));
        memoryList.add(new MemoryAdapter.MemoryItem("Parisian Cafe Vibes", R.drawable.voyagerbuds));
        memoryAdapter = new MemoryAdapter(getContext(), memoryList);
        recyclerViewMemories.setAdapter(memoryAdapter);

        // Load trips
        loadTrips();

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
        // For now, use userId = 1 (in production, get from logged-in user)
        int userId = 1;
        tripList = databaseHelper.getAllTrips(userId);

        // Sort by start_date ascending so earliest upcoming trip appears first
        Collections.sort(tripList, new Comparator<Trip>() {
            @Override
            public int compare(Trip o1, Trip o2) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date d1 = sdf.parse(o1.getStartDate());
                    Date d2 = sdf.parse(o2.getStartDate());
                    if (d1 == null || d2 == null)
                        return 0;
                    return d1.compareTo(d2);
                } catch (ParseException e) {
                    return 0;
                }
            }
        });

        // Split trips into Current, Upcoming, and Past
        upcomingTripList.clear();
        pastTripList.clear();
        Trip currentTrip = null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDate = new Date();

        for (Trip trip : tripList) {
            try {
                Date startDate = sdf.parse(trip.getStartDate());
                Date endDate = sdf.parse(trip.getEndDate());

                if (startDate != null && endDate != null) {
                    if (!currentDate.before(startDate) && !currentDate.after(endDate)) {
                        // Current Trip
                        if (currentTrip == null) {
                            currentTrip = trip;
                        }
                    } else if (currentDate.before(startDate)) {
                        // Upcoming Trip
                        upcomingTripList.add(trip);
                    } else {
                        // Past Trip
                        pastTripList.add(trip);
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Ensure upcoming list sorted ascending by start date (earliest first)
        Collections.sort(upcomingTripList, new Comparator<Trip>() {
            @Override
            public int compare(Trip o1, Trip o2) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date d1 = sdf.parse(o1.getStartDate());
                    Date d2 = sdf.parse(o2.getStartDate());
                    if (d1 == null || d2 == null)
                        return 0;
                    return d1.compareTo(d2);
                } catch (ParseException e) {
                    return 0;
                }
            }
        });

        // Sort past trips by start date descending (most recent first)
        Collections.sort(pastTripList, new Comparator<Trip>() {
            @Override
            public int compare(Trip o1, Trip o2) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date d1 = sdf.parse(o1.getStartDate());
                    Date d2 = sdf.parse(o2.getStartDate());
                    if (d1 == null || d2 == null)
                        return 0;
                    return d2.compareTo(d1);
                } catch (ParseException e) {
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
    }

    private Trip findCurrentTrip() {
        int userId = 1;
        List<Trip> allTrips = databaseHelper.getAllTrips(userId);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDate = new Date();

        for (Trip trip : allTrips) {
            try {
                Date startDate = sdf.parse(trip.getStartDate());
                Date endDate = sdf.parse(trip.getEndDate());
                if (startDate != null && endDate != null) {
                    if (!currentDate.before(startDate) && !currentDate.after(endDate)) {
                        return trip;
                    }
                }
            } catch (ParseException e) {
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

        // 4. Memories Section
        if (!memoryList.isEmpty()) {
            tvMemoriesHeader.setVisibility(View.VISIBLE);
            recyclerViewMemories.setVisibility(View.VISIBLE);
        } else {
            tvMemoriesHeader.setVisibility(View.GONE);
            recyclerViewMemories.setVisibility(View.GONE);
        }
    }

    private void displayHeroTrip(Trip trip, boolean isCurrentTrip) {
        cardHeroTrip.setVisibility(View.VISIBLE);
        if (tvCurrentTripHeader != null) {
            tvCurrentTripHeader.setVisibility(View.VISIBLE);
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
            double totalExpenses = databaseHelper.getTotalExpensesForTrip(trip.getTripId());
            tvHeroTripExpenses.setText(String.format(Locale.getDefault(), "$%.0f", totalExpenses));
        }

        // Format and display dates
        String dateDisplay = formatTripDatesSimple(trip.getStartDate(), trip.getEndDate());
        if (tvHeroTripDates != null) {
            tvHeroTripDates.setText(dateDisplay);
        }

        // Load image from photoUrl if available, otherwise use app icon as placeholder
        if (trip.getPhotoUrl() != null && !trip.getPhotoUrl().isEmpty()) {
            // TODO: Load image using Glide or Picasso
            // For now, use app icon as placeholder even if URL exists (until image loading
            // is implemented)
            imgHeroTrip.setImageResource(R.drawable.voyagerbuds);
        } else {
            imgHeroTrip.setImageResource(R.drawable.voyagerbuds);
        }
    }

    private boolean isCurrentTrip(Trip trip) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = new Date();
            Date startDate = sdf.parse(trip.getStartDate());
            Date endDate = sdf.parse(trip.getEndDate());

            if (startDate != null && endDate != null) {
                return !currentDate.before(startDate) && !currentDate.after(endDate);
            }
        } catch (ParseException e) {
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
