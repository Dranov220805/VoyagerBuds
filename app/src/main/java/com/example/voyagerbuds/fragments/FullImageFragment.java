package com.example.voyagerbuds.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.adapters.FullImageAdapter;
import com.example.voyagerbuds.database.DatabaseHelper;
import com.example.voyagerbuds.models.Capture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FullImageFragment extends Fragment {

    private static final String ARG_CAPTURES = "captures";
    private static final String ARG_POSITION = "position";

    private List<Capture> captures;
    private int startPosition;
    private ViewPager2 viewPager;
    private FullImageAdapter adapter;
    private TextView tvDateTime;
    private TextView tvCaption;
    private LinearLayout topBar;
    private LinearLayout bottomBar;
    private boolean areBarsVisible = true;
    private DatabaseHelper databaseHelper;

    public static FullImageFragment newInstance(List<Capture> captures, int position) {
        FullImageFragment fragment = new FullImageFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CAPTURES, new ArrayList<>(captures));
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            captures = (List<Capture>) getArguments().getSerializable(ARG_CAPTURES);
            startPosition = getArguments().getInt(ARG_POSITION);
        }
        databaseHelper = new DatabaseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_full_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.view_pager);
        tvDateTime = view.findViewById(R.id.tv_date_time);
        tvCaption = view.findViewById(R.id.tv_caption);
        topBar = view.findViewById(R.id.top_bar);
        bottomBar = view.findViewById(R.id.bottom_bar);
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        ImageButton btnInfo = view.findViewById(R.id.btn_info);
        ImageButton btnDelete = view.findViewById(R.id.btn_delete);

        adapter = new FullImageAdapter(requireContext(), captures);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition, false);

        updateOverlay(startPosition);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateOverlay(position);
            }
        });

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        btnInfo.setOnClickListener(v -> {
            int currentPos = viewPager.getCurrentItem();
            if (currentPos >= 0 && currentPos < captures.size()) {
                showInfoDialog(captures.get(currentPos));
            }
        });

        btnDelete.setOnClickListener(v -> {
            int currentPos = viewPager.getCurrentItem();
            if (currentPos >= 0 && currentPos < captures.size()) {
                confirmDelete(currentPos);
            }
        });

        // Toggle bars on click (this would require the adapter/viewholder to pass click
        // events,
        // but for now we can just leave them visible or add a click listener to the
        // viewpager itself if possible)
    }

    private void updateOverlay(int position) {
        if (position >= 0 && position < captures.size()) {
            Capture capture = captures.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            tvDateTime.setText(sdf.format(new Date(capture.getCapturedAt())));

            if (capture.getDescription() != null && !capture.getDescription().isEmpty()) {
                tvCaption.setText(capture.getDescription());
                tvCaption.setVisibility(View.VISIBLE);
            } else {
                tvCaption.setVisibility(View.GONE);
            }
        }
    }

    private void showInfoDialog(Capture capture) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.image_details_title))
                .setMessage(getString(R.string.image_details_message,
                        capture.getMediaPath(),
                        capture.getMediaType(),
                        new Date(capture.getCapturedAt()).toString()))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private void confirmDelete(int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_photo_title))
                .setMessage(getString(R.string.delete_photo_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    deleteCapture(position);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deleteCapture(int position) {
        Capture capture = captures.get(position);

        // Delete from DB
        if (capture.getCaptureId() != -1) {
            databaseHelper.deleteCapture(capture.getCaptureId());
        } else {
            // It's a schedule/expense image, we can't easily delete it from here without
            // more logic
            // For now, just show a message
            Toast.makeText(requireContext(), getString(R.string.cannot_delete_schedule_expense), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Remove from list
        captures.remove(position);
        adapter.notifyItemRemoved(position);

        if (captures.isEmpty()) {
            requireActivity().onBackPressed();
        } else {
            // Update overlay for new current item
            // ViewPager automatically adjusts current item
            // But we might need to force update if it was the last item
            int newPos = viewPager.getCurrentItem();
            updateOverlay(newPos);
        }

        Toast.makeText(requireContext(), getString(R.string.photo_deleted), Toast.LENGTH_SHORT).show();
    }
}
