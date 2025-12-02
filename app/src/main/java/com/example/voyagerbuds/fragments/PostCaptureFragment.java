package com.example.voyagerbuds.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Capture;
import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.utils.UserSessionManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostCaptureFragment extends Fragment {

    private static final String ARG_IMAGE_PATH = "image_path";
    private String imagePath;
    private ImageView imagePreview;
    private EditText etCaption;
    private Button btnPost;
    private ImageButton btnBack;
    private DatabaseHelper databaseHelper;
    private int currentUserId;

    public PostCaptureFragment() {
        // Required empty public constructor
    }

    public static PostCaptureFragment newInstance(String imagePath) {
        PostCaptureFragment fragment = new PostCaptureFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
        }
        databaseHelper = new DatabaseHelper(getContext());
        currentUserId = UserSessionManager.getCurrentUserId(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_capture, container, false);

        imagePreview = view.findViewById(R.id.image_preview);
        etCaption = view.findViewById(R.id.et_caption);
        btnPost = view.findViewById(R.id.btn_post);
        btnBack = view.findViewById(R.id.btn_back);

        // Configure EditText to handle Enter as Done while allowing multi-line wrapping
        etCaption.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etCaption.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        etCaption.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                                && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    // Hide keyboard
                    InputMethodManager imm = (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    etCaption.clearFocus();
                    return true;
                }
                return false;
            }
        });

        if (imagePath != null) {
            displayImage();
        }

        btnPost.setOnClickListener(v -> saveAndPost());
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    private void displayImage() {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        // Handle rotation
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }
            if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        imagePreview.setImageBitmap(bitmap);
    }

    private void saveAndPost() {
        String caption = etCaption.getText().toString().trim();

        // Create new image with embedded caption if caption exists
        String finalImagePath = imagePath;
        if (!caption.isEmpty()) {
            finalImagePath = embedCaptionToImage(caption);
            if (finalImagePath == null) {
                Toast.makeText(getContext(), getString(R.string.failed_embed_caption), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Trip currentTrip = detectCurrentTrip();
        int tripId = (currentTrip != null) ? currentTrip.getTripId() : -1;

        Capture capture = new Capture();
        capture.setUserId(currentUserId);
        capture.setTripId(tripId);
        capture.setMediaPath(finalImagePath);
        capture.setMediaType("photo");
        capture.setDescription(caption);
        capture.setCapturedAt(System.currentTimeMillis());
        capture.setCreatedAt(System.currentTimeMillis());
        capture.setUpdatedAt(System.currentTimeMillis());

        long id = databaseHelper.addCapture(capture);
        if (id > 0) {
            String tripName = (currentTrip != null) ? currentTrip.getTripName() : getString(R.string.unknown_trip);
            Toast.makeText(getContext(), getString(R.string.saved_to_trip, tripName), Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack(); // Go back to camera
        } else {
            Toast.makeText(getContext(), getString(R.string.failed_to_save), Toast.LENGTH_SHORT).show();
        }
    }

    private String embedCaptionToImage(String caption) {
        try {
            // Load original bitmap
            Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);
            if (originalBitmap == null)
                return null;

            // Handle rotation
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }
            if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0,
                        originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            }

            // Create mutable bitmap
            Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            // Calculate dimensions based on image size to match preview proportions
            int width = mutableBitmap.getWidth();
            int height = mutableBitmap.getHeight();

            // Scale values proportionally to image width
            // Padding ~12dp (0.04 of width)
            int padding = (int) (width * 0.04f);
            // Bottom margin ~24dp (0.04 of height)
            int bottomMargin = (int) (height * 0.04f);

            // Text size proportional to image width (14sp on ~360dp width = ~4.5%)
            float textSize = width * 0.045f;
            // Setup text paint
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(textSize);
            textPaint.setTextAlign(Paint.Align.LEFT); // Required for StaticLayout alignment

            // Calculate max text width (e.g., 80% of image width to leave margins)
            int maxTextWidth = (int) (width * 0.8f);

            // Create StaticLayout with max width to determine line breaks
            StaticLayout textLayout = new StaticLayout(caption, textPaint, maxTextWidth,
                    Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

            // Calculate actual text width by finding the widest line
            float maxLineWidth = 0;
            for (int i = 0; i < textLayout.getLineCount(); i++) {
                maxLineWidth = Math.max(maxLineWidth, textLayout.getLineWidth(i));
            }

            // Background width is actual text width + padding
            int bgWidth = (int) (maxLineWidth + (padding * 2));

            // Calculate background dimensions
            int textHeight = textLayout.getHeight();
            int bgHeight = textHeight + (padding * 2);
            float cornerRadius = bgHeight / 2f; // Fully rounded corners

            // Calculate position (Centered horizontally)
            int bgLeft = (width - bgWidth) / 2;
            int bgTop = height - bottomMargin - bgHeight;
            int bgRight = bgLeft + bgWidth;
            int bgBottom = bgTop + bgHeight;

            // Draw semi-transparent gray background with rounded corners
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            // Use resource color for consistency
            int bgColor = ContextCompat.getColor(requireContext(), R.color.caption_background);
            bgPaint.setColor(bgColor);
            RectF bgRect = new RectF(bgLeft, bgTop, bgRight, bgBottom);
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint);

            // Draw text centered in background
            canvas.save();
            // We need to draw the StaticLayout such that its text aligns with the
            // background center.
            // StaticLayout width is maxTextWidth. It centers text within that width.
            // So we center the StaticLayout box horizontally on the image.
            float layoutX = (width - maxTextWidth) / 2f;
            float layoutY = bgTop + padding;

            canvas.translate(layoutX, layoutY);
            textLayout.draw(canvas);
            canvas.restore();

            // Save to new file
            String newPath = imagePath.replace(".jpg", "_with_caption.jpg");
            java.io.FileOutputStream out = new java.io.FileOutputStream(newPath);
            mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            out.flush();
            out.close();

            // Cleanup
            originalBitmap.recycle();
            mutableBitmap.recycle();

            return newPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Trip detectCurrentTrip() {
        List<Trip> trips = databaseHelper.getAllTrips(currentUserId);
        if (trips.isEmpty())
            return null;

        long today = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date(today));

        for (Trip trip : trips) {
            if (trip.getStartDate() == null || trip.getEndDate() == null)
                continue;
            try {
                Date startDate = sdf.parse(trip.getStartDate());
                Date endDate = sdf.parse(trip.getEndDate());
                Date todayDate = sdf.parse(todayStr);

                if (todayDate != null && startDate != null && endDate != null) {
                    if (!todayDate.before(startDate) && !todayDate.after(endDate)) {
                        return trip;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Fallback to most recent trip if no ongoing trip
        return trips.get(0);
    }
}
