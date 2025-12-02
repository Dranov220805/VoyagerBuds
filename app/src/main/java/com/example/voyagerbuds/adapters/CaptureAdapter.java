package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.Capture;

import java.io.File;
import java.util.List;

/**
 * Adapter for displaying captures (photos/videos) in a grid RecyclerView.
 * Shows recent captures with delete functionality.
 */
public class CaptureAdapter extends RecyclerView.Adapter<CaptureAdapter.CaptureViewHolder> {
    private Context context;
    private List<Capture> captureList;
    private OnCaptureActionListener listener;

    public interface OnCaptureActionListener {
        void onCaptureClicked(Capture capture);

        void onCaptureDelete(Capture capture);
    }

    public CaptureAdapter(Context context, List<Capture> captureList, OnCaptureActionListener listener) {
        this.context = context;
        this.captureList = captureList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CaptureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout itemView = new FrameLayout(context);
        itemView.setLayoutParams(new ViewGroup.LayoutParams(
                (int) (parent.getMeasuredWidth() / 3),
                (int) (parent.getMeasuredWidth() / 3)));
        return new CaptureViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CaptureViewHolder holder, int position) {
        Capture capture = captureList.get(position);
        holder.bind(capture);
    }

    @Override
    public int getItemCount() {
        return captureList.size();
    }

    public void updateList(List<Capture> newList) {
        captureList = newList;
        notifyDataSetChanged();
    }

    class CaptureViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private FrameLayout itemView;

        CaptureViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = (FrameLayout) itemView;
            setupView();
        }

        private void setupView() {
            itemView.removeAllViews();

            // Main image view
            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(imageParams);
            itemView.addView(imageView);
        }

        void bind(Capture capture) {
            File mediaFile = new File(capture.getMediaPath());

            // Use Glide for efficient image loading with caching and thumbnails
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .override(300, 300); // Load smaller size for grid

            Glide.with(context)
                    .load(mediaFile.exists() ? mediaFile : R.drawable.ic_image)
                    .apply(options)
                    .into(imageView);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCaptureClicked(capture);
                }
            });
        }
    }
}
