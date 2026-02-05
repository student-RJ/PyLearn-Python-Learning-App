package com.example.python1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class UserProgressActivity extends BaseActivity {

    private static final String TAG = "UserProgressActivity";
    private final Random random = new Random();

    private ImageView backButton, imgProfile;
    private TextView tvUserName, tvProgressMessage, tvQuizzesAttempted, tvTotalScore,
            tvHighestScore, tvAverageScore, tvPercentage, tvLastAttempted, tvFastestTime;
    private PieChart pieChart;
    private RatingBar ratingBar;
    private RecyclerView recyclerTopicProgress;
    private TextView shareProgressText, resetProgressText;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TopicProgressAdapter topicProgressAdapter;
    private List<TopicProgressModel> topicList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_progress);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupRecyclerView();
        setClickListeners();
        configurePieChart();

        loadUserData();
        loadProgressData();

        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent(UserProgressActivity.this, profileActivity.class);
            startActivity(intent);
        });

    }

    private void initializeViews() {
        imgProfile = findViewById(R.id.imgProfile);
        backButton = findViewById(R.id.backButton);
        tvUserName = findViewById(R.id.tvUserName);
        tvProgressMessage = findViewById(R.id.tvProgressMessage);
        tvQuizzesAttempted = findViewById(R.id.tvQuizzesAttempted);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        tvHighestScore = findViewById(R.id.tvHighestScore);
        tvAverageScore = findViewById(R.id.tvAverageScore);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvLastAttempted = findViewById(R.id.tvLastAttempted);
        tvFastestTime = findViewById(R.id.tvFastestTime);
        pieChart = findViewById(R.id.pieChart);
        ratingBar = findViewById(R.id.ratingBar);
        recyclerTopicProgress = findViewById(R.id.recyclerTopicProgress);
        shareProgressText = findViewById(R.id.shareProgressText);
        resetProgressText = findViewById(R.id.resetProgressText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Make rating bar non-editable and set gold color
        ratingBar.setIsIndicator(true);
        ratingBar.setProgressTintList(ContextCompat.getColorStateList(this, R.color.gold));

    }

    private void configurePieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.animateY(1000);
    }

    private void setupRecyclerView() {
        recyclerTopicProgress.setLayoutManager(new LinearLayoutManager(this));
        topicProgressAdapter = new TopicProgressAdapter(topicList);
        recyclerTopicProgress.setAdapter(topicProgressAdapter);
        recyclerTopicProgress.setNestedScrollingEnabled(false);
    }

    private void setClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserData();
            loadProgressData();
            swipeRefreshLayout.setRefreshing(false);
        });

        shareProgressText.setOnClickListener(v -> shareUserProgress());
        resetProgressText.setOnClickListener(v -> resetUserProgress());
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Map<String, Object> profileDetails = (Map<String, Object>) document.get("Profile_details");

                        if (profileDetails != null) {
                            String name = (String) profileDetails.get("name");
                            String profileImageUrl = (String) profileDetails.get("profileImageUrl");

                            if (name != null) {
                                tvUserName.setText("Hello, " + name + " 👋");
                            }

                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.profile) // your default image
                                        .error(R.drawable.profile)       // fallback on error
                                        .into(imgProfile);               // ShapeableImageView
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }


    private void loadProgressData() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        resetProgressUI();

                        if (document.contains("overall_progress")) {
                            Map<String, Object> overallMap = (Map<String, Object>) document.get("overall_progress");

                            long quizzesAttempted = getLongValue(overallMap, "quizzes_attempted");
                            long totalScore = getLongValue(overallMap, "total_score");
                            long highestScore = getLongValue(overallMap, "highest_score");
                            double averageScore = getDoubleValue(overallMap, "average_score");
                            double percentage = getDoubleValue(overallMap, "percentage");

                            // Get last attempted timestamp and topic
                            Timestamp timestamp = (Timestamp) overallMap.get("last_attempted_timestamp");
                            long lastAttemptedTimestamp = (timestamp != null) ? timestamp.toDate().getTime() : 0;
                            String lastAttemptedTopic = getStringValue(overallMap, "last_attempted_topic", "No quizzes yet");
                            String lastAttemptedText = formatLastAttempted(lastAttemptedTimestamp, lastAttemptedTopic);


                            Log.d(TAG, "lastAttemptedTimestamp: " + lastAttemptedTimestamp);
                            Log.d(TAG, "lastAttemptedTopic: " + lastAttemptedTopic);


                            Log.d(TAG, "Formatted Last Attempted Text: " + lastAttemptedText);

                            long fastestTimeMs = getLongValue(overallMap, "fastest_time");
                            String fastestTimeText = formatTimeDuration(fastestTimeMs);

                            Log.d(TAG, "Fastest Time: " + fastestTimeMs + " (" + fastestTimeText + ")");

                            // Update UI
                            updateProgressUI(quizzesAttempted, totalScore, highestScore, averageScore,
                                    percentage, lastAttemptedText, fastestTimeText);

                            updatePieChart(percentage);
                            updateRatingBar(percentage);
                        }

                        loadTopicProgress(document);
                    } else {
                        Log.w(TAG, "Document does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load progress data", e);
                    Toast.makeText(this, "Failed to load progress data", Toast.LENGTH_SHORT).show();
                });
    }


    private String formatLastAttempted(long timestamp, String topic) {
        if (timestamp == 0) {
            Log.d(TAG, "formatLastAttempted: Timestamp is 0. Returning 'No quizzes yet'");
            return "No quizzes yet";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        String dateString = sdf.format(new Date(timestamp));
        String result = String.format("Last: %s (%s)", topic, dateString);
        Log.d(TAG, "formatLastAttempted: Returning -> " + result);
        return result;
    }


    private String formatTimeDuration(long milliseconds) {
        if (milliseconds == 0) {
            return "N/A";
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(minutes);

        return String.format(Locale.getDefault(), "%02d min %02d sec", minutes, seconds);
    }

    private void updateProgressUI(long quizzesAttempted, long totalScore, long highestScore,
                                  double averageScore, double percentage, String lastAttempted,
                                  String fastestTime) {
        tvQuizzesAttempted.setText("📝 Quizzes Attempted: " + quizzesAttempted);
        tvTotalScore.setText("🏆 Total Score: " + totalScore);
        tvHighestScore.setText("🥇 Highest Score: " + highestScore);
        tvAverageScore.setText(String.format("📈 Average Score: %.2f", averageScore));
        tvPercentage.setText("🔢 Percentage: " + String.format("%.2f", percentage) + "%");
        tvLastAttempted.setText(lastAttempted);
        tvFastestTime.setText("⏱ " + fastestTime);

        if (percentage >= 90) {
            tvProgressMessage.setText("Outstanding performance! 🌟");
        } else if (percentage >= 75) {
            tvProgressMessage.setText("Excellent work! Keep it up! 💯");
        } else if (percentage >= 50) {
            tvProgressMessage.setText("Good progress! You're improving! 👍");
        } else if (percentage >= 25) {
            tvProgressMessage.setText("Keep practicing! You'll get better! 💪");
        } else {
            tvProgressMessage.setText("Let's get started! You can do it! 🚀");
        }
    }

    private void updatePieChart(double percentage) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) percentage, "Achieved"));
        entries.add(new PieEntry((float) (100 - percentage), "Remaining"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getRandomColors());
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        // Format values as percentages
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.1f%%", value);
            }
        });

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void updateRatingBar(double percentage) {
        // Calculate rating based on percentage (0-100% maps to 0-5 stars)
        // Using a non-linear mapping for better distribution
        float rating;
        if (percentage >= 90) {
            rating = 5f;
        } else if (percentage >= 75) {
            rating = 4f;
        } else if (percentage >= 50) {
            rating = 3f;
        } else if (percentage >= 25) {
            rating = 2f;
        } else if (percentage > 0) {
            rating = 1f;
        } else {
            rating = 0f;
        }
        ratingBar.setRating(rating);
    }

    private void loadTopicProgress(DocumentSnapshot document) {
        topicList.clear();
        if (document.contains("topics_progress")) {
            Map<String, Object> topicsMap = (Map<String, Object>) document.get("topics_progress");

            for (String topicName : topicsMap.keySet()) {
                Map<String, Object> topicData = (Map<String, Object>) topicsMap.get(topicName);

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

                topicList.add(model);
            }
            topicProgressAdapter.notifyDataSetChanged();
        }
    }

    private int[] getRandomColors() {
        // Generate random colors for the pie chart
        return new int[]{
                Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)),
                Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        };
    }

    private void resetProgressUI() {
        tvQuizzesAttempted.setText("📝 Quizzes Attempted: 0");
        tvTotalScore.setText("🏆 Total Score: 0");
        tvHighestScore.setText("🥇 Highest Score: 0");
        tvAverageScore.setText("📈 Average Score: 0.0");
        tvPercentage.setText("🔢 Percentage: 0%");
        tvLastAttempted.setText("No quizzes yet");
        tvFastestTime.setText("N/A");
        tvProgressMessage.setText("No progress yet. Start learning now! 📚");
        ratingBar.setRating(0f);

        pieChart.clear();
        pieChart.invalidate();

        topicList.clear();
        topicProgressAdapter.notifyDataSetChanged();
    }

    private void shareUserProgress() {
        String name = tvUserName.getText().toString();
        String summary = name + "\n" +
                tvQuizzesAttempted.getText().toString() + "\n" +
                tvTotalScore.getText().toString() + "\n" +
                tvHighestScore.getText().toString() + "\n" +
                tvAverageScore.getText().toString() + "\n" +
                tvPercentage.getText().toString() + "\n" +
                "Last Attempt: " + tvLastAttempted.getText().toString() + "\n" +
                "Fastest Time: " + tvFastestTime.getText().toString() + "\n" +
                "Overall Rating: " + ratingBar.getRating() + "/5.0\n" +
                "#PyLearn #MyLearningJourney";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, summary);
        startActivity(Intent.createChooser(intent, "Share your progress via"));
    }

    private void resetUserProgress() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Progress")
                .setMessage("Are you sure you want to reset all your progress?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String userId = mAuth.getCurrentUser().getUid();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("overall_progress", FieldValue.delete());
                    updates.put("topics_progress", FieldValue.delete());

                    db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                resetProgressUI();
                                Toast.makeText(this, "Progress reset successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to reset progress", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Helper methods for data extraction
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