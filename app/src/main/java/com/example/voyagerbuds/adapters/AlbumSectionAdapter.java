package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.AlbumSection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AlbumSectionAdapter - Displays trips with their organized captures
 * Optimized with view pooling for performance
 */
public class AlbumSectionAdapter extends RecyclerView.Adapter<AlbumSectionAdapter.SectionViewHolder> {

    private Context context;
    private List<AlbumSection> sections;
    private OnCaptureClickListener listener;

    // Shared RecycledViewPool for nested RecyclerViews
    private RecyclerView.RecycledViewPool sharedPool;

    public interface OnCaptureClickListener {
        void onCaptureClick(int captureId, String mediaPath);
    }

    public AlbumSectionAdapter(Context context, List<AlbumSection> sections, OnCaptureClickListener listener) {
        this.context = context;
        this.sections = sections;
        this.listener = listener;
        this.sharedPool = new RecyclerView.RecycledViewPool();
        this.sharedPool.setMaxRecycledViews(0, 20); // Pool for day items
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album_section, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        AlbumSection section = sections.get(position);
        holder.bind(section);
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    public void updateSections(List<AlbumSection> newSections) {
        this.sections = newSections;
        notifyDataSetChanged();
    }

    class SectionViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerViewDays;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerViewDays = itemView.findViewById(R.id.recycler_view_days);
        }

        public void bind(AlbumSection section) {
            // Setup days RecyclerView with optimizations
            AlbumDayAdapter dayAdapter = new AlbumDayAdapter(context, section.getDays(), listener);
            recyclerViewDays.setLayoutManager(new LinearLayoutManager(context));
            recyclerViewDays.setAdapter(dayAdapter);
            recyclerViewDays.setNestedScrollingEnabled(false);
            recyclerViewDays.setRecycledViewPool(sharedPool);
            recyclerViewDays.setItemViewCacheSize(10);
        }
    }
}
