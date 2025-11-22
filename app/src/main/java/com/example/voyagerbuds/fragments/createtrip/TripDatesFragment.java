package com.example.voyagerbuds.fragments.createtrip;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Trip;
import com.example.voyagerbuds.utils.BookedDateDecorator;
import com.example.voyagerbuds.utils.DateValidatorBlockTrips;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TripDatesFragment extends Fragment {

    private EditText etStartDate, etEndDate;
    private Button btnNext;
    private ImageView btnBack;
    private OnTripDatesEnteredListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private DatabaseHelper databaseHelper;

    public interface OnTripDatesEnteredListener {
        void onTripDatesEntered(String startDate, String endDate);

        void onBack();
    }

    public void setListener(OnTripDatesEnteredListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_dates, container, false);

        databaseHelper = new DatabaseHelper(getContext());

        etStartDate = view.findViewById(R.id.et_start_date);
        etEndDate = view.findViewById(R.id.et_end_date);
        btnNext = view.findViewById(R.id.btn_next);
        btnBack = view.findViewById(R.id.btn_back_arrow);

        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));

        btnNext.setOnClickListener(v -> {
            String startDate = etStartDate.getTag() != null ? etStartDate.getTag().toString() : "";
            String endDate = etEndDate.getTag() != null ? etEndDate.getTag().toString() : "";
            if (validateInput(startDate, endDate)) {
                if (listener != null) {
                    listener.onTripDatesEntered(startDate, endDate);
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

    private void showDatePicker(boolean isStartDate) {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText(isStartDate ? "Select Start Date" : "Select End Date");

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();

        // Block past dates
        long today = MaterialDatePicker.todayInUtcMilliseconds();
        constraintsBuilder.setStart(today);

        List<CalendarConstraints.DateValidator> validators = new ArrayList<>();
        validators.add(DateValidatorPointForward.now());

        // We still block the dates so they are unselectable
        List<Long> blockedRanges = getBlockedDateRanges();
        validators.add(new DateValidatorBlockTrips(blockedRanges));

        constraintsBuilder.setValidator(CompositeDateValidator.allOf(validators));
        builder.setCalendarConstraints(constraintsBuilder.build());

        // Add decorator to show red strikethrough on booked dates
        builder.setDayViewDecorator(new BookedDateDecorator(blockedRanges));

        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            // MaterialDatePicker returns time in UTC
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);

            // Format for display (local time representation of the date)
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String formattedDate = utcFormat.format(calendar.getTime());

            // Format for UI display
            SimpleDateFormat displayUtcFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            displayUtcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String displayDate = displayUtcFormat.format(calendar.getTime());

            if (isStartDate) {
                etStartDate.setText(displayDate);
                etStartDate.setTag(formattedDate);
            } else {
                etEndDate.setText(displayDate);
                etEndDate.setTag(formattedDate);
            }
        });
        picker.show(getParentFragmentManager(), "date_picker");
    }

    private List<Long> getBlockedDateRanges() {
        List<Long> ranges = new ArrayList<>();
        // Assuming userId = 1 as per current implementation
        List<Trip> trips = databaseHelper.getAllTrips(1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (Trip trip : trips) {
            try {
                Date start = sdf.parse(trip.getStartDate());
                Date end = sdf.parse(trip.getEndDate());
                if (start != null && end != null) {
                    ranges.add(start.getTime());
                    ranges.add(end.getTime());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return ranges;
    }

    private boolean validateInput(String startDate, String endDate) {
        if (TextUtils.isEmpty(startDate)) {
            Toast.makeText(getContext(), "Please select start date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(endDate)) {
            Toast.makeText(getContext(), "Please select end date", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);
            if (start != null && end != null && end.before(start)) {
                Toast.makeText(getContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Check for overlapping trips
            if (!databaseHelper.isDateRangeAvailable(startDate, endDate)) {
                Toast.makeText(getContext(), "You already have a trip scheduled during these dates.", Toast.LENGTH_LONG)
                        .show();
                return false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void setDates(String startDate, String endDate) {
        if (etStartDate != null && etEndDate != null) {
            try {
                if (startDate != null && !startDate.isEmpty()) {
                    Date start = dateFormat.parse(startDate);
                    if (start != null) {
                        etStartDate.setText(displayFormat.format(start));
                        etStartDate.setTag(startDate);
                    }
                }
                if (endDate != null && !endDate.isEmpty()) {
                    Date end = dateFormat.parse(endDate);
                    if (end != null) {
                        etEndDate.setText(displayFormat.format(end));
                        etEndDate.setTag(endDate);
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
