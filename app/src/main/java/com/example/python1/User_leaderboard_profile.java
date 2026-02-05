package com.example.python1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class User_leaderboard_profile extends BaseActivity {

    private TextView userName, userRank, overallProgress, headingUsername;
    private ImageView profileImage, backIcon;
    private MaterialButton shareProfileBtn;
    private RecyclerView recyclerTopicProgress;
    private TopicProgressAdapter topicProgressAdapter;
    private List<TopicProgressModel> topicProgressList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_leaderboard_profile);

        // Initialize all UI elements
        initializeViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Get the userId and rank from intent
        String userId = getIntent().getStringExtra("USER_ID");
        String userRankText = getIntent().getStringExtra("USER_RANK");

        // Set rank if available
        if (userRankText != null) {
            userRank.setText("🏆 Rank: " + userRankText);
        }

        // Fetch user data
        if (userId != null) {
            fetchUserData(userId);
        } else {
            showError("User ID not found");
        }

        // Set click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        userName = findViewById(R.id.userName);
        userRank = findViewById(R.id.userRank);
        overallProgress = findViewById(R.id.overallProgress);
        profileImage = findViewById(R.id.profileImage);
        headingUsername = findViewById(R.id.headingUsername);
        backIcon = findViewById(R.id.backIcon);
        shareProfileBtn = findViewById(R.id.shareProfileBtn);
        recyclerTopicProgress = findViewById(R.id.recyclerTopicProgress);
    }

    private void setupRecyclerView() {
        topicProgressList = new ArrayList<>();
        topicProgressAdapter = new TopicProgressAdapter(topicProgressList);
        recyclerTopicProgress.setLayoutManager(new LinearLayoutManager(this));
        recyclerTopicProgress.setNestedScrollingEnabled(false);
        recyclerTopicProgress.setAdapter(topicProgressAdapter);
    }

    private void setupClickListeners() {
        // Back button
        backIcon.setOnClickListener(v -> finish());

        // Share Profile button
        shareProfileBtn.setOnClickListener(v -> shareProfile());
    }

    private void fetchUserData(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                updateUI(documentSnapshot);
            } else {
                showError("User data not found");
            }
        }).addOnFailureListener(e -> {
            showError("Error fetching user data");
        });
    }

    private void updateUI(DocumentSnapshot documentSnapshot) {
        Map<String, Object> profileDetails = (Map<String, Object>) documentSnapshot.get("Profile_details");

        if (profileDetails != null) {
            // Set user name (in card and title bar)
            String name = getStringValue(profileDetails, "name", "N/A");
            if (!name.equals("N/A")) {
                userName.setText("👤 " + name);
                headingUsername.setText("🏅 " + name + "'s Profile");
            }

            // Load profile image
            String profileImageUrl = getStringValue(profileDetails, "profileImageUrl", "");
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile) // fallback on error
                    .into(profileImage);

            // Set social media links
            setSocialLinks(profileDetails);
        }

        // Set overall progress
        Double overallPercentage = getDoubleValue((Map<String, Object>) documentSnapshot.get("overall_progress"), "percentage");
        if (overallPercentage != null) {
            overallProgress.setText("📈 Overall Progress: " + String.format("%.0f%%", overallPercentage));
        }

        // Load topic progress
        loadTopicProgress(documentSnapshot);
    }

    private void setSocialLinks(Map<String, Object> profileDetails) {
        String[] platforms = {"instagram", "facebook", "twitter"};
        int[] viewIds = {R.id.instagramIcon, R.id.facebookIcon, R.id.twitterIcon};

        for (int i = 0; i < platforms.length; i++) {
            String url = getStringValue(profileDetails, platforms[i] + "Url", "");
            ImageView icon = findViewById(viewIds[i]);

            if (!url.isEmpty()) {
                icon.setOnClickListener(v -> openUrl(url));
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }
        }
    }

    private void loadTopicProgress(DocumentSnapshot documentSnapshot) {
        topicProgressList.clear();
        Map<String, Object> topicsProgress = (Map<String, Object>) documentSnapshot.get("topics_progress");

        if (topicsProgress != null) {
            Log.d("TOPIC_DEBUG", "Topics count: " + topicsProgress.size());

            for (Map.Entry<String, Object> entry : topicsProgress.entrySet()) {
                String topicName = entry.getKey();
                Object value = entry.getValue();

                Log.d("TOPIC_DEBUG", "Processing topic: " + topicName + ", value class: " + value.getClass().getSimpleName());

                if (!(value instanceof Map)) {
                    Log.e("TOPIC_DEBUG", "Invalid topic data for: " + topicName);
                    continue;
                }

                Map<String, Object> topicData = (Map<String, Object>) value;

                try {
                    double averageScore = getDoubleValue(topicData, "average_score");
                    long bestTimeSpent = getLongValue(topicData, "best_time_spent");
                    long highestScore = getLongValue(topicData, "highest_score");
                    boolean quizCompleted = getBooleanValue(topicData, "quiz_completed");
                    long quizzesAttempted = getLongValue(topicData, "quizzes_attempted");
                    long totalScore = getLongValue(topicData, "total_score");
                    long totalTimeSpent = getLongValue(topicData, "total_time_spent");

                    double percentage = 0;
                    if (quizzesAttempted > 0) {
                        percentage = (totalScore * 100.0) / quizzesAttempted;
                    }

                    TopicProgressModel model = new TopicProgressModel(
                            topicName,
                            totalScore,
                            quizzesAttempted,
                            percentage,
                            averageScore,
                            quizzesAttempted,
                            bestTimeSpent,
                            highestScore,
                            totalScore,
                            totalTimeSpent,
                            quizCompleted
                    );
                    topicProgressList.add(model);
                } catch (Exception e) {
                    Log.e("TOPIC_DEBUG", "Error parsing topic: " + topicName, e);
                }
            }

            Log.d("TOPIC_DEBUG", "Final list size: " + topicProgressList.size());
            topicProgressAdapter.notifyDataSetChanged();
        }
    }


    private void shareProfile() {
        String shareText = "Check out " + userName.getText().toString() + "'s learning progress!";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            showError("Couldn't open link");
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Helper methods for data extraction (copied from UserProgressActivity for consistency)
    private long getLongValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return 0;
    }

    private double getDoubleValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0;
    }

    private boolean getBooleanValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return false;
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return false;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        if (val instanceof String) return (String) val;
        return defaultValue;
    }
}