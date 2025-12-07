package com.example.voyagerbuds.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrencyHelper {
    // Using free exchange-api from https://github.com/fawazahmed0/exchange-api
    // Endpoint:
    // https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json
    private static final String API_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json";

    private static final String PREF_NAME = "currency_prefs";
    private static final String KEY_EXCHANGE_RATE = "usd_to_vnd_rate";
    private static final String KEY_LAST_UPDATE = "last_exchange_rate_update";
    private static final String KEY_LAST_FETCH_DATE = "last_fetch_date";

    // Default fallback rate: 1 USD = 25,450 VND
    public static final double DEFAULT_USD_TO_VND_RATE = 25450.0;

    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;

    /**
     * Format amount based on language preference
     * English: converts to USD and displays with $
     * Vietnamese: converts to VND and displays with VNĐ
     */
    public static String formatAmountByLanguage(Context context, double amount, String storedCurrency) {
        String language = LocaleHelper.getLanguage(context);

        // Normalize currency input - handle both "VND" and "VNĐ"
        if (storedCurrency == null || storedCurrency.isEmpty()) {
            storedCurrency = "USD";
        }
        storedCurrency = storedCurrency.trim().toUpperCase().replace("Đ", "D");

        if ("vi".equals(language)) {
            // Convert to VND if needed
            double amountInVND = amount;
            if ("USD".equals(storedCurrency)) {
                double rate = getExchangeRate(context);
                amountInVND = amount * rate;
            }
            // Use explicit Vietnamese locale for formatting
            NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            numberFormat.setMaximumFractionDigits(0);
            numberFormat.setMinimumFractionDigits(0);
            String formattedAmount = numberFormat.format(Math.abs(amountInVND));
            return formattedAmount + " VNĐ";
        } else {
            // Convert to USD if needed
            double amountInUSD = amount;
            if ("VND".equals(storedCurrency)) {
                double rate = getExchangeRate(context);
                amountInUSD = amount / rate;
            }
            // Use explicit US locale for formatting
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            numberFormat.setMaximumFractionDigits(2);
            numberFormat.setMinimumFractionDigits(2);
            String formattedAmount = numberFormat.format(Math.abs(amountInUSD));
            return "$" + formattedAmount;
        }
    }

    public static String formatCurrency(Context context, double amountInUSD) {
        String language = LocaleHelper.getLanguage(context);
        if ("vi".equals(language)) {
            double rate = getExchangeRate(context);
            double amountInVND = amountInUSD * rate;
            // Vietnamese currency format
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            return format.format(amountInVND);
        } else {
            // Default to USD
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
            return format.format(amountInUSD);
        }
    }

    public static double convertToUSD(Context context, double amount, String currency) {
        if (currency == null || currency.isEmpty())
            return amount;
        // Normalize currency - handle both "VND" and "VNĐ"
        String normalizedCurrency = currency.trim().toUpperCase().replace("Đ", "D");
        if ("VND".equals(normalizedCurrency)) {
            double rate = getExchangeRate(context);
            return amount / rate;
        }
        return amount;
    }

    public static double getExchangeRate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_EXCHANGE_RATE, (float) DEFAULT_USD_TO_VND_RATE);
    }

    public static void fetchExchangeRateIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastFetchDate = prefs.getString(KEY_LAST_FETCH_DATE, "");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        // Only fetch once per day based on calendar date
        if (!currentDate.equals(lastFetchDate)) {
            fetchExchangeRate(context.getApplicationContext());
        }
    }

    private static void fetchExchangeRate(Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Parse response from exchange-api
                    // Response format: {"date":"2024-12-05","usd":{"vnd":25450.0,...}}
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject usdRates = jsonResponse.getJSONObject("usd");
                    double rate = usdRates.getDouble("vnd");

                    saveExchangeRate(context, rate);
                    Log.d("CurrencyHelper", "Exchange rate updated: 1 USD = " + rate + " VND");
                } else {
                    Log.e("CurrencyHelper", "Failed to fetch exchange rate. Response code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("CurrencyHelper", "Error fetching exchange rate", e);
            }
        });
    }

    private static void saveExchangeRate(Context context, double rate) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_EXCHANGE_RATE, (float) rate);
        editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        editor.putString(KEY_LAST_FETCH_DATE, currentDate);
        editor.apply();
        Log.d("CurrencyHelper", "Exchange rate saved: " + rate + " on " + currentDate);
    }

    /**
     * Get the last fetch date for debugging purposes
     * 
     * @return Last fetch date in yyyy-MM-dd format, or "Never" if not fetched yet
     */
    public static String getLastFetchDate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastFetchDate = prefs.getString(KEY_LAST_FETCH_DATE, "");
        return lastFetchDate.isEmpty() ? "Never" : lastFetchDate;
    }
}
