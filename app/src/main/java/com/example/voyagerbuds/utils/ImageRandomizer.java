package com.example.voyagerbuds.utils;

import com.example.voyagerbuds.R;
import java.util.Random;

/**
 * Utility class for randomly selecting trip background images
 */
public class ImageRandomizer {

    private static final int[] TRIP_BACKGROUNDS = {
            R.drawable.trip_bg_1,
            R.drawable.trip_bg_2,
            R.drawable.trip_bg_3,
            R.drawable.trip_bg_4,
            R.drawable.trip_bg_5,
            R.drawable.trip_bg_6
    };

    private static final Random random = new Random();

    /**
     * Get a random trip background drawable resource ID
     * 
     * @return drawable resource ID
     */
    public static int getRandomTripBackground() {
        int index = random.nextInt(TRIP_BACKGROUNDS.length);
        return TRIP_BACKGROUNDS[index];
    }

    /**
     * Get a consistent random background for a specific trip ID
     * This ensures the same trip always gets the same background image
     * 
     * @param tripId the trip ID
     * @return drawable resource ID
     */
    public static int getConsistentRandomBackground(int tripId) {
        int index = Math.abs(tripId % TRIP_BACKGROUNDS.length);
        return TRIP_BACKGROUNDS[index];
    }

    /**
     * Get the drawable resource name for a given trip ID (for database storage)
     * 
     * @param tripId the trip ID
     * @return drawable resource name (e.g., "trip_bg_1")
     */
    public static String getDefaultImageName(int tripId) {
        int index = Math.abs(tripId % TRIP_BACKGROUNDS.length);
        return "trip_bg_" + (index + 1);
    }

    /**
     * Get drawable resource ID from resource name
     * 
     * @param imageName the image name (e.g., "trip_bg_1" or "content://..." for
     *                  custom images)
     * @return drawable resource ID, or 0 if it's a custom URI
     */
    public static int getDrawableFromName(String imageName) {
        if (imageName == null || imageName.isEmpty()) {
            return R.drawable.voyagerbuds_nobg;
        }

        // If it's a content URI or file path, return 0 (will be handled differently)
        if (imageName.startsWith("content://") || imageName.startsWith("file://")) {
            return 0;
        }

        // Match the trip_bg_X pattern
        switch (imageName) {
            case "trip_bg_1":
                return R.drawable.trip_bg_1;
            case "trip_bg_2":
                return R.drawable.trip_bg_2;
            case "trip_bg_3":
                return R.drawable.trip_bg_3;
            case "trip_bg_4":
                return R.drawable.trip_bg_4;
            case "trip_bg_5":
                return R.drawable.trip_bg_5;
            case "trip_bg_6":
                return R.drawable.trip_bg_6;
            default:
                return R.drawable.voyagerbuds_nobg;
        }
    }
}
