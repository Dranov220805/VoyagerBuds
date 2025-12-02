package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.Capture;
import com.example.voyagerbuds.utils.ImageUtils;
import com.example.voyagerbuds.views.ZoomableImageView;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FullImageAdapter extends RecyclerView.Adapter<FullImageAdapter.ViewHolder> {

    private Context context;
    private List<Capture> captures;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    public FullImageAdapter(Context context, List<Capture> captures) {
        this.context = context;
        this.captures = captures;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_full_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Capture capture = captures.get(position);
        String imagePath = capture.getMediaPath();

        // Reset image
        holder.imageView.setImageBitmap(null);

        // Load image asynchronously
        executorService.execute(() -> {
            Bitmap bitmap = null;
            try {
                Uri uri;
                if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                    uri = Uri.parse(imagePath);
                } else {
                    uri = Uri.fromFile(new File(imagePath));
                }

                // Decode with sampling to avoid OOM
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                InputStream input = context.getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(input, null, options);
                if (input != null)
                    input.close();

                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(options, 1080, 1920);
                options.inJustDecodeBounds = false;

                input = context.getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(input, null, options);
                if (input != null)
                    input.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            Bitmap finalBitmap = bitmap;
            holder.imageView.post(() -> {
                if (finalBitmap != null) {
                    holder.imageView.setImageBitmap(finalBitmap);
                } else {
                    holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return captures.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ZoomableImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_full_image);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
