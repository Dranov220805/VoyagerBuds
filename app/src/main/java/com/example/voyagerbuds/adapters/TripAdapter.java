package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private Context context;
    private List<Trip> tripList;
    private OnTripClickListener listener;
    private DatabaseHelper databaseHelper;
    private boolean isCompactMode;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public TripAdapter(Context context, List<Trip> tripList, OnTripClickListener listener) {
        this.context = context;
        this.tripList = tripList;
        this.listener = listener;
        this.databaseHelper = new DatabaseHelper(context);
        this.isCompactMode = false;
    }

    public TripAdapter(Context context, List<Trip> tripList, OnTripClickListener listener, boolean isCompactMode) {
        this.context = context;
        this.tripList = tripList;
        this.listener = listener;
        this.databaseHelper = new DatabaseHelper(context);
        this.isCompactMode = isCompactMode;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (isCompactMode) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_trip_compact, parent, false);
            return new CompactViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_trip, parent, false);
            return new TripViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        if (holder instanceof CompactViewHolder) {
            CompactViewHolder compactHolder = (CompactViewHolder) holder;
            compactHolder.tvTripName.setText(trip.getTripName());

            // Show destination and dates in one line
            String destination = trip.getDestination() != null ? trip.getDestination() : "Unknown";
            String duration = formatDuration(trip.getStartDate(), trip.getEndDate());
            compactHolder.tvTripInfo.setText(destination + " • " + duration);
        } else {
            holder.tvTripName.setText(trip.getTripName());
            holder.tvDestination.setText(trip.getDestination() != null ? trip.getDestination() : "Unknown");

            // Format duration
            String duration = formatDuration(trip.getStartDate(), trip.getEndDate());
            holder.tvDuration.setText(duration);

            // Get total spent from expenses
            double totalSpent = databaseHelper.getTotalExpensesForTrip(trip.getTripId());
            if (trip.getBudget() > 0) {
                holder.tvTotalSpent.setText(String.format(Locale.getDefault(), "$%.2f / $%.2f", totalSpent, trip.getBudget()));
            } else {
                holder.tvTotalSpent.setText(String.format(Locale.getDefault(), "$%.2f", totalSpent));
            }

            // Load image if available (placeholder for now)
            if (trip.getPhotoUrl() != null && !trip.getPhotoUrl().isEmpty()) {
                // TODO: Load image using Glide or Picasso
                // For now, use placeholder
                holder.imgTrip.setImageResource(R.drawable.voyagerbuds);
            } else {
                holder.imgTrip.setImageResource(R.drawable.voyagerbuds);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTripClick(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    private String formatDuration(String startDate, String endDate) {
        if (startDate == null || endDate == null) {
            return "No dates set";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

            Date start = inputFormat.parse(startDate);
            Date end = inputFormat.parse(endDate);

            if (start != null && end != null) {
                return outputFormat.format(start) + " - " + outputFormat.format(end);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return startDate + " - " + endDate;
    }

    public void updateTrips(List<Trip> newTripList) {
        this.tripList = newTripList;
        notifyDataSetChanged();
    }

    public boolean isCompactMode() {
        return isCompactMode;
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        ImageView imgTrip;
        TextView tvTripName, tvDestination, tvDuration, tvTotalSpent;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            imgTrip = itemView.findViewById(R.id.img_trip);
            tvTripName = itemView.findViewById(R.id.tv_trip_name);
            tvDestination = itemView.findViewById(R.id.tv_destination);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvTotalSpent = itemView.findViewById(R.id.tv_total_spent);
        }
    }

    static class CompactViewHolder extends TripViewHolder {
        TextView tvTripInfo;

        public CompactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripInfo = itemView.findViewById(R.id.tv_trip_info);
        }
    }
}
