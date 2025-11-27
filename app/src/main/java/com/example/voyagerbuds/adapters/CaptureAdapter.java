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
                (int) (parent.getMeasuredWidth() / 3)
        ));
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
        private ImageView deleteButton;
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
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            imageView.setLayoutParams(imageParams);
            itemView.addView(imageView);

            // Delete button
            deleteButton = new ImageView(context);
            deleteButton.setImageResource(R.drawable.ic_close);
            deleteButton.setBackgroundResource(R.drawable.circle_bg_light);
            deleteButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            deleteButton.setColorFilter(context.getResources().getColor(R.color.red_light));

            FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(
                    (int) (itemView.getLayoutParams().width * 0.3),
                    (int) (itemView.getLayoutParams().height * 0.3),
                    android.view.Gravity.TOP | android.view.Gravity.END
            );
            deleteParams.setMargins(8, 8, 8, 8);
            deleteButton.setLayoutParams(deleteParams);
            itemView.addView(deleteButton);
        }

        void bind(Capture capture) {
            File mediaFile = new File(capture.getMediaPath());
            if (mediaFile.exists()) {
                imageView.setImageURI(android.net.Uri.fromFile(mediaFile));
            } else {
                imageView.setImageResource(R.drawable.ic_image);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCaptureClicked(capture);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCaptureDelete(capture);
                }
            });
        }
    }
}
