package com.example.voyagerbuds.utils;

import android.content.Context;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyHelper {
    // Approximate exchange rate: 1 USD = 25,450 VND
    private static final double USD_TO_VND_RATE = 25450.0;

    public static String formatCurrency(Context context, double amountInUSD) {
        String language = LocaleHelper.getLanguage(context);
        if ("vi".equals(language)) {
            double amountInVND = amountInUSD * USD_TO_VND_RATE;
            // Vietnamese currency format
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            return format.format(amountInVND);
        } else {
            // Default to USD
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
            return format.format(amountInUSD);
        }
    }
}
