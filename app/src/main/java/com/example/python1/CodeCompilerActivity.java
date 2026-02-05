package com.example.python1;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CodeCompilerActivity extends BaseActivity {

    private MaterialButton runCodeButton, copyOutputButton, clearCodeButton;
    private EditText codeEditor;
    private TextView outputTextView;
    private AdView mAdView;
    private SeekBar fontSizeSeekBar;
    private ImageView errorIcon;
    private TextView lineNumbers;
    private android.widget.ProgressBar progressBar;

    private JDoodleAPI jdoodleAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_compiler);

        // Toolbar setup with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Code Compiler");
        }

        // Initialize UI components
        runCodeButton = findViewById(R.id.btnRunCode);
        copyOutputButton = findViewById(R.id.btnCopyOutput);
        clearCodeButton = findViewById(R.id.btnClearCode);
        codeEditor = findViewById(R.id.codeEditor);
        outputTextView = findViewById(R.id.tvOutput);
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        errorIcon = findViewById(R.id.errorIcon);
        lineNumbers = findViewById(R.id.lineNumbers);
        progressBar = findViewById(R.id.progressBarRunning);

        // Initialize AdMob SDK
        // Initialize AdMob SDK
        MobileAds.initialize(this, initializationStatus -> {
            // Check subscription status after AdMob initialization
            checkSubscriptionAndLoadAd();
        });


        // Load AdMob ad
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Initialize Retrofit for JDoodle API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.jdoodle.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jdoodleAPI = retrofit.create(JDoodleAPI.class);

        // Run code button click
        runCodeButton.setOnClickListener(v -> {
            String code = codeEditor.getText().toString();
            if (!code.trim().isEmpty()) {
                executeCode(code);
            } else {
                outputTextView.setText("Please enter code.");
                errorIcon.setVisibility(ImageView.GONE);
            }
        });

        // Copy output button click
        copyOutputButton.setOnClickListener(v -> {
            String output = outputTextView.getText().toString();
            if (!output.trim().isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Code Output", output);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(CodeCompilerActivity.this, "Output copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CodeCompilerActivity.this, "Nothing to copy", Toast.LENGTH_SHORT).show();
            }
        });

        // Clear code button click
        clearCodeButton.setOnClickListener(v -> {
            codeEditor.setText("");
            outputTextView.setText("Output will appear here");
            errorIcon.setVisibility(ImageView.GONE);
            resetLineNumbers();
        });

        // Font size seek bar
        fontSizeSeekBar.setMax(30);
        fontSizeSeekBar.setProgress(16);  // default font size
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 8) progress = 8; // minimum font size
                codeEditor.setTextSize(progress);
                lineNumbers.setTextSize(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No action needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No action needed
            }
        });

        // Sync line numbers with code editor lines as user types
        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                updateLineNumbers(s.toString());
            }
        });

        // Initialize line numbers on start
        updateLineNumbers(codeEditor.getText().toString());
    }
    private void checkSubscriptionAndLoadAd() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // No user logged in - show ad
            loadBannerAd();
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        boolean isSubscribed = false;

                        // Check subscription status in multiple possible ways
                        if (document.contains("isSubscribed")) {
                            isSubscribed = Boolean.TRUE.equals(document.getBoolean("isSubscribed"));
                        } else if (document.contains("paymentData")) {
                            Map<String, Object> paymentData = (Map<String, Object>) document.get("paymentData");
                            if (paymentData != null) {
                                String currentPlan = (String) paymentData.get("currentPlan");
                                Boolean paymentStatus = (Boolean) paymentData.get("paymentStatus");
                                isSubscribed = currentPlan != null && !currentPlan.isEmpty()
                                        && paymentStatus != null && paymentStatus;
                            }
                        } else if (document.contains("subscriptionExpiry")) {
                            Date expiryDate = document.getDate("subscriptionExpiry");
                            isSubscribed = expiryDate != null && expiryDate.after(new Date());
                        }

                        // Only show ad if user is NOT subscribed
                        if (!isSubscribed) {
                            loadBannerAd();
                        } else {
                            // Hide the ad view for subscribed users
                            mAdView.setVisibility(View.GONE);
                        }
                    } else {
                        // Error checking status - default to showing ad
                        loadBannerAd();
                    }
                });
    }
    private void loadBannerAd() {
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mAdView.setVisibility(View.VISIBLE);
    }

    private void executeCode(String pythonCode) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        outputTextView.setText("");
        errorIcon.setVisibility(ImageView.GONE);

        JDoodleRequest request = new JDoodleRequest(
                "631ccc65ce4791ee33520e339c9d95be",
                "1a5a1722a024d5ee267e019be80d49d1620c80928d0fd7990603e90cf17dba10",
                pythonCode,
                "python3",
                "3"
        );

        jdoodleAPI.executeCode(request).enqueue(new Callback<JDoodleResponse>() {
            @Override
            public void onResponse(Call<JDoodleResponse> call, Response<JDoodleResponse> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    JDoodleResponse jdoodleResponse = response.body();
                    if (jdoodleResponse.getError() != null && !jdoodleResponse.getError().isEmpty()) {
                        outputTextView.setText("Error: " + jdoodleResponse.getError());
                        errorIcon.setVisibility(ImageView.VISIBLE);
                    } else {
                        outputTextView.setText(jdoodleResponse.getOutput());
                        errorIcon.setVisibility(ImageView.GONE);
                    }
                } else {
                    outputTextView.setText("Failed to execute code.");
                    errorIcon.setVisibility(ImageView.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<JDoodleResponse> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                outputTextView.setText("Failed to execute code: " + t.getMessage());
                errorIcon.setVisibility(ImageView.VISIBLE);
            }
        });
    }

    // Update line numbers based on code lines
    private void updateLineNumbers(String text) {
        int lines = text.split("\n").length;
        if (lines == 0) lines = 1;
        StringBuilder lineNumbersText = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            lineNumbersText.append(i).append("\n");
        }
        lineNumbers.setText(lineNumbersText.toString());
    }

    // Reset line numbers when clearing code
    private void resetLineNumbers() {
        lineNumbers.setText("1\n2\n3\n4\n5\n6\n7\n8\n9\n10");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle toolbar back button click
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

}
