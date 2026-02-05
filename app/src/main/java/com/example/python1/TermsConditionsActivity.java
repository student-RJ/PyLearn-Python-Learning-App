package com.example.python1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class TermsConditionsActivity extends AppCompatActivity {

    private CheckBox checkboxAccept;
    private Button btnContinue;
    private TextView txtAlreadyAccepted;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        checkboxAccept = findViewById(R.id.checkbox_accept);
        btnContinue = findViewById(R.id.btn_continue);
        txtAlreadyAccepted = findViewById(R.id.txt_already_accepted);
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Default UI states
        btnContinue.setEnabled(false);
        txtAlreadyAccepted.setVisibility(TextView.GONE); // Initially hidden

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Check if already accepted
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            Map<String, Object> termMap = (Map<String, Object>) snapshot.get("term_condition_status");
                            if (termMap != null && Boolean.TRUE.equals(termMap.get("accepted"))) {
                                txtAlreadyAccepted.setVisibility(TextView.VISIBLE);
                                checkboxAccept.setVisibility(CheckBox.GONE);
                                btnContinue.setVisibility(Button.GONE);
                            } else {
                                setupListeners(userId);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error checking terms status.", Toast.LENGTH_SHORT).show();
                    });
        }

        // Handle system insets for full screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupListeners(String userId) {
        checkboxAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnContinue.setEnabled(isChecked);
        });

        btnContinue.setOnClickListener(v -> {
            // Save acceptance in nested map
            Map<String, Object> termStatusMap = new HashMap<>();
            termStatusMap.put("accepted", true);
            termStatusMap.put("accepted_at", FieldValue.serverTimestamp());

            Map<String, Object> update = new HashMap<>();
            update.put("term_condition_status", termStatusMap);

            db.collection("users").document(userId)
                    .set(update, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Terms accepted", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
