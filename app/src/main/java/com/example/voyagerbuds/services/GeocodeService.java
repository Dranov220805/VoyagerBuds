package com.example.voyagerbuds.services;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service class for handling Geocoding operations.
 * Provides async geocoding from location names to coordinates and vice versa.
 */
public class GeocodeService {
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public GeocodeService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Callback interface for geocoding results
     */
    public interface GeocodeCallback {
        void onSuccess(double latitude, double longitude, String formattedAddress);

        void onError(Exception e);
    }

    /**
     * Callback interface for reverse geocoding results
     */
    public interface ReverseGeocodeCallback {
        void onSuccess(String address, String city, String country);

        void onError(Exception e);
    }

    /**
     * Convert location name to coordinates asynchronously
     * 
     * @param locationName The name of the location
     * @param callback     Callback for results
     */
    public void getCoordinatesFromLocationName(String locationName, GeocodeCallback callback) {
        executorService.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(locationName, 1);

                // If not found and doesn't contain "Vietnam", try appending it
                if ((addresses == null || addresses.isEmpty()) &&
                        !locationName.toLowerCase().contains("vietnam")) {
                    addresses = geocoder.getFromLocationName(locationName + ", Vietnam", 1);
                }

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    double lat = address.getLatitude();
                    double lng = address.getLongitude();
                    String formattedAddress = getFormattedAddress(address);

                    mainHandler.post(() -> callback.onSuccess(lat, lng, formattedAddress));
                } else {
                    mainHandler.post(() -> callback.onError(
                            new Exception("Location not found")));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Convert coordinates to address asynchronously
     * 
     * @param latitude  The latitude
     * @param longitude The longitude
     * @param callback  Callback for results
     */
    public void getAddressFromCoordinates(double latitude, double longitude,
            ReverseGeocodeCallback callback) {
        executorService.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String fullAddress = getFormattedAddress(address);
                    String city = address.getLocality() != null ? address.getLocality() : "";
                    String country = address.getCountryName() != null ? address.getCountryName() : "";

                    mainHandler.post(() -> callback.onSuccess(fullAddress, city, country));
                } else {
                    mainHandler.post(() -> callback.onError(
                            new Exception("Address not found")));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Synchronous geocoding (use with caution, should not be called on main thread)
     * 
     * @param locationName The name of the location
     * @return Array with [latitude, longitude] or null if not found
     */
    public double[] getCoordinatesSync(String locationName) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);

            // If not found and doesn't contain "Vietnam", try appending it
            if ((addresses == null || addresses.isEmpty()) &&
                    !locationName.toLowerCase().contains("vietnam")) {
                addresses = geocoder.getFromLocationName(locationName + ", Vietnam", 1);
            }

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return new double[] { address.getLatitude(), address.getLongitude() };
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Synchronous reverse geocoding (use with caution, should not be called on main
     * thread)
     * 
     * @param latitude  The latitude
     * @param longitude The longitude
     * @return Formatted address or null if not found
     */
    public String getAddressSync(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                return getFormattedAddress(addresses.get(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Format address object into a readable string
     * 
     * @param address The Address object
     * @return Formatted address string
     */
    private String getFormattedAddress(Address address) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(address.getAddressLine(i));
        }

        if (sb.length() == 0) {
            // Fallback if no address lines
            if (address.getFeatureName() != null) {
                sb.append(address.getFeatureName());
            }
            if (address.getLocality() != null) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(address.getLocality());
            }
            if (address.getCountryName() != null) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(address.getCountryName());
            }
        }

        return sb.toString();
    }

    /**
     * Check if geocoding is available on the device
     * 
     * @return true if Geocoder is present
     */
    public boolean isGeocodingAvailable() {
        return Geocoder.isPresent();
    }

    /**
     * Shutdown the executor service
     * Call this when the service is no longer needed
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
