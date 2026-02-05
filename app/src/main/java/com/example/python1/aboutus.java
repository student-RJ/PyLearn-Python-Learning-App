package com.example.python1;

import android.os.Bundle;
import android.widget.ImageView;

public class aboutus extends BaseActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_aboutus);

            ImageView backButton = findViewById(R.id.back_button);
            backButton.setOnClickListener(v -> finish()); // Close activity on back press
        }
    }
