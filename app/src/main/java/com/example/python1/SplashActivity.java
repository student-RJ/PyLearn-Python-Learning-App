package com.example.python1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class SplashActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashactivity);

        // Delay for 3 seconds and open MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, Sign_in.class);
            startActivity(intent);
            finish(); // Close SplashActivity so it's not in back stack
        }, 3000); // 3000 milliseconds = 3 seconds
    }
}

