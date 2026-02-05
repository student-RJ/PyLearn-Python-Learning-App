package com.example.python1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    protected SharedPreferences preferences;
    private final BroadcastReceiver fontSizeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            recreate(); // Recreate activity to apply new font scale
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("user_settings", MODE_PRIVATE);
        int sizePref = prefs.getInt("font_size", 1); // Default: Medium
        float scale;

        switch (sizePref) {
            case 0:
                scale = 0.85f;
                break;
            case 1:
                scale = 1.0f;
                break;
            case 2:
                scale = 1.15f;
                break;
            default:
                scale = 1.0f;
        }

        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.fontScale = scale;

        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("user_settings", MODE_PRIVATE);

        // Register broadcast receiver
        registerReceiver(fontSizeReceiver, new IntentFilter("com.example.ACTION_FONT_SIZE_CHANGED"), Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(fontSizeReceiver);
    }
}
