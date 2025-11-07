package com.example.voyagerbuds.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.voyagerbuds.R;

public class UpperBarFragment extends Fragment {

    public interface OnProfileClickListener {
        void onProfileClicked();
    }

    private OnProfileClickListener listener;
    private TextView appName;
    private ImageView profileIcon;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnProfileClickListener) {
            listener = (OnProfileClickListener) context;
        }
        // It's optional, so we don't throw an exception if not implemented
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
        View view = inflater.inflate(R.layout.fragment_upper_bar, container, false);

        appName = view.findViewById(R.id.app_name);
        profileIcon = view.findViewById(R.id.profile_icon);

        // Set profile icon click listener
        profileIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileClicked();
            }
        });

        return view;
    }

    // Optional: Method to update app name dynamically
    public void setAppName(String name) {
        if (appName != null) {
            appName.setText(name);
        }
    }

    // Optional: Method to change profile icon
    public void setProfileIcon(int drawableResId) {
        if (profileIcon != null) {
            profileIcon.setImageResource(drawableResId);
        }
    }
}
