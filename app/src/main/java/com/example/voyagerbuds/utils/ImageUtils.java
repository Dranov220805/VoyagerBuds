package com.example.voyagerbuds.utils;

import android.graphics.BitmapFactory;

public class ImageUtils {
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static android.graphics.Bitmap rotateImageIfRequired(android.content.Context context,
            android.graphics.Bitmap img, android.net.Uri selectedImage) throws java.io.IOException {
        java.io.InputStream input = context.getContentResolver().openInputStream(selectedImage);
        androidx.exifinterface.media.ExifInterface ei;
        try {
            ei = new androidx.exifinterface.media.ExifInterface(input);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return img;
        } finally {
            if (input != null)
                input.close();
        }

        int orientation = ei.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static android.graphics.Bitmap rotateImage(android.graphics.Bitmap img, int degree) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(degree);
        android.graphics.Bitmap rotatedImg = android.graphics.Bitmap.createBitmap(img, 0, 0, img.getWidth(),
                img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }
}
