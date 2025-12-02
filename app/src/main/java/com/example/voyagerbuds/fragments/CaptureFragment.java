package com.example.voyagerbuds.fragments;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CaptureFragment extends Fragment {

    private static final String TAG = "CaptureFragment";
    private static final String PHOTO_DIR = "VoyagerBuds/Photos";

    private PreviewView cameraPreview;
    private ImageButton btnFlash;
    private TextView tvZoom;
    private ImageButton btnAdd; // Change Camera
    private FloatingActionButton btnCapture;
    private ImageButton btnGallery;
    private View swipeArea;
    private View bottomDrawer;
    private ImageButton btnCloseDrawer;

    private ImageCapture imageCapture;
    private Camera camera;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private ProcessCameraProvider cameraProvider;
    private boolean isFlashOn = false;

    private ActivityResultLauncher<String[]> cameraPermissionLauncher;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private GestureDetector gestureDetector;

    public CaptureFragment() {
        // Required empty public constructor
    }

    public static CaptureFragment newInstance(int tripId) {
        CaptureFragment fragment = new CaptureFragment();
        Bundle args = new Bundle();
        args.putInt("tripId", tripId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActivityResultLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_capture, container, false);

        cameraPreview = view.findViewById(R.id.camera_preview);
        btnFlash = view.findViewById(R.id.btn_flash);
        tvZoom = view.findViewById(R.id.tv_zoom);
        btnAdd = view.findViewById(R.id.btn_add);
        btnCapture = view.findViewById(R.id.btn_capture);
        btnGallery = view.findViewById(R.id.btn_gallery);
        swipeArea = view.findViewById(R.id.swipe_area);
        bottomDrawer = view.findViewById(R.id.bottom_drawer);
        btnCloseDrawer = view.findViewById(R.id.btn_close_drawer);
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnAdd.setOnClickListener(v -> switchCamera());
        btnFlash.setOnClickListener(v -> toggleFlash());
        tvZoom.setOnClickListener(v -> toggleZoom());
        btnGallery.setOnClickListener(v -> openDrawer());
        btnCloseDrawer.setOnClickListener(v -> closeDrawer());

        setupBottomSheet();
        setupSwipeGesture();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions();
        }

        return view;
    }

    private void setupActivityResultLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean granted : permissions.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private boolean allPermissionsGranted() {
        if (getContext() == null)
            return false;
        return ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        cameraPermissionLauncher.launch(new String[] { Manifest.permission.CAMERA });
    }

    private void startCamera() {
        if (getContext() == null)
            return;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.failed_start_camera), Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .build();

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            try {
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                updateZoomUI();
            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }
    }

    private void capturePhoto() {
        if (imageCapture == null || getContext() == null)
            return;

        // Create output options for shared storage
        ImageCapture.OutputFileOptions outputOptions;
        final File legacyPhotoFile; // Store reference for Android 8-9

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ use MediaStore
            ContentValues contentValues = createMediaStoreEntry();
            if (contentValues == null) {
                Toast.makeText(getContext(), getString(R.string.failed_create_photo_file), Toast.LENGTH_SHORT).show();
                return;
            }
            outputOptions = new ImageCapture.OutputFileOptions.Builder(
                    getContext().getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues).build();
            legacyPhotoFile = null;
        } else {
            // Android 9 and below use File API
            legacyPhotoFile = createImageFile();
            if (legacyPhotoFile == null) {
                Toast.makeText(getContext(), getString(R.string.failed_create_photo_file), Toast.LENGTH_SHORT).show();
                return;
            }
            outputOptions = new ImageCapture.OutputFileOptions.Builder(legacyPhotoFile).build();
        }

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Get the saved URI or file path
                        String imagePath = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Uri savedUri = outputFileResults.getSavedUri();
                            if (savedUri != null) {
                                imagePath = savedUri.toString();
                            }
                        } else {
                            // For Android 8-9, use the file path we created
                            if (legacyPhotoFile != null) {
                                imagePath = legacyPhotoFile.getAbsolutePath();
                                // Trigger media scan so gallery picks up the new image
                                if (getContext() != null) {
                                    MediaScannerConnection.scanFile(getContext(),
                                            new String[] { imagePath },
                                            new String[] { "image/jpeg" },
                                            null);
                                }
                            }
                        }

                        // Navigate to PostCaptureFragment
                        if (isAdded() && imagePath != null) {
                            PostCaptureFragment fragment = PostCaptureFragment.newInstance(imagePath);
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.content_container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), getString(R.string.failed_capture_photo), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
    }

    /**
     * Creates MediaStore entry for Android 10+
     */
    private ContentValues createMediaStoreEntry() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String displayName = "IMG_" + timeStamp + ".jpg";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VoyagerBuds");
        }
        return contentValues;
    }

    /**
     * Creates image file for Android 9 and below
     */
    private File createImageFile() {
        if (getContext() == null)
            return null;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        // Use public Pictures directory so gallery can access it
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "VoyagerBuds");
        if (!storageDir.exists())
            storageDir.mkdirs();
        return new File(storageDir, "IMG_" + timeStamp + ".jpg");
    }

    private void switchCamera() {
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        // Reset zoom when switching cameras
        bindCameraUseCases();
    }

    private void toggleFlash() {
        isFlashOn = !isFlashOn;
        // Rebind camera with new flash mode
        bindCameraUseCases();
        if (btnFlash != null && getContext() != null) {
            btnFlash.setImageResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        }
    }

    private void toggleZoom() {
        if (camera == null)
            return;
        CameraControl control = camera.getCameraControl();
        ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();

        if (zoomState != null) {
            float minZoom = zoomState.getMinZoomRatio();
            float currentZoom = zoomState.getZoomRatio();

            float targetZoom = 1.0f;
            String zoomText = getString(R.string.zoom_1x);

            // Only allow toggling if the camera supports wide angle (< 1.0x)
            if (minZoom < 1.0f) {
                // Check if we are effectively at 1x (allow small epsilon)
                if (Math.abs(currentZoom - 1.0f) < 0.05f) {
                    // We are at 1x, go to 0.5x (or minZoom)
                    targetZoom = Math.max(minZoom, 0.5f);
                    zoomText = getString(R.string.zoom_0_5x);
                } else {
                    // We are not at 1x (presumably 0.5x), go back to 1x
                    targetZoom = 1.0f;
                    zoomText = getString(R.string.zoom_1x);
                }
                control.setZoomRatio(targetZoom);
                tvZoom.setText(zoomText);
            } else {
                // If wide angle not supported, ensure we stay at 1x
                control.setZoomRatio(1.0f);
                tvZoom.setText(getString(R.string.zoom_1x));
            }
        }
    }

    private void updateZoomUI() {
        // Reset to 1x when binding/rebinding
        tvZoom.setText(R.string.zoom_1x);
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomDrawer);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setPeekHeight(0);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // Load AlbumFragment into the drawer
                    loadLibraryContent();
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Clear the fragment when drawer is hidden
                    clearLibraryContent();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Optional: Add animation effects during slide
            }
        });
    }

    private void setupSwipeGesture() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null)
                    return false;

                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) {
                            // Swipe up detected
                            openDrawer();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        swipeArea.setOnTouchListener((v, event) -> {
            boolean handled = gestureDetector.onTouchEvent(event);
            // Only consume the event if it was a swipe gesture, otherwise pass through to
            // views below
            return handled;
        });
    }

    private void openDrawer() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void closeDrawer() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void loadLibraryContent() {
        if (isAdded()) {
            Fragment existingFragment = getChildFragmentManager().findFragmentById(R.id.drawer_content_container);
            if (existingFragment instanceof AlbumFragment) {
                // Refresh existing fragment
                ((AlbumFragment) existingFragment).refreshAlbum();
            } else if (existingFragment == null) {
                // Create new fragment
                AlbumFragment albumFragment = AlbumFragment.newInstance();
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.drawer_content_container, albumFragment)
                        .commit();
            }
        }
    }

    private void clearLibraryContent() {
        if (isAdded()) {
            Fragment fragment = getChildFragmentManager().findFragmentById(R.id.drawer_content_container);
            if (fragment != null) {
                getChildFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        gestureDetector = null;
    }
}