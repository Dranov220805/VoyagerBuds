package com.example.voyagerbuds.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.voyagerbuds.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.bonuspack.location.GeocoderNominatim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private TextView tvSelectedLocation;
    private Button btnConfirm;
    private FloatingActionButton fabMyLocation;
    private View loadingContainer;

    private FusedLocationProviderClient fusedLocationClient;
    private GeocoderNominatim geocoder;

    private GeoPoint selectedLocation;
    private String selectedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color to white
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFFFFFFFF);
            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
                this,
                PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_location_picker);

        // Initialize views
        mapView = findViewById(R.id.map);
        tvSelectedLocation = findViewById(R.id.tv_selected_location);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        fabMyLocation = findViewById(R.id.fab_my_location);
        loadingContainer = findViewById(R.id.loading_container);
        ImageButton btnBack = findViewById(R.id.btn_back);
        com.google.android.material.textfield.TextInputEditText etSearchLocation = findViewById(
                R.id.et_search_location);

        // Initialize location client and geocoder
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // GeocoderNominatim requires a user agent string
        geocoder = new GeocoderNominatim(Locale.getDefault(), getString(R.string.app_name));

        // Set up map
        setupMap();

        // Check if there's an existing location
        Intent intent = getIntent();
        if (intent.hasExtra("latitude") && intent.hasExtra("longitude")) {
            double lat = intent.getDoubleExtra("latitude", 0);
            double lng = intent.getDoubleExtra("longitude", 0);
            selectedLocation = new GeoPoint(lat, lng);
            mapView.getController().setCenter(selectedLocation);
            updateAddressFromLocation(selectedLocation);
            btnConfirm.setEnabled(true);
        } else {
            // Default to Vietnam center
            GeoPoint startPoint = new GeoPoint(16.0544, 108.2022); // Da Nang
            mapView.getController().setCenter(startPoint);
        }

        // Set up listeners
        btnBack.setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLocation.getLatitude());
                resultIntent.putExtra("longitude", selectedLocation.getLongitude());
                resultIntent.putExtra("address", selectedAddress);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        fabMyLocation.setOnClickListener(v -> getCurrentLocation());

        // Search location listener
        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchLocation.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchLocation(query);
                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                            android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
                return true;
            }
            return false;
        });

        // Map listener to update location when map moves
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateLocationFromMapCenter();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
    }

    private void updateLocationFromMapCenter() {
        selectedLocation = (GeoPoint) mapView.getMapCenter();
        btnConfirm.setEnabled(true);
        updateAddressFromLocation(selectedLocation);
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(
                android.content.Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            android.net.NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null &&
                    (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    private void updateAddressFromLocation(GeoPoint location) {
        // Check network availability first
        if (!isNetworkAvailable()) {
            android.util.Log.w("LocationPicker", "No network available for reverse geocoding");
            selectedAddress = String.format(Locale.getDefault(),
                    "%.6f, %.6f",
                    location.getLatitude(),
                    location.getLongitude());
            runOnUiThread(() -> tvSelectedLocation.setText(selectedAddress));
            return;
        }

        new Thread(() -> {
            try {
                android.util.Log.d("LocationPicker",
                        "Reverse geocoding: " + location.getLatitude() + ", " + location.getLongitude());

                List<android.location.Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1);

                android.util.Log.d("LocationPicker", "Addresses found: " + (addresses != null ? addresses.size() : 0));

                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address address = addresses.get(0);

                    // Log all available fields
                    android.util.Log.d("LocationPicker", "Address line 0: " + address.getAddressLine(0));
                    android.util.Log.d("LocationPicker", "Feature name: " + address.getFeatureName());
                    android.util.Log.d("LocationPicker", "Locality: " + address.getLocality());
                    android.util.Log.d("LocationPicker", "SubLocality: " + address.getSubLocality());
                    android.util.Log.d("LocationPicker", "AdminArea: " + address.getAdminArea());
                    android.util.Log.d("LocationPicker", "SubAdminArea: " + address.getSubAdminArea());

                    selectedAddress = formatAddress(address);

                    if (selectedAddress.isEmpty()) {
                        selectedAddress = String.format(Locale.getDefault(),
                                "%.6f, %.6f",
                                location.getLatitude(),
                                location.getLongitude());
                    }
                } else {
                    android.util.Log.w("LocationPicker", "No addresses found for location");
                    selectedAddress = String.format(Locale.getDefault(),
                            "%.6f, %.6f",
                            location.getLatitude(),
                            location.getLongitude());
                }

                runOnUiThread(() -> tvSelectedLocation.setText(selectedAddress));

            } catch (Exception e) {
                android.util.Log.e("LocationPicker", "Error reverse geocoding", e);
                selectedAddress = String.format(Locale.getDefault(),
                        "%.6f, %.6f",
                        location.getLatitude(),
                        location.getLongitude());
                runOnUiThread(() -> tvSelectedLocation.setText(selectedAddress));
            }
        }).start();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        loadingContainer.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    loadingContainer.setVisibility(View.GONE);

                    if (location != null) {
                        selectedLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapView.getController().animateTo(selectedLocation);
                        updateAddressFromLocation(selectedLocation);
                        btnConfirm.setEnabled(true);
                    } else {
                        Toast.makeText(this, R.string.location_not_available, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingContainer.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_getting_location, Toast.LENGTH_SHORT).show();
                });
    }

    private void searchLocation(String query) {
        // Check network availability first
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No network connection. Please check your internet and try again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        loadingContainer.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                android.util.Log.d("LocationPicker", "Searching for: " + query);

                List<android.location.Address> addresses = geocoder.getFromLocationName(query, 5);

                android.util.Log.d("LocationPicker", "Search results: " + (addresses != null ? addresses.size() : 0));

                runOnUiThread(() -> {
                    loadingContainer.setVisibility(View.GONE);

                    if (addresses != null && !addresses.isEmpty()) {
                        android.location.Address address = addresses.get(0);
                        selectedLocation = new GeoPoint(address.getLatitude(), address.getLongitude());
                        selectedAddress = formatAddress(address);

                        android.util.Log.d("LocationPicker", "Selected: " + selectedAddress);

                        mapView.getController().animateTo(selectedLocation);
                        tvSelectedLocation.setText(selectedAddress);
                        btnConfirm.setEnabled(true);
                    } else {
                        Toast.makeText(this, "Location not found. Try dragging the map instead.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("LocationPicker", "Error searching location", e);
                runOnUiThread(() -> {
                    loadingContainer.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage() + ". Please drag the map to select location.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String formatAddress(android.location.Address address) {
        // First try to use the full address line if available (Nominatim provides this)
        String fullAddress = address.getAddressLine(0);
        if (fullAddress != null && !fullAddress.isEmpty()) {
            return fullAddress;
        }

        // Otherwise build from components
        StringBuilder addressText = new StringBuilder();

        // Try to build a readable format: Street/Ward, District, City
        // For Vietnamese addresses: "Tan Phong, Ho Chi Minh"

        // Add feature name (specific location)
        if (address.getFeatureName() != null && !address.getFeatureName().matches("\\d+\\.\\d+")) {
            addressText.append(address.getFeatureName());
        }

        // Add subLocality (ward/commune) or thoroughfare (street)
        if (address.getSubLocality() != null) {
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getSubLocality());
        } else if (address.getThoroughfare() != null) {
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getThoroughfare());
        }

        // Add locality (city/district)
        if (address.getLocality() != null) {
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getLocality());
        } else if (address.getSubAdminArea() != null) {
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getSubAdminArea());
        } else if (address.getAdminArea() != null) {
            // Fallback to admin area (province/state)
            if (addressText.length() > 0) {
                addressText.append(", ");
            }
            addressText.append(address.getAdminArea());
        }

        // If still empty, try country name
        if (addressText.length() == 0 && address.getCountryName() != null) {
            addressText.append(address.getCountryName());
        }

        return addressText.toString().trim();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }
}
