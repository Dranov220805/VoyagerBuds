package com.example.voyagerbuds.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.voyagerbuds.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrencyHelper {
    private static final String API_KEY = BuildConfig.CURRENCY_API_KEY;
    private static final String API_URL = "https://api.freecurrencyapi.com/v1/latest?apikey=" + API_KEY
            + "&base_currency=USD&currencies=VND";

    private static final String PREF_NAME = "currency_prefs";
    private static final String KEY_EXCHANGE_RATE = "usd_to_vnd_rate";
    private static final String KEY_LAST_UPDATE = "last_exchange_rate_update";

    // Default fallback rate: 1 USD = 25,450 VND
    public static final double DEFAULT_USD_TO_VND_RATE = 25450.0;

    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;

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
        if (currency.equalsIgnoreCase("VND")) {
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
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUpdate > ONE_DAY_MILLIS) {
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

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject data = jsonResponse.getJSONObject("data");
                    double rate = data.getDouble("VND");

                    saveExchangeRate(context, rate);
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
        editor.apply();
    }
}
