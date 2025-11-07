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
            setSelected("trip");
            if (listener != null)
                listener.onNavigationItemSelected("trip");
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
            case "trip":
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

    private void setNavItemSelected(LinearLayout navItem, boolean selected) {
        if (navItem == null)
            return;

        // Get the ImageView and TextView children
        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof ImageView || child instanceof TextView) {
                child.setSelected(selected);
            }
        }
    }
}