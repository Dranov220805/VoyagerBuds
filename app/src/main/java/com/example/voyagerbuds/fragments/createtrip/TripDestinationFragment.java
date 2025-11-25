package com.example.voyagerbuds.fragments.createtrip;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class TripDestinationFragment extends Fragment {

    private AutoCompleteTextView etDestination;
    private EditText etNotes, etFriends, etBudget;
    private Button btnFinish, btnBack;
    private OnTripDestinationEnteredListener listener;

    private String initialDestination;
    private String initialNotes;
    private String initialFriends;
    private String initialBudget;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ArrayAdapter<String> suggestionAdapter;

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

        suggestionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line);
        etDestination.setAdapter(suggestionAdapter);

        etDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    fetchSuggestions(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

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

        // Also make the arrow icon clickable for consistency
        ImageView ivBack = view.findViewById(R.id.btn_back_arrow);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBack();
                }
            });
        }
        btnBack.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBack();
            }
        });

        if (initialDestination != null)
            etDestination.setText(initialDestination);
        if (initialNotes != null)
            etNotes.setText(initialNotes);
        if (initialFriends != null)
            etFriends.setText(initialFriends);
        if (initialBudget != null)
            etBudget.setText(initialBudget);

        return view;
    }

    private boolean validateInput(String destination) {
        if (TextUtils.isEmpty(destination)) {
            etDestination.setError(getString(R.string.error_destination_required));
            etDestination.requestFocus();
            return false;
        }
        return true;
    }

    public void setDestination(String destination, String notes, String friends, String budget) {
        this.initialDestination = destination;
        this.initialNotes = notes;
        this.initialFriends = friends;
        this.initialBudget = budget;

        if (etDestination != null)
            etDestination.setText(destination);
        if (etNotes != null)
            etNotes.setText(notes);
        if (etFriends != null)
            etFriends.setText(friends);
        if (etBudget != null)
            etBudget.setText(budget);
    }

    private void fetchSuggestions(String query) {
        executorService.execute(() -> {
            List<String> suggestions = new ArrayList<>();
            HttpURLConnection connection = null;
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                URL url = new URL("https://nominatim.openstreetmap.org/search?q=" + encodedQuery
                        + "&format=json&addressdetails=1&limit=5");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "VoyagerBuds/1.0");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        suggestions.add(jsonObject.getString("display_name"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            mainHandler.post(() -> {
                if (getContext() != null && !suggestions.isEmpty()) {
                    suggestionAdapter.clear();
                    suggestionAdapter.addAll(suggestions);
                    suggestionAdapter.notifyDataSetChanged();
                }
            });
        });
    }
}
