package com.example.voyagerbuds.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.activities.HomeActivity;

/**
 * Fragment shown after creating a trip to prompt user to add schedules per day
 * or skip.
 */
public class AddScheduleStepFragment extends Fragment {

    private static final String ARG_TRIP_ID = "arg_trip_id";
    private static final String ARG_TRIP_NAME = "arg_trip_name";
    private static final String ARG_TRIP_DATES = "arg_trip_dates";

    private long tripId = -1;
    private String tripName;
    private String tripDates;

    public AddScheduleStepFragment() {
        // Required empty public constructor
    }

    public static AddScheduleStepFragment newInstance(long tripId) {
        AddScheduleStepFragment fragment = new AddScheduleStepFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TRIP_ID, tripId);
        fragment.setArguments(args);
        return fragment;
    }

    // Optional: allow passing name and dates for preview
    public static AddScheduleStepFragment newInstance(long tripId, String name, String dates) {
        AddScheduleStepFragment fragment = new AddScheduleStepFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TRIP_ID, tripId);
        args.putString(ARG_TRIP_NAME, name);
        args.putString(ARG_TRIP_DATES, dates);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tripId = getArguments().getLong(ARG_TRIP_ID, -1);
            tripName = getArguments().getString(ARG_TRIP_NAME);
            tripDates = getArguments().getString(ARG_TRIP_DATES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_schedule_step, container, false);

        Button btnNow = view.findViewById(R.id.btn_add_schedule_now);
        TextView btnLater = view.findViewById(R.id.btn_add_schedule_later);

        btnNow.setOnClickListener(v -> {
            // Navigate to TripDetailFragment for this trip so user can add schedule items
            TripDetailFragment tripDetailFragment = TripDetailFragment.newInstance(tripId);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                    .replace(R.id.content_container, tripDetailFragment)
                    .addToBackStack(null)
                    .commit();

            // Update upper bar title if present
            if (getActivity() instanceof HomeActivity) {
                // Assuming "trip_detail" is the key or just keep it generic
                // ((HomeActivity) getActivity()).setCurrentFragmentKey("trip_detail");
            }
        });

        btnLater.setOnClickListener(v -> {
            // Skip adding schedule now, go back to home
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.content_container, new HomeFragment())
                    .commit();

            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).setCurrentFragmentKey("home");
            }
        });

        return view;
    }
}
