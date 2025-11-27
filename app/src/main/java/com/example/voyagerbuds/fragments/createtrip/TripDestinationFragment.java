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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class TripDestinationFragment extends Fragment {

    private MaterialAutoCompleteTextView etDestination;
    private EditText etNotes, etFriends, etBudget;
    private Spinner spinnerBudgetCurrency;
    private Button btnFinish, btnBack;
    private OnTripDestinationEnteredListener listener;

    private String initialDestination;
    private String initialNotes;
    private String initialFriends;
    private String initialBudget;
    private String initialCurrency;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ArrayAdapter<String> suggestionAdapter;
    private Runnable searchRunnable;
    private static final long DEBOUNCE_DELAY = 500; // 500ms delay

    public interface OnTripDestinationEnteredListener {
        void onTripDestinationEntered(String destination, String notes, String friends, String budget, String currency);

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
        spinnerBudgetCurrency = view.findViewById(R.id.spinner_budget_currency);
        btnFinish = view.findViewById(R.id.btn_finish);
        btnBack = view.findViewById(R.id.btn_back);

        // Setup currency spinner
        String[] currencies = new String[] { "USD", "VND" };
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBudgetCurrency.setAdapter(currencyAdapter);

        // Auto-select currency based on language
        String language = Locale.getDefault().getLanguage();
        if ("vi".equals(language)) {
            spinnerBudgetCurrency.setSelection(1); // VND
        } else {
            spinnerBudgetCurrency.setSelection(0); // USD
        }

        suggestionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line);
        etDestination.setAdapter(suggestionAdapter);

        // Optimized autocomplete with debouncing
        etDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search request
                if (searchRunnable != null) {
                    mainHandler.removeCallbacks(searchRunnable);
                }

                if (s.length() >= 3) {
                    // Create new search request with delay
                    searchRunnable = () -> fetchSuggestions(s.toString());
                    mainHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY);
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
            String currency = spinnerBudgetCurrency.getSelectedItem().toString();

            if (validateInput(destination)) {
                if (listener != null) {
                    listener.onTripDestinationEntered(destination, notes, friends, budget, currency);
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

    public void setDestination(String destination, String notes, String friends, String budget, String currency) {
        this.initialDestination = destination;
        this.initialNotes = notes;
        this.initialFriends = friends;
        this.initialBudget = budget;
        this.initialCurrency = currency;

        if (etDestination != null)
            etDestination.setText(destination);
        if (etNotes != null)
            etNotes.setText(notes);
        if (etFriends != null)
            etFriends.setText(friends);
        if (etBudget != null)
            etBudget.setText(budget);
        if (spinnerBudgetCurrency != null && currency != null) {
            if ("VND".equals(currency)) {
                spinnerBudgetCurrency.setSelection(1);
            } else {
                spinnerBudgetCurrency.setSelection(0);
            }
        }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any pending search requests
        if (searchRunnable != null) {
            mainHandler.removeCallbacks(searchRunnable);
        }
    }
}
