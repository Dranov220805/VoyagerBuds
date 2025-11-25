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

import java.util.List;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {

    private Context context;
    private List<MemoryItem> memoryList;

    public static class MemoryItem {
        String title;
        int imageResId; // For dummy data

        public MemoryItem(String title, int imageResId) {
            this.title = title;
            this.imageResId = imageResId;
        }
    }

    public MemoryAdapter(Context context, List<MemoryItem> memoryList) {
        this.context = context;
        this.memoryList = memoryList;
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
        // holder.imgMemory.setImageResource(item.imageResId); // Use placeholder for
        // now if resource not available
        holder.imgMemory.setImageResource(R.drawable.voyagerbuds_nobg); // Default
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
