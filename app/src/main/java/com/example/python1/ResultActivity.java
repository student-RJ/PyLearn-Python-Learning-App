package com.example.python1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.github.lzyzsd.circleprogress.CircleProgress;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ResultActivity extends BaseActivity {

    private TextView tvGreeting, tvScore, tvCorrect, tvIncorrect, tvUnattempted, tvTimeTaken, tvMessage;
    private CircleProgress circleProgress;
    private MaterialButton btnShare, btnRetry;
    private ImageButton btnBack;
    private ImageView ivUserAvatar;
    private BarChart barChart;
    private PieChart pieChart;
    private CardView cardCorrect, cardIncorrect, cardUnattempted;
    private LinearLayout layoutAchievements, layoutBadges;

    private int correctAnswers, incorrectAnswers, unattempted;
    private int totalQuestions;
    private int scorePercentage;
    private long totalSessionTime = 0;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private TextView tvNoAchievements, tvNoBadges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Initialize Views
        tvGreeting = findViewById(R.id.tv_greeting);
        tvScore = findViewById(R.id.tv_score);
        tvCorrect = findViewById(R.id.tv_correct);
        tvIncorrect = findViewById(R.id.tv_incorrect);
        tvUnattempted = findViewById(R.id.tv_unattempted);
        tvTimeTaken = findViewById(R.id.tv_time_taken);
        tvMessage = findViewById(R.id.tv_message);
        circleProgress = findViewById(R.id.circle_progress);
        btnShare = findViewById(R.id.btn_share);
        btnRetry = findViewById(R.id.btn_retry);
        btnBack = findViewById(R.id.btn_back);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        barChart = findViewById(R.id.bar_chart);
        pieChart = findViewById(R.id.pie_chart);
        cardCorrect = findViewById(R.id.card_correct);
        cardIncorrect = findViewById(R.id.card_incorrect);
        cardUnattempted = findViewById(R.id.card_unattempted);
        layoutAchievements = findViewById(R.id.layout_achievements);
        layoutBadges = findViewById(R.id.layout_badges);
        tvNoAchievements = findViewById(R.id.tv_no_achievements); // <--- ADD THIS
        tvNoBadges = findViewById(R.id.tv_no_badges);

        // Firebase Initialization
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Retrieve quiz results from intent
        correctAnswers = getIntent().getIntExtra("correctAnswers", 0);
        incorrectAnswers = getIntent().getIntExtra("incorrectAnswers", 0);
        unattempted = getIntent().getIntExtra("unattemptedAnswers", 0);
        totalQuestions = getIntent().getIntExtra("totalQuestions", 1);
        totalSessionTime = getIntent().getLongExtra("totalSessionTime", 0);

        ArrayList<String> correctQuestions = getIntent().getStringArrayListExtra("correctQuestions");
        ArrayList<String> incorrectQuestions = getIntent().getStringArrayListExtra("incorrectQuestions");
        ArrayList<String> unattemptedQuestions = getIntent().getStringArrayListExtra("unattemptedQuestions");

        // Calculate Score Percentage
        scorePercentage = (int) (((double) correctAnswers / totalQuestions) * 100);

        // Set click listeners for summary cards
        cardCorrect.setOnClickListener(v -> showSummaryDialog("Correct Answers", correctQuestions));
        cardIncorrect.setOnClickListener(v -> showSummaryDialog("Incorrect Answers", incorrectQuestions));
        cardUnattempted.setOnClickListener(v -> showSummaryDialog("Unattempted Questions", unattemptedQuestions));

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Update UI
        updateGreeting();
        updateScoreUI();
        setupCharts();
        checkAchievements();
        checkBadges();

        // Retry Button Click
        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, QuizActivity.class);
            startActivity(intent);
            finish();
        });

        // Share Score Button Click
        btnShare.setOnClickListener(v -> shareScore());

        // Update user progress
        String selectedTopic = getIntent().getStringExtra("selectedTopic");
        if (selectedTopic == null || selectedTopic.isEmpty()) {
            selectedTopic = "Unknown Topic";
        }
        updateUserProgress(selectedTopic);
    }

    private void setupCharts() {
        // Bar Chart Setup - Performance Breakdown
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(0, correctAnswers));
        barEntries.add(new BarEntry(1, incorrectAnswers));
        barEntries.add(new BarEntry(2, unattempted));

        BarDataSet barDataSet = new BarDataSet(barEntries, "Question Breakdown");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setValueTextSize(12f);

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);

        // Customize bar chart appearance
        barChart.getDescription().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0);
        barChart.getAxisRight().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{"Correct", "Incorrect", "Unattempted"}));
        barChart.getXAxis().setTextSize(12f);
        barChart.getXAxis().setTextColor(Color.BLACK);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();

        // Pie Chart Setup - Score Distribution
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(correctAnswers, "Correct"));
        pieEntries.add(new PieEntry(incorrectAnswers, "Incorrect"));
        if (unattempted > 0) {
            pieEntries.add(new PieEntry(unattempted, "Unattempted"));
        }

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(new int[]{Color.GREEN, Color.RED, Color.GRAY});
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(12f);

        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(pieData);

        // Customize pie chart appearance
        pieChart.getDescription().setEnabled(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setHoleRadius(30f);
        pieChart.setTransparentCircleRadius(35f);
        pieChart.setDrawEntryLabels(true);
        pieChart.setUsePercentValues(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void checkAchievements() {
        // Clear existing views (except the default message)
        layoutAchievements.removeAllViews(); // This will remove tv_no_achievements if it's a direct child

        // Add achievements based on performance
        ArrayList<String> achievements = new ArrayList<>();

        if (scorePercentage == 100) {
            achievements.add("Perfect Score");
        }

        if (scorePercentage >= 90) {
            achievements.add("Top Performer");
        }

        if (correctAnswers >= totalQuestions / 2 && correctAnswers == totalQuestions) {
            achievements.add("No Mistakes");
        }

        if (totalSessionTime < (totalQuestions * 15000)) { // Less than 15 seconds per question
            achievements.add("Speed Runner");
        }

        if (achievements.isEmpty()) {
            tvNoAchievements.setVisibility(View.VISIBLE); // Use the initialized variable
        } else {
            tvNoAchievements.setVisibility(View.GONE); // Use the initialized variable

            for (String achievement : achievements) {
                TextView achievementView = new TextView(this);
                achievementView.setText("✓ " + achievement);
                achievementView.setTextSize(14);
                achievementView.setTextColor(getResources().getColor(R.color.white));
                achievementView.setBackgroundResource(R.drawable.achviemnt_trophy);
                achievementView.setPadding(16, 8, 16, 8);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 16, 0);
                achievementView.setLayoutParams(params);

                layoutAchievements.addView(achievementView);
            }
            // Re-add tvNoAchievements if it was removed by removeAllViews() and you want it to reappear later
            // Or, better yet, manage its visibility without removing it from the layout.
            // A better approach is to keep tvNoAchievements in the layout and just toggle its visibility.
            // If you remove all views, you'll need to add it back if achievements become empty again.
            // The current XML snippet has tv_no_achievements as a direct child of layout_achievements.
            // So, removeAllViews() will remove it.
            // To fix this, you should set tv_no_achievements to GONE by default in XML,
            // and only make it VISIBLE if achievements are empty.
            // And when adding dynamic views, ensure they are added *after* tv_no_achievements,
            // or simply remove the tv_no_achievements from the layout if achievements are present.
            // A more robust way is to have a separate container for dynamic achievements.
        }
    }

    private void checkBadges() {
        // Clear existing views (except the default message)
        layoutBadges.removeAllViews(); // This will remove tv_no_badges if it's a direct child

        // Add badges based on performance
        ArrayList<String> badges = new ArrayList<>();

        if (scorePercentage >= 80) {
            badges.add("Gold");
        } else if (scorePercentage >= 60) {
            badges.add("Silver");
        } else if (scorePercentage >= 40) {
            badges.add("Bronze");
        }

        if (correctAnswers == totalQuestions) {
            badges.add("Perfect");
        }

        if (badges.isEmpty()) {
            tvNoBadges.setVisibility(View.VISIBLE); // Use the initialized variable
        } else {
            tvNoBadges.setVisibility(View.GONE); // Use the initialized variable

            for (String badge : badges) {
                ImageView badgeView = new ImageView(this);

                // Set appropriate badge icon based on badge type
                switch (badge.toLowerCase()) {
                    case "gold":
                        badgeView.setImageResource(R.drawable.gold);
                        break;
                    case "silver":
                        badgeView.setImageResource(R.drawable.silver);
                        break;
                    case "bronze":
                        badgeView.setImageResource(R.drawable.bronze);
                        break;
                    case "perfect":
                        badgeView.setImageResource(R.drawable.left_icon);
                        break;
                    default:
                        badgeView.setImageResource(R.drawable.badge);
                }

                badgeView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
                badgeView.setPadding(8, 8, 8, 8);

                // Add tooltip with badge name
                badgeView.setContentDescription(badge + " Badge");

                layoutBadges.addView(badgeView);
            }
            // Similar to achievements, if removeAllViews() is used, tvNoBadges will be removed.
            // Consider setting its visibility instead of removing it from the parent.
        }
    }

    private void updateUserProgress(String topicName) {
        if (user == null) return;
        String uid = user.getUid();
        DocumentReference userDocRef = db.collection("users").document(uid);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;

            Map<String, Object> overall = (Map<String, Object>) documentSnapshot.get("overall_progress");
            Map<String, Object> topicsProgress = (Map<String, Object>) documentSnapshot.get("topics_progress");

            if (topicsProgress == null) topicsProgress = new HashMap<>();
            if (overall == null) overall = new HashMap<>();

            // --- Update topic progress ---

            Map<String, Object> topicProgress = topicsProgress.containsKey(topicName) ?
                    (Map<String, Object>) topicsProgress.get(topicName) : new HashMap<>();

            long topicAttempts = topicProgress.get("quizzes_attempted") != null ? (Long) topicProgress.get("quizzes_attempted") : 0;
            long topicTotalScore = topicProgress.get("total_score") != null ? (Long) topicProgress.get("total_score") : 0;
            long topicHighestScore = topicProgress.get("highest_score") != null ? (Long) topicProgress.get("highest_score") : 0;
            long topicBestTime = topicProgress.get("best_time_spent") != null ? (Long) topicProgress.get("best_time_spent") : Long.MAX_VALUE;
            long topicTotalTime = topicProgress.get("total_time_spent") != null ? (Long) topicProgress.get("total_time_spent") : 0;

            long newTopicAttempts = topicAttempts + 1;
            long newTopicTotalScore = topicTotalScore + correctAnswers;
            long newTopicHighestScore = Math.max(topicHighestScore, correctAnswers);
            long newTopicBestTime = totalSessionTime < topicBestTime ? totalSessionTime : topicBestTime;
            long newTopicTotalTime = topicTotalTime + totalSessionTime;
            long newTopicAvgScore = newTopicTotalScore / newTopicAttempts;

            Map<String, Object> updatedTopicProgress = new HashMap<>();
            updatedTopicProgress.put("quizzes_attempted", newTopicAttempts);
            updatedTopicProgress.put("total_score", newTopicTotalScore);
            updatedTopicProgress.put("highest_score", newTopicHighestScore);
            updatedTopicProgress.put("best_time_spent", newTopicBestTime);
            updatedTopicProgress.put("total_time_spent", newTopicTotalTime);
            updatedTopicProgress.put("average_score", newTopicAvgScore);
            updatedTopicProgress.put("quiz_completed", true);

            topicsProgress.put(topicName, updatedTopicProgress);

            // --- Aggregate overall progress ---

            com.google.firebase.Timestamp currentTimestamp = com.google.firebase.Timestamp.now();

            long overallAttempts = 0;
            long overallTotalScore = 0;
            long overallHighestScore = 0;
            long overallFastestTime = Long.MAX_VALUE;
            long overallTotalTime = 0;

            for (Map.Entry<String, Object> entry : topicsProgress.entrySet()) {
                Map<String, Object> tProg = (Map<String, Object>) entry.getValue();

                long attempts = tProg.get("quizzes_attempted") != null ? (Long) tProg.get("quizzes_attempted") : 0;
                long totalScore = tProg.get("total_score") != null ? (Long) tProg.get("total_score") : 0;
                long highestScore = tProg.get("highest_score") != null ? (Long) tProg.get("highest_score") : 0;
                long bestTime = tProg.get("best_time_spent") != null ? (Long) tProg.get("best_time_spent") : Long.MAX_VALUE;
                long totalTime = tProg.get("total_time_spent") != null ? (Long) tProg.get("total_time_spent") : 0;

                overallAttempts += attempts;
                overallTotalScore += totalScore;
                overallHighestScore = Math.max(overallHighestScore, highestScore);
                overallFastestTime = Math.min(overallFastestTime, bestTime);
                overallTotalTime += totalTime;
            }

            long overallAvgScore = overallAttempts > 0 ? overallTotalScore / overallAttempts : 0;
            long overallPercentage = overallAttempts > 0 ? (overallTotalScore * 100) / (overallAttempts * totalQuestions) : 0;
            long overallAvgTime = overallAttempts > 0 ? overallTotalTime / overallAttempts : 0;

            Map<String, Object> updatedOverall = new HashMap<>();
            updatedOverall.put("quizzes_attempted", overallAttempts);
            updatedOverall.put("total_score", overallTotalScore);
            updatedOverall.put("highest_score", overallHighestScore);
            updatedOverall.put("average_score", overallAvgScore);
            updatedOverall.put("percentage", overallPercentage);
            updatedOverall.put("fastest_time", overallFastestTime);
            updatedOverall.put("average_time", overallAvgTime);
            updatedOverall.put("last_attempted_topic", topicName);
            updatedOverall.put("last_attempted_timestamp", currentTimestamp);

            // --- Prepare update map for user doc ---
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("topics_progress", topicsProgress);
            updateMap.put("overall_progress", updatedOverall);

            // --- Update user document ---
            userDocRef.update(updateMap)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "User progress updated successfully");

                        // --- Also update daily progress in subcollection ---
                        String dayDocId = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                        DocumentReference dailyDocRef = userDocRef.collection("daily_progress").document(dayDocId);

                        dailyDocRef.get().addOnSuccessListener(dailyDocSnapshot -> {
                            Map<String, Object> dailyData = dailyDocSnapshot.exists() ?
                                    (Map<String, Object>) dailyDocSnapshot.getData() : new HashMap<>();

                            long dailyAttempts = dailyData.get("quizzes_attempted") != null ? (Long) dailyData.get("quizzes_attempted") : 0;
                            long dailyTotalScore = dailyData.get("total_score") != null ? (Long) dailyData.get("total_score") : 0;

                            long newDailyAttempts = dailyAttempts + 1;
                            long newDailyTotalScore = dailyTotalScore + correctAnswers;
                            long dailyPercentage = (newDailyAttempts > 0) ? (newDailyTotalScore * 100) / (newDailyAttempts * totalQuestions) : 0;

                            Map<String, Object> updatedDaily = new HashMap<>();
                            updatedDaily.put("quizzes_attempted", newDailyAttempts);
                            updatedDaily.put("total_score", newDailyTotalScore);
                            updatedDaily.put("percentage", dailyPercentage);
                            updatedDaily.put("last_updated", currentTimestamp);

                            dailyDocRef.set(updatedDaily)
                                    .addOnSuccessListener(aVoid2 -> Log.d("Firestore", "Daily progress updated"))
                                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to update daily progress", e));
                        });

                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to update user progress", e));
        });
    }


    private void updateGreeting() {
        String greeting;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning, ";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon, ";
        } else {
            greeting = "Good Evening, ";
        }

        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> profileDetails = (Map<String, Object>) documentSnapshot.get("Profile_details");
                            String name = "User";
                            String profileImageUrl = null;

                            if (profileDetails != null) {
                                if (profileDetails.get("name") != null) {
                                    name = profileDetails.get("name").toString();
                                }

                                if (profileDetails.get("profileImageUrl") != null) {
                                    profileImageUrl = profileDetails.get("profileImageUrl").toString();
                                }
                            }

                            tvGreeting.setText(greeting + name + "!");

                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(ResultActivity.this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.profile)
                                        .circleCrop()
                                        .into(ivUserAvatar);
                            }
                        }
                    });
        } else {
            tvGreeting.setText(greeting + "User!");
        }
    }

    private void showSummaryDialog(String title, ArrayList<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        StringBuilder message = new StringBuilder();
        for (String question : questions) {
            message.append(question).append("\n\n");
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateScoreUI() {
        tvCorrect.setText("Correct: " + correctAnswers);
        tvIncorrect.setText("Incorrect: " + incorrectAnswers);
        tvUnattempted.setText("Unattempted: " + unattempted);

        // Format time taken (convert milliseconds to minutes:seconds)
        long seconds = totalSessionTime / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        String timeString = String.format("⏱️ Time: %02d:%02d", minutes, seconds);
        tvTimeTaken.setText(timeString);

        tvScore.setText("Score: " + correctAnswers + "/" + totalQuestions + " (" + scorePercentage + "%)");

        // Dynamic message based on score
        String motivationalMessage;
        if (scorePercentage == 100) {
            motivationalMessage = "🏆 Excellent! You got everything correct!";
        } else if (scorePercentage >= 80) {
            motivationalMessage = "👏 Great job! Keep it up!";
        } else if (scorePercentage >= 50) {
            motivationalMessage = "👍 Good effort! Try to improve.";
        } else {
            motivationalMessage = "😕 Don't worry, keep practicing!";
        }

        tvMessage.setText(motivationalMessage);

        // Animate Circle Progress
        animateCircleProgress(scorePercentage);
    }

    private void animateCircleProgress(int targetProgress) {
        new Thread(() -> {
            for (int i = 0; i <= targetProgress; i++) {
                final int progress = i;
                runOnUiThread(() -> circleProgress.setProgress(progress));
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void shareScore() {
        String message = "I scored " + scorePercentage + "% in my quiz on PyLearn! 🎉\nCan you beat my score? Try it now!";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Share your score via"));
    }
}