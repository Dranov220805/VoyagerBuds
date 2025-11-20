package com.example.voyagerbuds.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
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
                // TODO: Implement edit trip
                return true;
            } else if (id == R.id.action_delete) {
                // TODO: Implement delete trip
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
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        return view;
    }
}
