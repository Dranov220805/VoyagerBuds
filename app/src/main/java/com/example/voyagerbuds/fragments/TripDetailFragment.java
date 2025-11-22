package com.example.voyagerbuds.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

public class TripDetailFragment extends Fragment {

    private static final String ARG_TRIP_ID = "trip_id";
    private long tripId;
    private DatabaseHelper databaseHelper;
    private Trip trip;

    public static TripDetailFragment newInstance(long tripId) {
        TripDetailFragment fragment = new TripDetailFragment();
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_detail, container, false);

        // Initialize Views
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        TextView tvTitle = view.findViewById(R.id.tv_trip_title);
        TextView tvDates = view.findViewById(R.id.tv_trip_dates);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);

        // Setup Toolbar
        toolbar.setNavigationOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                editTrip();
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteConfirmationDialog();
                return true;
            }
            return false;
        });

        // Load Trip Data
        trip = databaseHelper.getTripById((int) tripId);
        if (trip != null) {
            tvTitle.setText(trip.getTripName());
            String dateRange = trip.getStartDate() + " - " + trip.getEndDate();
            tvDates.setText(dateRange);
        }

        // Handle Tabs
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Handle tab selection
                // For now, just show a toast or switch visibility if I had multiple layouts
                // Since I only implemented Schedule layout in XML, I'll leave it as is.
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        return view;
    }

    private void editTrip() {
        if (trip == null) {
            Toast.makeText(getContext(), "Trip not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to EditTripFragment with trip data
        EditTripFragment editFragment = EditTripFragment.newInstance(tripId);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right)
                .replace(R.id.content_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showDeleteConfirmationDialog() {
        if (trip == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Trip")
                .setMessage(
                        "Are you sure you want to delete \"" + trip.getTripName() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTrip();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTrip() {
        if (trip == null) {
            return;
        }

        try {
            databaseHelper.deleteTrip((int) tripId);
            Toast.makeText(getContext(), "Trip deleted successfully", Toast.LENGTH_SHORT).show();

            // Navigate back to home fragment
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() instanceof HomeActivity) {
                // If no back stack, manually show home fragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.content_container, new HomeFragment())
                        .commit();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to delete trip", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
