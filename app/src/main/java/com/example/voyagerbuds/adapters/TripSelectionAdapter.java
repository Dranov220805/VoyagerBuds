package com.example.voyagerbuds.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.Trip;

import java.util.List;

public class TripSelectionAdapter extends RecyclerView.Adapter<TripSelectionAdapter.TripViewHolder> {

    private List<Trip> trips;
    private OnTripSelectedListener listener;

    public interface OnTripSelectedListener {
        void onTripSelected(Trip trip);
    }

    public TripSelectionAdapter(List<Trip> trips, OnTripSelectedListener listener) {
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_selection, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);

        String tripName = trip.getTripName();
        if (tripName == null || tripName.trim().isEmpty()) {
            holder.tvTripName.setText(R.string.unknown_trip);
        } else {
            holder.tvTripName.setText(tripName);
        }

        String startDate = trip.getStartDate();
        String endDate = trip.getEndDate();

        if (startDate != null && endDate != null) {
            holder.tvTripDate.setText(startDate + " - " + endDate);
            holder.tvTripDate.setVisibility(View.VISIBLE);
        } else if (startDate != null) {
            holder.tvTripDate.setText(startDate);
            holder.tvTripDate.setVisibility(View.VISIBLE);
        } else if (endDate != null) {
            holder.tvTripDate.setText(endDate);
            holder.tvTripDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvTripDate.setText(R.string.no_dates_set);
            holder.tvTripDate.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTripSelected(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTripName;
        TextView tvTripDate;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tv_trip_name);
            tvTripDate = itemView.findViewById(R.id.tv_trip_date);
        }
    }
}
