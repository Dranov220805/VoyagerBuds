package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;

import java.io.File;
import java.util.List;

public class ScheduleImageAdapter extends RecyclerView.Adapter<ScheduleImageAdapter.ViewHolder> {

    private Context context;
    private List<String> imagePaths;
    private boolean isEditable;
    private OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public ScheduleImageAdapter(Context context, List<String> imagePaths, boolean isEditable,
            OnImageRemoveListener removeListener) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.isEditable = isEditable;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String path = imagePaths.get(position);

        Uri uri;
        try {
            if (path.startsWith("content://") || path.startsWith("file://")) {
                uri = Uri.parse(path);
            } else {
                uri = Uri.fromFile(new File(path));
            }
            holder.ivImage.setImageURI(uri);
        } catch (Exception e) {
            e.printStackTrace();
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        if (isEditable) {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onRemove(holder.getAdapterPosition());
                }
            });
        } else {
            holder.btnRemove.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_schedule_image);
            btnRemove = itemView.findViewById(R.id.btn_remove_image);
        }
    }
}
