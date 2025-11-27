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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.voyagerbuds.utils.ImageUtils;

public class ScheduleImageAdapter extends RecyclerView.Adapter<ScheduleImageAdapter.ViewHolder> {

    private Context context;
    private List<String> imagePaths;
    private boolean isEditable;
    private OnImageRemoveListener removeListener;
    private OnImageClickListener clickListener;
    private OnImageRotationListener rotationListener;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public interface OnImageClickListener {
        void onImageClick(String imagePath);
    }

    public interface OnImageRotationListener {
        void onRotate(int position, String imagePath);
    }

    public ScheduleImageAdapter(Context context, List<String> imagePaths, boolean isEditable,
            OnImageRemoveListener removeListener) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.isEditable = isEditable;
        this.removeListener = removeListener;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnImageRotationListener(OnImageRotationListener listener) {
        this.rotationListener = listener;
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
        holder.ivImage.setTag(path); // Set tag to check later
        holder.ivImage.setImageResource(android.R.color.transparent); // Clear previous image

        executorService.execute(() -> {
            Bitmap bitmap = null;
            try {
                Uri uri;
                if (path.startsWith("content://") || path.startsWith("file://")) {
                    uri = Uri.parse(path);
                } else {
                    uri = Uri.fromFile(new File(path));
                }

                // First decode with inJustDecodeBounds=true to check dimensions
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                java.io.InputStream input = context.getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(input, null, options);
                if (input != null)
                    input.close();

                // Calculate inSampleSize
                options.inSampleSize = ImageUtils.calculateInSampleSize(options, 200, 200); // 100dp is approx 200-300px
                                                                                            // depending on density

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                input = context.getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(input, null, options);
                if (input != null)
                    input.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            final Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                if (path.equals(holder.ivImage.getTag())) {
                    if (finalBitmap != null) {
                        holder.ivImage.setImageBitmap(finalBitmap);
                    } else {
                        holder.ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
                    }
                }
            });
        });

        holder.ivImage.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onImageClick(path);
            }
        });

        if (isEditable) {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onRemove(holder.getAdapterPosition());
                }
            });

            holder.btnRotate.setVisibility(View.VISIBLE);
            holder.btnRotate.setOnClickListener(v -> {
                if (rotationListener != null) {
                    rotationListener.onRotate(holder.getAdapterPosition(), path);
                }
            });
        } else {
            holder.btnRemove.setVisibility(View.GONE);
            holder.btnRotate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageButton btnRemove;
        ImageButton btnRotate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_schedule_image);
            btnRemove = itemView.findViewById(R.id.btn_remove_image);
            btnRotate = itemView.findViewById(R.id.btn_rotate_image);
        }
    }

    // Removed calculateInSampleSize as it is now in ImageUtils
}
