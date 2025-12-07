package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.Expense;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    private Context context;
    private List<Expense> expenses;
    private OnExpenseClickListener listener;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    // test
    public ExpenseAdapter(Context context, List<Expense> expenses, OnExpenseClickListener listener) {
        this.context = context;
        this.expenses = expenses;
        this.listener = listener;
    }

    public void updateExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.tvCategoryName.setText(expense.getCategory());

        // Parse note if it's JSON
        String noteDisplay = "";
        String rawNote = expense.getNote();
        if (rawNote != null && !rawNote.isEmpty()) {
            try {
                org.json.JSONObject json = new org.json.JSONObject(rawNote);
                if (json.has("text")) {
                    noteDisplay = json.getString("text");
                }
            } catch (org.json.JSONException e) {
                // Not JSON, use raw
                noteDisplay = rawNote;
            }
        }
        holder.tvExpenseNote.setText(noteDisplay);

        // Format amount
        java.text.NumberFormat numberFormat = java.text.NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);

        String currencySymbol = expense.getCurrency();
        if ("VND".equals(currencySymbol)) {
            currencySymbol = "VNĐ";
        }

        String formattedAmount = numberFormat.format(Math.abs(expense.getAmount()));
        String amountText = "-" + formattedAmount + " " + currencySymbol;

        // All expenses are shown in red with minus sign
        holder.tvExpenseAmount.setText(amountText);
        holder.tvExpenseAmount.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));

        // Set icon based on category (simplified logic for now)
        int iconRes = getIconForCategory(expense.getCategory());
        holder.ivCategoryIcon.setImageResource(iconRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpenseClick(expense);
            }
        });
    }

    private int getIconForCategory(String category) {
        if (category == null)
            return R.drawable.ic_amount;
        switch (category.toLowerCase()) {
            case "food":
            case "dining":
                return R.drawable.ic_dinner;
            case "transport":
            case "flight":
                return R.drawable.ic_airplane;
            case "hotel":
            case "accommodation":
                return R.drawable.ic_hotel;
            case "shopping":
                return R.drawable.ic_wallet;
            default:
                return R.drawable.ic_amount;
        }
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        TextView tvExpenseNote;
        TextView tvExpenseAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvExpenseNote = itemView.findViewById(R.id.tv_expense_note);
            tvExpenseAmount = itemView.findViewById(R.id.tv_expense_amount);
        }
    }
}
