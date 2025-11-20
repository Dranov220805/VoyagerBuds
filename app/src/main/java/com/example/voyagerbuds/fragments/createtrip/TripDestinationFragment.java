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

public class TripDestinationFragment extends Fragment {

    private EditText etDestination, etNotes, etFriends, etBudget;
    private Button btnFinish, btnBack;
    private OnTripDestinationEnteredListener listener;

    public interface OnTripDestinationEnteredListener {
        void onTripDestinationEntered(String destination, String notes, String friends, String budget);

        void onBack();
    }

    public void setListener(OnTripDestinationEnteredListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_destination, container, false);

        etDestination = view.findViewById(R.id.et_destination);
        etNotes = view.findViewById(R.id.et_notes);
        etFriends = view.findViewById(R.id.et_invite_friends);
        etBudget = view.findViewById(R.id.et_budget);
        btnFinish = view.findViewById(R.id.btn_finish);
        btnBack = view.findViewById(R.id.btn_back);

        btnFinish.setOnClickListener(v -> {
            String destination = etDestination.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();
            String friends = etFriends.getText().toString().trim();
            String budget = etBudget.getText().toString().trim();
            
            if (validateInput(destination)) {
                if (listener != null) {
                    listener.onTripDestinationEntered(destination, notes, friends, budget);
                }
            }
        });

        btnBack.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBack();
            }
        });

        return view;
    }

    private boolean validateInput(String destination) {
        if (TextUtils.isEmpty(destination)) {
            etDestination.setError("Destination is required");
            etDestination.requestFocus();
            return false;
        }
        return true;
    }

    public void setDestination(String destination, String notes, String friends, String budget) {
        if (etDestination != null) etDestination.setText(destination);
        if (etNotes != null) etNotes.setText(notes);
        if (etFriends != null) etFriends.setText(friends);
        if (etBudget != null) etBudget.setText(budget);
    }
}
