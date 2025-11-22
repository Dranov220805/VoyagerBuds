package com.example.voyagerbuds.fragments;

import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
// removed geocoding imports because getAddressFromLocation is removed
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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Locale;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;
import com.example.voyagerbuds.adapters.TripAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements TripAdapter.OnTripClickListener {

    private RecyclerView recyclerViewTrips;
    private TripAdapter tripAdapter;
    private DatabaseHelper databaseHelper;
    private List<Trip> tripList;
    private List<Trip> filteredTripList;
    private View emptyStateView;
    private ImageView emptyLogo;

    // Hero card views
    private CardView cardHeroTrip;
    private ImageView imgHeroTrip;
    private TextView tvHeroTripName;
    private TextView tvHeroTripDates;
    private TextView tvCurrentBadge;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddTrip;
    private RecyclerView recyclerViewUpcomingTrips;
    private RecyclerView recyclerViewPastTrips;
    private TextView tvUpcomingTripsHeader;
    private TextView tvPastTripsHeader;

    // Location UI elements (removed - geocoding & visibility removed)
    // Executor for background geocoding (avoid blocking UI thread)
    // Geocoding executor removedy

    // Filter options
    private String sortBy = "date_oldest";
    private String filterStartDate = null;
    private String filterEndDate = null;

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
        recyclerViewTrips = view.findViewById(R.id.recycler_view_trips);
        emptyStateView = view.findViewById(R.id.tv_empty_state);
        emptyLogo = view.findViewById(R.id.iv_empty_logo);

        // Initialize hero card views
        cardHeroTrip = view.findViewById(R.id.card_hero_trip);
        imgHeroTrip = view.findViewById(R.id.img_hero_trip);
        tvHeroTripName = view.findViewById(R.id.tv_hero_trip_name);
        tvHeroTripDates = view.findViewById(R.id.tv_hero_trip_dates);
        tvCurrentBadge = view.findViewById(R.id.tv_current_badge);
        fabAddTrip = view.findViewById(R.id.fab_add_trip);
        recyclerViewUpcomingTrips = view.findViewById(R.id.recycler_view_upcoming_trips);
        recyclerViewPastTrips = view.findViewById(R.id.recycler_view_past_trips);
        tvUpcomingTripsHeader = view.findViewById(R.id.tv_upcoming_trips_header);
        tvPastTripsHeader = view.findViewById(R.id.tv_past_trips_header);

        // Location UI removed

        // Setup RecyclerView
        recyclerViewTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        tripList = new ArrayList<>();
        filteredTripList = new ArrayList<>();
        tripAdapter = new TripAdapter(getContext(), filteredTripList, this);
        recyclerViewTrips.setAdapter(tripAdapter);

        // Load trips
        loadTrips();

        // Fade-in animation for Home fragment root view
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(400).start();

        // Hero card click listener
        cardHeroTrip.setOnClickListener(v -> {
            if (!filteredTripList.isEmpty()) {
                onTripClick(filteredTripList.get(0));
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

        // Location UI removed

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTrips();
    }

    private void loadTrips() {
        // For now, use userId = 1 (in production, get from logged-in user)
        int userId = 1;
        tripList = databaseHelper.getAllTrips(userId);

        // Apply current filter and sort
        applyFilterAndSort();
    }

    private void displayHeroTrip(Trip trip, boolean isCurrentTrip) {
        cardHeroTrip.setVisibility(View.VISIBLE);

        // Set trip name
        tvHeroTripName.setText(trip.getTripName() != null ? trip.getTripName() : "Trip");

        // Show/hide current trip badge
        tvCurrentBadge.setVisibility(isCurrentTrip ? View.VISIBLE : View.GONE);

        // Format and display dates (new format without emojis)
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
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE - MMM dd", Locale.getDefault());

            Date startDate = inputFormat.parse(startDateStr);
            Date endDate = inputFormat.parse(endDateStr);

            if (startDate != null && endDate != null) {
                String start = outputFormat.format(startDate);
                String end = outputFormat.format(endDate);

                // If same month, show range differently
                if (start.substring(6).equals(end.substring(6))) {
                    return start + " - " + new SimpleDateFormat("dd", Locale.getDefault()).format(endDate);
                }
                return start + " - " + end;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return startDateStr + " - " + endDateStr;
    }

    private String formatTripDatesDetailed(String startDateStr, String endDateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd - MMM dd, yyyy", Locale.getDefault());

            Date startDate = inputFormat.parse(startDateStr);
            Date endDate = inputFormat.parse(endDateStr);

            if (startDate != null && endDate != null) {
                SimpleDateFormat monthDay = new SimpleDateFormat("MMM dd", Locale.getDefault());
                SimpleDateFormat year = new SimpleDateFormat("yyyy", Locale.getDefault());

                String start = monthDay.format(startDate);
                String end = monthDay.format(endDate);
                String yearStr = year.format(endDate);

                return start + " - " + end + ", " + yearStr;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return startDateStr + " - " + endDateStr;
    }

    private String formatTripDatesSimple(String startDateStr, String endDateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

            Date startDate = inputFormat.parse(startDateStr);
            Date endDate = inputFormat.parse(endDateStr);

            if (startDate != null && endDate != null) {
                String start = outputFormat.format(startDate);
                String end = outputFormat.format(endDate);
                return start + " - " + end;
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

    private void setCurrentSortSelection(RadioGroup radioGroup) {
        switch (sortBy) {
            case "date_newest":
                radioGroup.check(R.id.radio_date_newest);
                break;
            case "date_oldest":
                radioGroup.check(R.id.radio_date_oldest);
                break;
            case "duration_longest":
                radioGroup.check(R.id.radio_duration_longest);
                break;
            case "duration_shortest":
                radioGroup.check(R.id.radio_duration_shortest);
                break;
            case "name_az":
                radioGroup.check(R.id.radio_name_az);
                break;
            case "name_za":
                radioGroup.check(R.id.radio_name_za);
                break;
        }
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_sort, null);
        dialog.setContentView(dialogView);

        RadioGroup radioGroupSort = dialogView.findViewById(R.id.radio_group_sort);
        Button btnFilterStartDate = dialogView.findViewById(R.id.btn_filter_start_date);
        Button btnFilterEndDate = dialogView.findViewById(R.id.btn_filter_end_date);
        Button btnClearFilter = dialogView.findViewById(R.id.btn_clear_filter);
        Button btnApplyFilter = dialogView.findViewById(R.id.btn_apply_filter);

        setCurrentSortSelection(radioGroupSort);

        if (filterStartDate != null)
            btnFilterStartDate.setText(filterStartDate);
        if (filterEndDate != null)
            btnFilterEndDate.setText(filterEndDate);

        btnFilterStartDate.setOnClickListener(v -> showDatePicker(btnFilterStartDate, true));
        btnFilterEndDate.setOnClickListener(v -> showDatePicker(btnFilterEndDate, false));

        btnClearFilter.setOnClickListener(v -> {
            sortBy = "date_oldest";
            filterStartDate = null;
            filterEndDate = null;
            applyFilterAndSort();
            dialog.dismiss();
        });

        btnApplyFilter.setOnClickListener(v -> {
            int selectedId = radioGroupSort.getCheckedRadioButtonId();
            if (selectedId == R.id.radio_date_newest)
                sortBy = "date_newest";
            else if (selectedId == R.id.radio_date_oldest)
                sortBy = "date_oldest";
            else if (selectedId == R.id.radio_duration_longest)
                sortBy = "duration_longest";
            else if (selectedId == R.id.radio_duration_shortest)
                sortBy = "duration_shortest";
            else if (selectedId == R.id.radio_name_az)
                sortBy = "name_az";
            else if (selectedId == R.id.radio_name_za)
                sortBy = "name_za";

            applyFilterAndSort();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDatePicker(Button button, boolean isStartDate) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    button.setText(date);
                    if (isStartDate) {
                        filterStartDate = date;
                    } else {
                        filterEndDate = date;
                    }
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void applyFilterAndSort() {
        filteredTripList = new ArrayList<>(tripList);

        // Apply date range filter
        if (filterStartDate != null || filterEndDate != null) {
            List<Trip> dateFiltered = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            for (Trip trip : filteredTripList) {
                try {
                    Date tripStart = sdf.parse(trip.getStartDate());
                    boolean include = true;

                    if (filterStartDate != null) {
                        Date filterStart = sdf.parse(filterStartDate);
                        if (tripStart != null && filterStart != null && tripStart.before(filterStart)) {
                            include = false;
                        }
                    }

                    if (filterEndDate != null && include) {
                        Date filterEnd = sdf.parse(filterEndDate);
                        if (tripStart != null && filterEnd != null && tripStart.after(filterEnd)) {
                            include = false;
                        }
                    }

                    if (include) {
                        dateFiltered.add(trip);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            filteredTripList = dateFiltered;
        }

        // Apply sorting
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        switch (sortBy) {
            case "date_newest":
                filteredTripList.sort((t1, t2) -> t2.getStartDate().compareTo(t1.getStartDate()));
                break;
            case "date_oldest":
                filteredTripList.sort((t1, t2) -> t1.getStartDate().compareTo(t2.getStartDate()));
                break;
            case "duration_longest":
                filteredTripList.sort((t1, t2) -> {
                    long duration1 = calculateDuration(t1, sdf);
                    long duration2 = calculateDuration(t2, sdf);
                    return Long.compare(duration2, duration1);
                });
                break;
            case "duration_shortest":
                filteredTripList.sort((t1, t2) -> {
                    long duration1 = calculateDuration(t1, sdf);
                    long duration2 = calculateDuration(t2, sdf);
                    return Long.compare(duration1, duration2);
                });
                break;
            case "name_az":
                filteredTripList.sort((t1, t2) -> t1.getTripName().compareToIgnoreCase(t2.getTripName()));
                break;
            case "name_za":
                filteredTripList.sort((t1, t2) -> t2.getTripName().compareToIgnoreCase(t1.getTripName()));
                break;
        }

        // Update display
        updateTripDisplay();
    }

    private long calculateDuration(Trip trip, SimpleDateFormat sdf) {
        try {
            Date startDate = sdf.parse(trip.getStartDate());
            Date endDate = sdf.parse(trip.getEndDate());
            if (startDate != null && endDate != null) {
                return endDate.getTime() - startDate.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void updateTripDisplay() {
        if (filteredTripList.isEmpty()) {
            // Check if this is due to filtering or genuinely no trips
            if (tripList.isEmpty()) {
                // No trips at all - show empty state
                emptyStateView.setVisibility(View.VISIBLE);
                // Set the empty state image to undraw_trip when there are no trips
                if (emptyLogo != null) {
                    emptyLogo.setImageResource(R.drawable.undraw_trip);
                    emptyLogo.setAlpha(1.0f); // keep it fully opaque for the illustration
                }
            } else {
                // Trips exist but filtered out - show "No results" message
                emptyStateView.setVisibility(View.VISIBLE);

                // Update empty state message for filter context
                TextView emptyTitle = emptyStateView.findViewById(R.id.tv_empty_title);
                TextView emptySubtitle = emptyStateView.findViewById(R.id.tv_empty_subtitle);
                if (emptyTitle != null)
                    emptyTitle.setText("No Trips Found");
                if (emptySubtitle != null)
                    emptySubtitle.setText("Try adjusting your filters");
            }
            recyclerViewTrips.setVisibility(View.GONE);
            cardHeroTrip.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);

            // Reset empty state message to default
            TextView emptyTitle = emptyStateView.findViewById(R.id.tv_empty_title);
            TextView emptySubtitle = emptyStateView.findViewById(R.id.tv_empty_subtitle);
            if (emptyTitle != null)
                emptyTitle.setText("Discover\nAmazing Places");
            if (emptySubtitle != null)
                emptySubtitle.setText("Adventure New Places");
            // Ensure default icon for non-empty states
            if (emptyLogo != null) {
                emptyLogo.setImageResource(android.R.drawable.ic_dialog_map);
                emptyLogo.setAlpha(0.2f);
            }

            // Get the first trip for hero card
            Trip heroTrip = filteredTripList.get(0);
            boolean isCurrentTrip = isCurrentTrip(heroTrip);
            displayHeroTrip(heroTrip, isCurrentTrip);

            // Display remaining trips
            if (filteredTripList.size() > 1) {
                List<Trip> otherTrips = new ArrayList<>(filteredTripList.subList(1, filteredTripList.size()));

                // Recreate adapter to ensure correct layout
                tripAdapter = new TripAdapter(getContext(), otherTrips, this);
                recyclerViewTrips.setAdapter(tripAdapter);

                recyclerViewTrips.setVisibility(View.VISIBLE);
            } else {
                recyclerViewTrips.setVisibility(View.GONE);
            }
        }
    }
}
