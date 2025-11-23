package com.example.voyagerbuds.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;
import android.widget.TextView;
import android.widget.LinearLayout;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;

public class MapFragment extends Fragment {

    private MapView mapView;
    private List<LocationPin> vietnamLocations;
    private MyLocationNewOverlay myLocationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker currentLocationMarker;
    private FloatingActionButton fabMyLocation;
    private View loadingContainer;
    private DatabaseHelper databaseHelper;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));

        // Initialize Vietnam locations
        initializeVietnamLocations();

        // Initialize database helper
        databaseHelper = new DatabaseHelper(requireContext());

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.map);
        fabMyLocation = view.findViewById(R.id.fab_my_location);
        loadingContainer = view.findViewById(R.id.loading_container);

        // Show loading indicator
        showLoading();

        setupMap();
        setupLocationButton();

        return view;
    }

    private void setupLocationButton() {
        fabMyLocation.setOnClickListener(v -> {
            getCurrentLocation();
        });
    }

    private void setupMap() {
        // Enable multi-touch controls
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        // Set tile source
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Set default view to Vietnam (center)
        mapView.getController().setZoom(6.0);
        GeoPoint startPoint = new GeoPoint(16.0544, 108.2022); // Vietnam center
        mapView.getController().setCenter(startPoint);

        // Add my location overlay
        setupMyLocationOverlay();

        // Add map events overlay to handle clicks on map background
        setupMapEventsOverlay();

        // Add markers for user trips
        loadUserTrips();

        // Hide loading indicator once map is ready
        // Use a small delay to ensure tiles start loading
        mapView.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideLoading();
            }
        }, 800); // 800ms delay to let initial tiles load

        // Don't automatically get location - let user click the button
        // getCurrentLocation();
    }

    private void setupMapEventsOverlay() {
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // Close all info windows when map is clicked
                InfoWindow.closeAllInfoWindowsOn(mapView);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        MapEventsOverlay OverlayEvents = new MapEventsOverlay(mReceive);
        mapView.getOverlays().add(0, OverlayEvents); // Add at index 0 to be below other overlays
    }

    private void setupMyLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void getCurrentLocation() {
        // Check location permission
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(),
                    "Location permission not granted. Please enable it in settings.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Got last known location
                            GeoPoint currentLocation = new GeoPoint(
                                    location.getLatitude(),
                                    location.getLongitude());

                            // Add marker for current location
                            addCurrentLocationMarker(currentLocation);

                            // Zoom to current location
                            mapView.getController().setZoom(15.0);
                            mapView.getController().animateTo(currentLocation);

                            Toast.makeText(requireContext(),
                                    String.format("Current Location:\nLat: %.4f, Lon: %.4f",
                                            location.getLatitude(),
                                            location.getLongitude()),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Unable to get current location. Please check GPS.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addCurrentLocationMarker(GeoPoint location) {
        // Remove old marker if exists
        if (currentLocationMarker != null) {
            mapView.getOverlays().remove(currentLocationMarker);
        }

        // Create new marker for current location
        currentLocationMarker = new Marker(mapView);
        currentLocationMarker.setPosition(location);
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentLocationMarker.setTitle(getString(R.string.your_current_location));
        currentLocationMarker.setSnippet(String.format("Lat: %.4f, Lon: %.4f",
                location.getLatitude(), location.getLongitude()));

        // Different icon or color for current location (you can customize this)
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.ic_location));

        currentLocationMarker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            String message = String.format(
                    "📍 %s\nLatitude: %.4f\nLongitude: %.4f",
                    clickedMarker.getTitle(),
                    clickedMarker.getPosition().getLatitude(),
                    clickedMarker.getPosition().getLongitude());

            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            clickedMarker.showInfoWindow();
            return true;
        });

        mapView.getOverlays().add(currentLocationMarker);
        mapView.invalidate();
    }

    private void initializeVietnamLocations() {
        vietnamLocations = new ArrayList<>();

        // Add famous locations in Vietnam
        vietnamLocations.add(new LocationPin("Hanoi", 21.0285, 105.8542));
        vietnamLocations.add(new LocationPin("Ho Chi Minh City", 10.8231, 106.6297));
        vietnamLocations.add(new LocationPin("Da Nang", 16.0544, 108.2022));
        vietnamLocations.add(new LocationPin("Hoi An", 15.8801, 108.3380));
        vietnamLocations.add(new LocationPin("Hue", 16.4637, 107.5909));
        vietnamLocations.add(new LocationPin("Nha Trang", 12.2388, 109.1967));
        vietnamLocations.add(new LocationPin("Da Lat", 11.9404, 108.4583));
        vietnamLocations.add(new LocationPin("Ha Long Bay", 20.9101, 107.1839));
        vietnamLocations.add(new LocationPin("Sapa", 22.3364, 103.8438));
        vietnamLocations.add(new LocationPin("Phu Quoc", 10.2899, 103.9840));
    }

    private void addMarkersToMap() {
        for (LocationPin location : vietnamLocations) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(location.latitude, location.longitude));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(location.name);

            // Set marker click listener
            marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                String message = String.format(
                        "%s\nLatitude: %.4f\nLongitude: %.4f",
                        clickedMarker.getTitle(),
                        clickedMarker.getPosition().getLatitude(),
                        clickedMarker.getPosition().getLongitude());

                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

                // Show info window
                clickedMarker.showInfoWindow();

                return true;
            });

            mapView.getOverlays().add(marker);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
        }
    }

    private void showLoading() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.GONE);
        }
    }

    private void loadUserTrips() {
        // Use userId = 1 as in HomeFragment
        int userId = 1;
        List<Trip> trips = databaseHelper.getAllTrips(userId);

        for (Trip trip : trips) {
            if (trip.getMapLatitude() != 0.0 && trip.getMapLongitude() != 0.0) {
                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(trip.getMapLatitude(), trip.getMapLongitude()));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle(trip.getTripName());
                marker.setSnippet(trip.getStartDate() + " - " + trip.getEndDate());

                // Set custom info window
                marker.setInfoWindow(new TripInfoWindow(R.layout.info_window_trip, mapView, trip));

                mapView.getOverlays().add(marker);
            }
        }
        mapView.invalidate();
    }

    private void navigateToTripDetail(int tripId) {
        TripDetailFragment fragment = TripDetailFragment.newInstance(tripId);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right)
                .replace(R.id.content_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private class TripInfoWindow extends InfoWindow {
        private Trip trip;

        public TripInfoWindow(int layoutResId, MapView mapView, Trip trip) {
            super(layoutResId, mapView);
            this.trip = trip;
        }

        @Override
        public void onOpen(Object item) {
            Marker marker = (Marker) item;

            TextView tvTitle = (TextView) mView.findViewById(R.id.tv_trip_title);
            TextView tvDate = (TextView) mView.findViewById(R.id.tv_trip_date);

            if (tvTitle != null)
                tvTitle.setText(trip.getTripName());
            if (tvDate != null)
                tvDate.setText(trip.getStartDate() + " - " + trip.getEndDate());

            // Set click listener on the whole view to navigate
            mView.setOnClickListener(v -> {
                navigateToTripDetail(trip.getTripId());
                close();
            });
        }

        @Override
        public void onClose() {
            // nothing to do
        }
    }

    // Helper class to store location data
    private static class LocationPin {
        String name;
        double latitude;
        double longitude;

        LocationPin(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}