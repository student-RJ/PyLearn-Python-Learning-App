package com.example.python1;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackActivity extends BaseActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private RatingBar ratingBar;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private EditText feedbackDescription;
    private Button submitFeedbackButton;
    private MaterialAutoCompleteTextView feedbackCategory;
    private ImageView screenshotPreview;
    private TextView screenshotFilename;
    private Button attachButton;
    private ChipGroup surveyChipGroup;

    private Uri selectedImageUri;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind Views
        ratingBar = findViewById(R.id.feedback_rating_bar);
        feedbackDescription = findViewById(R.id.feedback_description);
        submitFeedbackButton = findViewById(R.id.submit_feedback_button);
        feedbackCategory = findViewById(R.id.feedback_category);
        screenshotPreview = findViewById(R.id.screenshot_preview);
        screenshotFilename = findViewById(R.id.screenshot_filename);
        attachButton = findViewById(R.id.attach_button);
        surveyChipGroup = findViewById(R.id.survey_chip_group);
        MaterialButton backButton = findViewById(R.id.back_button);

// ✅ CORRECT
// Make sure to assign an ID in XML

        // Populate dropdown
        String[] categories = {"Bug Report", "Feature Request", "UI Feedback", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        feedbackCategory.setAdapter(adapter);

        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                Toast.makeText(this, "Rated: " + rating + " stars", Toast.LENGTH_SHORT).show();
            }
        });

        attachButton.setOnClickListener(v -> openFileChooser());

        submitFeedbackButton.setOnClickListener(v -> submitFeedback());
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // Go back to the previous activity
            }
        });// Go back to the previous activity

    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            screenshotPreview.setImageURI(selectedImageUri);
            screenshotFilename.setText(selectedImageUri.getLastPathSegment());
        }
    }

    private void submitFeedback() {
        float rating = ratingBar.getRating();
        String description = feedbackDescription.getText().toString().trim();
        String category = feedbackCategory.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please rate the app", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please describe your feedback", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < surveyChipGroup.getChildCount(); i++) {
            View chipView = surveyChipGroup.getChildAt(i);
            if (chipView instanceof Chip && ((Chip) chipView).isChecked()) {
                selectedTags.add(((Chip) chipView).getText().toString());
            }
        }

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String docId = String.valueOf(System.currentTimeMillis());
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("rating", rating);
        feedback.put("comment", description);
        feedback.put("category", category);
        feedback.put("timestamp", Timestamp.now());
        feedback.put("surveyTags", selectedTags);

        if (selectedImageUri != null) {
            // Upload to Firebase Storage
            StorageReference fileRef = storageRef.child("feedback_screenshots/" + userId + "/" + docId + ".jpg");
            fileRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        feedback.put("screenshotUrl", uri.toString());
                        uploadFeedbackToFirestore(userId, docId, feedback);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            uploadFeedbackToFirestore(userId, docId, feedback);
        }
    }

    private void uploadFeedbackToFirestore(String userId, String docId, Map<String, Object> feedback) {
        db.collection("users")
                .document(userId)
                .collection("feedback")
                .document(docId)
                .set(feedback)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
