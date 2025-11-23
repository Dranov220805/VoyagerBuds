package com.example.voyagerbuds.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voyagerbuds.R;

public class NavigationBarFragment extends Fragment {

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(String item);

        // void onCreatePlaylistSelected();
        //
        // void onImportMusicSelected(Uri folderUri);
    }

    private OnNavigationItemSelectedListener listener;
    private LinearLayout navHome, navSchedule, navCapture, navMap, navDashboard;
    private String currentSelection = "home";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigationItemSelectedListener) {
            listener = (OnNavigationItemSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigationItemSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation_bar, container, false);

        navHome = view.findViewById(R.id.nav_home);
        navSchedule = view.findViewById(R.id.nav_schedule);
        navCapture = view.findViewById(R.id.nav_capture);
        navMap = view.findViewById(R.id.nav_map);
        navDashboard = view.findViewById(R.id.nav_dashboard);

        navHome.setOnClickListener(v -> {
            setSelected("home");
            if (listener != null)
                listener.onNavigationItemSelected("home");
        });
        navSchedule.setOnClickListener(v -> {
            setSelected("schedule");
            if (listener != null)
                listener.onNavigationItemSelected("schedule");
        });
        navCapture.setOnClickListener(v -> {
            setSelected("capture");
            if (listener != null)
                listener.onNavigationItemSelected("capture");
        });
        navMap.setOnClickListener(v -> {
            setSelected("map");
            if (listener != null)
                listener.onNavigationItemSelected("map");
        });
        navDashboard.setOnClickListener(v -> {
            setSelected("dashboard");
            if (listener != null)
                listener.onNavigationItemSelected("dashboard");
        });

        // Set initial selection
        setSelected(currentSelection);

        return view;
    }

    public void updateSelection(String item) {
        setSelected(item);
    }

    private void setSelected(String item) {
        currentSelection = item;

        // Reset all selections
        setNavItemSelected(navHome, false);
        setNavItemSelected(navSchedule, false);
        setNavItemSelected(navCapture, false);
        setNavItemSelected(navMap, false);
        setNavItemSelected(navDashboard, false);

        // Set current selection
        switch (item) {
            case "home":
                setNavItemSelected(navHome, true);
                break;
            case "schedule":
                setNavItemSelected(navSchedule, true);
                break;
            case "capture":
                setNavItemSelected(navCapture, true);
                break;
            case "map":
                setNavItemSelected(navMap, true);
                break;
            case "dashboard":
                setNavItemSelected(navDashboard, true);
                break;
        }
    }

    private void setNavItemSelected(ViewGroup navItem, boolean selected) {
        if (navItem == null)
            return;
        // Resolve theme colorPrimary once
        int colorPrimary = ContextCompat.getColor(navItem.getContext(), R.color.main_color_voyager);
        int colorDefault = ContextCompat.getColor(navItem.getContext(), R.color.text_medium);

        // Get the ImageView and TextView children (works for both LinearLayout and
        // FrameLayout)
        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                ImageView icon = (ImageView) child;
                if (selected) {
                    // Use theme color for selected icon tint
                    icon.setImageTintList(ColorStateList.valueOf(colorPrimary));
                } else {
                    icon.setImageTintList(ColorStateList.valueOf(colorDefault));
                }
            } else if (child instanceof TextView) {
                TextView text = (TextView) child;
                if (selected) {
                    text.setTextColor(colorPrimary);
                } else {
                    text.setTextColor(colorDefault);
                }
            } else if (child instanceof ViewGroup) {
                // Recursively check nested layouts (for FrameLayout > LinearLayout structure)
                setNavItemSelected((ViewGroup) child, selected);
            }
        }
    }
}