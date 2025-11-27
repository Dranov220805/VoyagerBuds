package com.example.voyagerbuds.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.adapters.CaptureAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Capture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CaptureFragment allows users to capture photos/videos or upload from gallery.
 * Displays recent captures as a diary-like interface.
 */
public class CaptureFragment extends Fragment implements CaptureAdapter.OnCaptureActionListener {

    private DatabaseHelper databaseHelper;
    private int currentUserId = 1; // Get from SharedPreferences/session in production
    private int currentTripId = -1; // Get from arguments in production

    // UI Components
    private Button btnCapture;
    private Button btnUpload;
    private EditText etTravelNote;
    private Button btnSaveNote;
    private RecyclerView recyclerViewCaptures;
    private CaptureAdapter captureAdapter;
    private List<Capture> captureList;
    private TextView tvViewAllPhotos;

    // Media handling
    private Uri currentPhotoUri;
    private String currentPhotoPath;
    private static final String PHOTO_DIR = "VoyagerBuds/Photos";

    // Activity result launchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;

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
        databaseHelper = new DatabaseHelper(getContext());

        if (getArguments() != null) {
            currentTripId = getArguments().getInt("tripId", -1);
        }

        setupActivityResultLaunchers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_capture, container, false);

        // Initialize views
        btnCapture = view.findViewById(R.id.btn_capture);
        btnUpload = view.findViewById(R.id.btn_upload);
        etTravelNote = view.findViewById(R.id.et_travel_note);
        btnSaveNote = view.findViewById(R.id.btn_save_note);
        recyclerViewCaptures = view.findViewById(R.id.recycler_view_captures);
        tvViewAllPhotos = view.findViewById(R.id.tv_view_all_photos);

        // Setup RecyclerView
        captureList = new ArrayList<>();
        captureAdapter = new CaptureAdapter(getContext(), captureList, this);
        recyclerViewCaptures.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerViewCaptures.setAdapter(captureAdapter);

        // Setup button listeners
        btnCapture.setOnClickListener(v -> openCamera());
        btnUpload.setOnClickListener(v -> openGallery());
        btnSaveNote.setOnClickListener(v -> saveNote());
        tvViewAllPhotos.setOnClickListener(v -> openFullGallery());

        // Load initial captures
        loadCaptures();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCaptures();
    }

    // ======================== Camera & Gallery Methods ========================

    private void setupActivityResultLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        if (currentPhotoUri != null) {
                            saveCapture(currentPhotoPath, "photo");
                        }
                    }
                });

        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        handleGallerySelection(result.getData());
                    }
                });

        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        // Gallery permission launcher
        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchGalleryPicker();
                    } else {
                        Toast.makeText(getContext(), "Gallery permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        // Multiple permissions launcher
        multiplePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                    boolean storageGranted = permissions.getOrDefault(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                                    Manifest.permission.READ_MEDIA_IMAGES :
                                    Manifest.permission.READ_EXTERNAL_STORAGE, false);

                    if (cameraGranted && storageGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(getContext(), "Permissions denied", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(getContext(),
                        getContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = new File(getContext().getExternalFilesDir(null), PHOTO_DIR);

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File imageFile = new File(storageDir, "IMG_" + timeStamp + ".jpg");
        currentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void openGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(getContext(), permission)
                != PackageManager.PERMISSION_GRANTED) {
            galleryPermissionLauncher.launch(permission);
        } else {
            launchGalleryPicker();
        }
    }

    private void launchGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/* video/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(intent);
    }

    private void handleGallerySelection(Intent data) {
        if (data.getClipData() != null) {
            // Multiple files selected
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                copyUriToInternalStorage(uri);
            }
        } else if (data.getData() != null) {
            // Single file selected
            Uri uri = data.getData();
            copyUriToInternalStorage(uri);
        }
    }

    private void copyUriToInternalStorage(Uri uri) {
        try {
            String mimeType = getContext().getContentResolver().getType(uri);
            String mediaType = mimeType != null && mimeType.startsWith("video") ? "video" : "photo";

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = new File(getContext().getExternalFilesDir(null), PHOTO_DIR);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String fileName = mediaType.equals("video") ? "VID_" + timeStamp + ".mp4" : "IMG_" + timeStamp + ".jpg";
            File destFile = new File(storageDir, fileName);

            java.io.InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            saveCapture(destFile.getAbsolutePath(), mediaType);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to import media", Toast.LENGTH_SHORT).show();
        }
    }

    // ======================== Database & UI Methods ========================

    private void saveCapture(String mediaPath, String mediaType) {
        Capture capture = new Capture();
        capture.setUserId(currentUserId);
        capture.setTripId(currentTripId > 0 ? currentTripId : 1); // Default to trip 1 if not set
        capture.setMediaPath(mediaPath);
        capture.setMediaType(mediaType);
        capture.setDescription(""); // Empty description initially
        capture.setCapturedAt(System.currentTimeMillis());
        capture.setCreatedAt(System.currentTimeMillis());
        capture.setUpdatedAt(System.currentTimeMillis());

        long id = databaseHelper.addCapture(capture);
        if (id > 0) {
            Toast.makeText(getContext(), mediaType.equals("video") ? "Video saved" : "Photo saved", Toast.LENGTH_SHORT).show();
            loadCaptures();
        } else {
            Toast.makeText(getContext(), "Failed to save capture", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNote() {
        String note = etTravelNote.getText().toString().trim();
        if (note.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a note", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a capture with just a note (no media)
        Capture capture = new Capture();
        capture.setUserId(currentUserId);
        capture.setTripId(currentTripId > 0 ? currentTripId : 1);
        capture.setMediaPath(""); // No media for note-only entries
        capture.setMediaType("note");
        capture.setDescription(note);
        capture.setCapturedAt(System.currentTimeMillis());
        capture.setCreatedAt(System.currentTimeMillis());
        capture.setUpdatedAt(System.currentTimeMillis());

        long id = databaseHelper.addCapture(capture);
        if (id > 0) {
            Toast.makeText(getContext(), "Note saved", Toast.LENGTH_SHORT).show();
            etTravelNote.setText("");
            loadCaptures();
        } else {
            Toast.makeText(getContext(), "Failed to save note", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCaptures() {
        if (currentTripId > 0) {
            List<Capture> captures = databaseHelper.getRecentCapturesForTrip(currentTripId, 6);
            captureAdapter.updateList(captures);
        }
    }

    private void openFullGallery() {
        // TODO: Navigate to full gallery fragment showing all captures
        Toast.makeText(getContext(), "Full gallery view coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCaptureClicked(Capture capture) {
        // Show capture details or preview
        if (!capture.getMediaPath().isEmpty()) {
            // TODO: Open media preview
            Toast.makeText(getContext(), "Preview: " + capture.getDescription(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCaptureDelete(Capture capture) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Capture")
                .setMessage("Are you sure you want to delete this capture?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHelper.deleteCapture(capture.getCaptureId());

                    // Delete file if exists
                    if (!capture.getMediaPath().isEmpty()) {
                        File file = new File(capture.getMediaPath());
                        if (file.exists()) {
                            file.delete();
                        }
                    }

                    Toast.makeText(getContext(), "Capture deleted", Toast.LENGTH_SHORT).show();
                    loadCaptures();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}