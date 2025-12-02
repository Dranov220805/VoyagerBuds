package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.AlbumDay;

import java.util.List;

/**
 * AlbumDayAdapter - Displays captures organized by day
 * Optimized with view pooling for grid items
 */
public class AlbumDayAdapter extends RecyclerView.Adapter<AlbumDayAdapter.DayViewHolder> {

    private Context context;
    private List<AlbumDay> days;
    private AlbumSectionAdapter.OnCaptureClickListener listener;

    // Shared pool for capture grid items
    private RecyclerView.RecycledViewPool capturePool;

    public AlbumDayAdapter(Context context, List<AlbumDay> days,
            AlbumSectionAdapter.OnCaptureClickListener listener) {
        this.context = context;
        this.days = days;
        this.listener = listener;
        this.capturePool = new RecyclerView.RecycledViewPool();
        this.capturePool.setMaxRecycledViews(0, 30); // Pool for capture items
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        AlbumDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayLabel;
        RecyclerView recyclerViewCaptures;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayLabel = itemView.findViewById(R.id.tv_day_label);
            recyclerViewCaptures = itemView.findViewById(R.id.recycler_view_captures);
        }

        public void bind(AlbumDay day) {
            tvDayLabel.setText(day.getDateLabel());

            // Setup captures grid with optimizations
            CaptureAdapter captureAdapter = new CaptureAdapter(context, day.getCaptures(),
                    new CaptureAdapter.OnCaptureActionListener() {
                        @Override
                        public void onCaptureClicked(com.example.voyagerbuds.models.Capture capture) {
                            if (listener != null) {
                                listener.onCaptureClick(capture.getCaptureId(), capture.getMediaPath());
                            }
                        }

                        @Override
                        public void onCaptureDelete(com.example.voyagerbuds.models.Capture capture) {
                            // Handle delete if needed
                        }
                    });

            GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 3);
            recyclerViewCaptures.setLayoutManager(gridLayoutManager);
            recyclerViewCaptures.setAdapter(captureAdapter);
            recyclerViewCaptures.setNestedScrollingEnabled(false);
            recyclerViewCaptures.setRecycledViewPool(capturePool);
            recyclerViewCaptures.setItemViewCacheSize(15);
            recyclerViewCaptures.setHasFixedSize(true);
        }
    }
}
