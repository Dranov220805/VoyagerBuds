package com.example.voyagerbuds.fragments.createtrip;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;

public class TripNameFragment extends Fragment {

    private EditText etTripName;
    private Button btnNext;
    private OnTripNameEnteredListener listener;
    private String initialTripName;

    public interface OnTripNameEnteredListener {
        void onTripNameEntered(String tripName);
    }

    public void setListener(OnTripNameEnteredListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_name, container, false);

        etTripName = view.findViewById(R.id.et_trip_name);
        btnNext = view.findViewById(R.id.btn_next);

        if (initialTripName != null) etTripName.setText(initialTripName);

        btnNext.setOnClickListener(v -> {
            String tripName = etTripName.getText().toString().trim();
            if (validateInput(tripName)) {
                if (listener != null) {
                    listener.onTripNameEntered(tripName);
                }
            }
        });

        // Handle keyboard "Done" button
        etTripName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                btnNext.performClick();
                return true;
            }
            return false;
        });

        return view;
    }

    private boolean validateInput(String tripName) {
        if (TextUtils.isEmpty(tripName)) {
            etTripName.setError("Trip name is required");
            etTripName.requestFocus();
            return false;
        }
        if (tripName.length() < 3) {
            etTripName.setError("Trip name must be at least 3 characters");
            etTripName.requestFocus();
            return false;
        }
        return true;
    }

    public void setTripName(String tripName) {
        this.initialTripName = tripName;
        if (etTripName != null) {
            etTripName.setText(tripName);
        }
    }
}
