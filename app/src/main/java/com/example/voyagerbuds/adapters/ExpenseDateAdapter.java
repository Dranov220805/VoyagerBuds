package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseDateAdapter extends RecyclerView.Adapter<ExpenseDateAdapter.ViewHolder> {

    private Context context;
    private List<Date> dates;
    private int selectedPosition = 0;
    private OnDateClickListener listener;
    private SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEE", Locale.getDefault());
    private SimpleDateFormat dayOfMonthFormat = new SimpleDateFormat("dd", Locale.getDefault());

    public interface OnDateClickListener {
        void onDateClick(Date date, int position);
    }

    public ExpenseDateAdapter(Context context, List<Date> dates, OnDateClickListener listener) {
        this.context = context;
        this.dates = dates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense_date, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Date date = dates.get(position);
        holder.tvDayOfWeek.setText(dayOfWeekFormat.format(date));
        holder.tvDayOfMonth.setText(dayOfMonthFormat.format(date));

        if (position == selectedPosition) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.main_color_voyager));
            holder.tvDayOfWeek.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvDayOfMonth.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background));
            holder.tvDayOfWeek.setTextColor(ContextCompat.getColor(context, R.color.text_medium));
            holder.tvDayOfMonth.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onDateClick(date, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvDayOfWeek;
        TextView tvDayOfMonth;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_date);
            tvDayOfWeek = itemView.findViewById(R.id.tv_day_of_week);
            tvDayOfMonth = itemView.findViewById(R.id.tv_day_of_month);
        }
    }
}
