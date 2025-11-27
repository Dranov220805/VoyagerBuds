package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.Trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.voyagerbuds.utils.DateUtils;
import com.example.voyagerbuds.utils.ImageRandomizer;

public class TripCardAdapter extends RecyclerView.Adapter<TripCardAdapter.TripCardViewHolder> {

    private Context context;
    private List<Trip> tripList;
    private OnTripClickListener listener;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public TripCardAdapter(Context context, List<Trip> tripList, OnTripClickListener listener) {
        this.context = context;
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip_card, parent, false);
        return new TripCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripCardViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.bind(trip, listener);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripCardViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBackground;
        TextView tvName;
        TextView tvDate;
        TextView tvLocation;

        public TripCardViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBackground = itemView.findViewById(R.id.img_trip_background);
            tvName = itemView.findViewById(R.id.tv_trip_name);
            tvDate = itemView.findViewById(R.id.tv_trip_date);
            tvLocation = itemView.findViewById(R.id.tv_trip_location);
        }

        public void bind(final Trip trip, final OnTripClickListener listener) {
            tvName.setText(trip.getTripName());
            tvLocation.setText(trip.getDestination() != null ? trip.getDestination()
                    : itemView.getContext().getString(R.string.unknown));
            tvDate.setText(formatDateRange(trip.getStartDate(), trip.getEndDate()));

            // Set background image for the trip using photoUrl if available
            String photoUrl = trip.getPhotoUrl();
            int backgroundImage;

            if (photoUrl != null && !photoUrl.isEmpty()) {
                backgroundImage = ImageRandomizer.getDrawableFromName(photoUrl);
                if (backgroundImage == 0) {
                    // It's a custom URI
                    RequestOptions options = new RequestOptions()
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.voyagerbuds_nobg)
                            .error(R.drawable.voyagerbuds_nobg);

                    Glide.with(itemView.getContext())
                            .load(android.net.Uri.parse(photoUrl))
                            .apply(options)
                            .into(imgBackground);

                    itemView.setOnClickListener(v -> listener.onTripClick(trip));
                    return;
                }
            } else {
                // No photoUrl, use consistent random based on trip ID
                backgroundImage = ImageRandomizer.getConsistentRandomBackground(trip.getTripId());
            }

            // Use Glide to load images efficiently with caching and downsampling
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.voyagerbuds_nobg)
                    .error(R.drawable.voyagerbuds_nobg);

            Glide.with(itemView.getContext())
                    .load(backgroundImage)
                    .apply(options)
                    .into(imgBackground);

            itemView.setOnClickListener(v -> listener.onTripClick(trip));
        }

        private String formatDateRange(String startDateStr, String endDateStr) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date startDate = inputFormat.parse(startDateStr);
                Date endDate = inputFormat.parse(endDateStr);

                if (startDate != null && endDate != null) {
                    return DateUtils.formatDateRangeSimple(itemView.getContext(), startDate, endDate);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return startDateStr + " - " + endDateStr;
        }
    }
}
