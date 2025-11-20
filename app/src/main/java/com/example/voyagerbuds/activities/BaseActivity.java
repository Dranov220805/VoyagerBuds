package com.example.voyagerbuds.activities;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.voyagerbuds.utils.LocaleHelper;
import com.example.voyagerbuds.utils.ThemeHelper;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        ThemeHelper.initTheme(this);
        super.onCreate(savedInstanceState);
    }
}
