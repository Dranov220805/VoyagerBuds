package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.net.Uri;
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

import java.io.File;
import java.util.List;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {

    private Context context;
    private List<MemoryItem> memoryList;
    private OnMemoryClickListener clickListener;

    public interface OnMemoryClickListener {
        void onMemoryClick(MemoryItem memoryItem);
    }

    public static class MemoryItem {
        public String title;
        public int imageResId; // For dummy data (if needed)
        public String imagePath; // Path to real image file
        public int tripId; // Associated trip ID

        public MemoryItem(String title, int imageResId) {
            this.title = title;
            this.imageResId = imageResId;
            this.imagePath = null;
            this.tripId = -1;
        }

        public MemoryItem(String title, int imageResId, String imagePath, int tripId) {
            this.title = title;
            this.imageResId = imageResId;
            this.imagePath = imagePath;
            this.tripId = tripId;
        }
    }

    public MemoryAdapter(Context context, List<MemoryItem> memoryList) {
        this.context = context;
        this.memoryList = memoryList;
        this.clickListener = null;
    }

    public MemoryAdapter(Context context, List<MemoryItem> memoryList, OnMemoryClickListener clickListener) {
        this.context = context;
        this.memoryList = memoryList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_memory_card, parent, false);
        return new MemoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        MemoryItem item = memoryList.get(position);
        holder.tvTitle.setText(item.title);

        // Load image - prefer real path over resource ID
        if (item.imagePath != null && !item.imagePath.isEmpty()) {
            // Load from file path
            File imageFile = new File(item.imagePath);
            if (imageFile.exists()) {
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.voyagerbuds_nobg)
                        .error(R.drawable.voyagerbuds_nobg);

                Glide.with(context)
                        .load(imageFile)
                        .apply(options)
                        .into(holder.imgMemory);
            } else {
                // Try as URI
                try {
                    RequestOptions options = new RequestOptions()
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.voyagerbuds_nobg)
                            .error(R.drawable.voyagerbuds_nobg);

                    Glide.with(context)
                            .load(Uri.parse(item.imagePath))
                            .apply(options)
                            .into(holder.imgMemory);
                } catch (Exception e) {
                    holder.imgMemory.setImageResource(R.drawable.voyagerbuds_nobg);
                }
            }
        } else if (item.imageResId != 0) {
            // Load from resource ID
            holder.imgMemory.setImageResource(item.imageResId);
        } else {
            // Default placeholder
            holder.imgMemory.setImageResource(R.drawable.voyagerbuds_nobg);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMemoryClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memoryList.size();
    }

    public static class MemoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMemory;
        TextView tvTitle;

        public MemoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMemory = itemView.findViewById(R.id.img_memory);
            tvTitle = itemView.findViewById(R.id.tv_memory_title);
        }
    }
}
