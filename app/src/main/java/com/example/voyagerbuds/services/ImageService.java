package com.example.voyagerbuds.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.voyagerbuds.utils.ImageUtils;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service class for handling Image-related operations.
 * Provides async image loading, processing, and caching.
 */
public class ImageService {
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public ImageService(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(3); // Multiple threads for parallel loading
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Callback interface for image loading results
     */
    public interface ImageLoadCallback {
        void onImageLoaded(Bitmap bitmap);

        void onError(Exception e);
    }

    /**
     * Load an image asynchronously from a URI or file path
     * 
     * @param imagePath The path or URI of the image
     * @param reqWidth  Required width for scaling
     * @param reqHeight Required height for scaling
     * @param callback  Callback for results
     */
    public void loadImage(String imagePath, int reqWidth, int reqHeight, ImageLoadCallback callback) {
        executorService.execute(() -> {
            try {
                Uri uri = parseImageUri(imagePath);
                Bitmap bitmap = loadBitmapFromUri(uri, reqWidth, reqHeight);

                if (bitmap != null) {
                    // Rotate if needed based on EXIF data
                    bitmap = ImageUtils.rotateImageIfRequired(context, bitmap, uri);
                    Bitmap finalBitmap = bitmap;
                    mainHandler.post(() -> callback.onImageLoaded(finalBitmap));
                } else {
                    mainHandler.post(() -> callback.onError(
                            new Exception("Failed to load bitmap")));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Load an image asynchronously with automatic size detection
     * 
     * @param imagePath The path or URI of the image
     * @param callback  Callback for results
     */
    public void loadImage(String imagePath, ImageLoadCallback callback) {
        // Use screen dimensions as default
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        loadImage(imagePath, metrics.widthPixels, metrics.heightPixels, callback);
    }

    /**
     * Load a thumbnail image (smaller size for lists/grids)
     * 
     * @param imagePath The path or URI of the image
     * @param callback  Callback for results
     */
    public void loadThumbnail(String imagePath, ImageLoadCallback callback) {
        int thumbnailSize = (int) (200 * context.getResources().getDisplayMetrics().density);
        loadImage(imagePath, thumbnailSize, thumbnailSize, callback);
    }

    /**
     * Load bitmap from URI synchronously (use with caution, should not be called on
     * main thread)
     * 
     * @param uri       The image URI
     * @param reqWidth  Required width for scaling
     * @param reqHeight Required height for scaling
     * @return Bitmap or null if failed
     */
    private Bitmap loadBitmapFromUri(Uri uri, int reqWidth, int reqHeight) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            InputStream input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(input, null, options);
            if (input != null)
                input.close();

            // Calculate inSampleSize
            options.inSampleSize = ImageUtils.calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            input = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (input != null)
                input.close();

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse image path to URI
     * 
     * @param imagePath The image path (can be content://, file://, or absolute
     *                  path)
     * @return URI object
     */
    private Uri parseImageUri(String imagePath) {
        if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
            return Uri.parse(imagePath);
        } else {
            return Uri.fromFile(new File(imagePath));
        }
    }

    /**
     * Load multiple images asynchronously
     * 
     * @param imagePaths Array of image paths
     * @param reqWidth   Required width for scaling
     * @param reqHeight  Required height for scaling
     * @param callback   Callback for each loaded image
     */
    public void loadImages(String[] imagePaths, int reqWidth, int reqHeight,
            MultiImageLoadCallback callback) {
        for (int i = 0; i < imagePaths.length; i++) {
            final int index = i;
            final String path = imagePaths[i];

            loadImage(path, reqWidth, reqHeight, new ImageLoadCallback() {
                @Override
                public void onImageLoaded(Bitmap bitmap) {
                    callback.onImageLoaded(index, bitmap);
                }

                @Override
                public void onError(Exception e) {
                    callback.onError(index, e);
                }
            });
        }
    }

    /**
     * Callback interface for multiple image loading
     */
    public interface MultiImageLoadCallback {
        void onImageLoaded(int index, Bitmap bitmap);

        void onError(int index, Exception e);
    }

    /**
     * Decode bitmap synchronously (blocking call, use in background thread)
     * 
     * @param imagePath The path or URI of the image
     * @param reqWidth  Required width for scaling
     * @param reqHeight Required height for scaling
     * @return Bitmap or null if failed
     */
    public Bitmap decodeBitmapSync(String imagePath, int reqWidth, int reqHeight) {
        try {
            Uri uri = parseImageUri(imagePath);
            Bitmap bitmap = loadBitmapFromUri(uri, reqWidth, reqHeight);

            if (bitmap != null) {
                bitmap = ImageUtils.rotateImageIfRequired(context, bitmap, uri);
            }

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if an image file exists
     * 
     * @param imagePath The image path
     * @return true if file exists
     */
    public boolean imageExists(String imagePath) {
        try {
            if (imagePath.startsWith("content://")) {
                // Check content URI
                InputStream input = context.getContentResolver().openInputStream(Uri.parse(imagePath));
                if (input != null) {
                    input.close();
                    return true;
                }
                return false;
            } else {
                // Check file path
                File file = new File(imagePath.replace("file://", ""));
                return file.exists();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Shutdown the executor service
     * Call this when the service is no longer needed
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
