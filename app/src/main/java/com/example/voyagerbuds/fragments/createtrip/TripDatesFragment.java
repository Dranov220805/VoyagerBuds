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
import com.example.voyagerbuds.utils.DateUtils;
import java.util.TimeZone;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class TripDatesFragment extends Fragment {

    private EditText etStartDate, etEndDate;
    private Button btnNext;
    private ImageView btnBack;
    private OnTripDatesEnteredListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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

        if (initialStartDate != null && initialEndDate != null) {
            setDates(initialStartDate, initialEndDate);
        }

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
            // MaterialDatePicker returns time in UTC epoch millis, convert to local date
            Instant instant = Instant.ofEpochMilli(selection);
            LocalDate selectedLocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

            String formattedDate = DateUtils.formatLocalDateToDbKey(selectedLocalDate);
            // Format for UI display (as Date at start of day in local timezone)
            Date displayDateObj = Date.from(selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            String displayDate = DateUtils.formatFullDate(getContext(), displayDateObj);

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
            Toast.makeText(getContext(), getString(R.string.prompt_select_start_date), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(endDate)) {
            Toast.makeText(getContext(), getString(R.string.prompt_select_end_date), Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            java.time.LocalDate s = DateUtils.parseDbDateToLocalDate(startDate);
            java.time.LocalDate e = DateUtils.parseDbDateToLocalDate(endDate);
            if (s != null && e != null && e.isBefore(s)) {
                Toast.makeText(getContext(), getString(R.string.error_end_date_after_start), Toast.LENGTH_SHORT).show();
                return false;
            }

            // Check for overlapping trips
            if (!databaseHelper.isDateRangeAvailable(startDate, endDate)) {
                Toast.makeText(getContext(), getString(R.string.toast_trip_date_conflict), Toast.LENGTH_LONG)
                        .show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private String initialStartDate;
    private String initialEndDate;

    public void setDates(String startDate, String endDate) {
        this.initialStartDate = startDate;
        this.initialEndDate = endDate;
        if (etStartDate != null && etEndDate != null) {
            try {
                Date start = dateFormat.parse(startDate);
                Date end = dateFormat.parse(endDate);
                if (start != null) {
                    etStartDate.setText(DateUtils.formatFullDate(getContext(), start));
                    etStartDate.setTag(startDate);
                }
                if (end != null) {
                    etEndDate.setText(DateUtils.formatFullDate(getContext(), end));
                    etEndDate.setTag(endDate);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
