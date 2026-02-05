package com.example.python1;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TutorialActivity extends BaseActivity {

    // Video Player Components
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private ImageView btnFullScreen;
    private ProgressBar videoLoadingSpinner;

    // UI Components
    private Spinner spinnerTopics;
    private TextView tvTitle, tvContent, tvCodeExample;
    private TextView tvOverallProgressPercentage;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private Button btnPreviousLesson, btnNextLesson, btnTakeQuiz;
    private MaterialCardView videoCardContainer;

    // State Management
    private boolean isFullScreen = false;
    private FirebaseFirestore firestore;
    private int currentTopicIndex = 0;

    // Data
    private final HashMap<String, String> videoUrls = new HashMap<String, String>() {{
        put("Python Basics", "https://res.cloudinary.com/dw02yjba1/video/upload/v1744376459/Introduction_to_Programming_Python_Python_Tutorial_-_Day_1_-_1080_eyxxsq.mp4");
        put("Loops", "https://res.cloudinary.com/dw02yjba1/video/upload/v1744379460/For_Loops_in_Python_Python_Tutorial_-_Day_17_-_1080_slnorf.mp4");
        put("Data Types", "https://res.cloudinary.com/dw02yjba1/video/upload/v1744377367/Variables_and_Data_Types_Python_Tutorial_-_Day_6_-_1080_h2wxqv.mp4");
        put("Functions", "https://res.cloudinary.com/dw02yjba1/video/upload/v1744386604/Functions_in_Python_Python_Tutorial_-_Day_20_-_1080_yjn2ed.mp4");
        put("OOPs", "https://res.cloudinary.com/dw02yjba1/video/upload/v1744386510/Introduction_to_OOPs_in_Python_Python_Tutorial_-_Day_56_-_1080_s3rga7.mp4");
    }};

    private final List<String> topicList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        initializeViews();
        setupPlayer();
        setupSpinner();
        setupButtons();
        fetchTopicsFromFirestore();
        loadUserProgress();
    }

    private void initializeViews() {
        // Video Player
        playerView = findViewById(R.id.playerView);
        btnFullScreen = findViewById(R.id.btnFullScreen);
        videoLoadingSpinner = findViewById(R.id.videoLoadingSpinner);
        videoCardContainer = findViewById(R.id.videoCardContainer);

        // Navigation
        findViewById(R.id.backButton).setOnClickListener(v -> onBackPressed());

        // Topic Selection
        spinnerTopics = findViewById(R.id.spinnerTopics);

        // Content Display
        tvTitle = findViewById(R.id.tvTitle);
        tvContent = findViewById(R.id.tvContent);
        tvCodeExample = findViewById(R.id.tvCodeExample);
        scrollView = findViewById(R.id.scrollView);

        // Progress Tracking
        progressBar = findViewById(R.id.progressBar);
        tvOverallProgressPercentage = findViewById(R.id.tvOverallProgressPercentage);

        // Navigation Buttons
        btnPreviousLesson = findViewById(R.id.btnPreviousLesson);
        btnNextLesson = findViewById(R.id.btnNextLesson);
        btnTakeQuiz = findViewById(R.id.btnTakeQuiz);

        firestore = FirebaseFirestore.getInstance();
    }

    private void setupPlayer() {
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);
        playerView.setUseController(true);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setControllerShowTimeoutMs(3000);

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    videoLoadingSpinner.setVisibility(View.VISIBLE);
                } else {
                    videoLoadingSpinner.setVisibility(View.GONE);
                }

                if (state == Player.STATE_ENDED) {
                    String selectedTopic = spinnerTopics.getSelectedItem().toString();
                    markTutorialCompleted(selectedTopic);
                }
            }
        });
    }

    private void setupSpinner() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, topicList);
        spinnerTopics.setAdapter(adapter);

        spinnerTopics.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentTopicIndex = position;
                updateNavigationButtons();
                loadLessonData(topicList.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupButtons() {
        btnFullScreen.setOnClickListener(v -> toggleFullScreen());

        btnPreviousLesson.setOnClickListener(v -> {
            if (currentTopicIndex > 0) {
                spinnerTopics.setSelection(currentTopicIndex - 1);
            }
        });

        btnNextLesson.setOnClickListener(v -> {
            if (currentTopicIndex < topicList.size() - 1) {
                spinnerTopics.setSelection(currentTopicIndex + 1);
            }
        });

        btnTakeQuiz.setOnClickListener(v -> {
            String selectedTopic = spinnerTopics.getSelectedItem().toString();
            Intent intent = new Intent(TutorialActivity.this, QuizActivity.class);
            intent.putExtra("selectedTopic", selectedTopic);
            startActivity(intent);
        });
    }

    private void updateNavigationButtons() {
        btnPreviousLesson.setEnabled(currentTopicIndex > 0);
        btnNextLesson.setEnabled(currentTopicIndex < topicList.size() - 1);
    }

    private void fetchTopicsFromFirestore() {
        firestore.collection("tutorials").get()
                .addOnSuccessListener(querySnapshot -> {
                    topicList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        topicList.add(capitalizeWords(doc.getId().replace("_", " ")));
                    }
                    adapter.notifyDataSetChanged();

                    if (!topicList.isEmpty()) {
                        spinnerTopics.setSelection(0);
                        loadLessonData(topicList.get(0));
                    }
                })
                .addOnFailureListener(e -> showError("Failed to load topics: " + e.getMessage()));
    }

    private void loadLessonData(String topic) {
        firestore.collection("tutorials").document(topic).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        tvTitle.setText(snapshot.getString("title"));
                        tvContent.setText(snapshot.getString("content"));
                        tvCodeExample.setText(snapshot.getString("code_example"));

                        // Load Video
                        if (videoUrls.containsKey(topic)) {
                            MediaItem mediaItem = MediaItem.fromUri(videoUrls.get(topic));
                            exoPlayer.setMediaItem(mediaItem);
                            exoPlayer.prepare();
                            exoPlayer.play();
                        } else {
                            showError("No video found for this topic.");
                        }
                    } else {
                        showError("Document not found for: " + topic);
                    }
                })
                .addOnFailureListener(e -> showError("Error fetching lesson: " + e.getMessage()));
    }

    private void loadUserProgress() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> progress = documentSnapshot.getData();
                        if (progress != null && progress.containsKey("overall_progress")) {
                            Map<String, Object> overallProgress = (Map<String, Object>) progress.get("overall_progress");
                            if (overallProgress != null && overallProgress.containsKey("percentage")) {
                                int percentage = ((Long) overallProgress.get("percentage")).intValue();
                                progressBar.setProgress(percentage); // Reads and displays
                                tvOverallProgressPercentage.setText(percentage + "% Completed"); // Reads and displays
                            }
                        }
                    }
                });
    }

    private void markTutorialCompleted(String topicDisplayName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Get current time for timestamp
        long currentTimeMillis = System.currentTimeMillis();

        // Reference to the user's topic progress document
        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Get the existing topics_progress map
                    Map<String, Object> topicsProgress = (Map<String, Object>) documentSnapshot.get("topics_progress");
                    if (topicsProgress == null) {
                        topicsProgress = new HashMap<>();
                    }

                    // Get the specific topic's data, or create if it doesn't exist
                    Map<String, Object> topicData = (Map<String, Object>) topicsProgress.get(topicDisplayName);
                    if (topicData == null) {
                        topicData = new HashMap<>();
                    }

                    // Update the tutorial completion status
                    topicData.put("tutorial_completed", true);

                    // Update completion count
                    long currentCount = 0;
                    if (topicData.containsKey("tutorial_completed_count")) {
                        // Firebase stores numbers as Long by default
                        currentCount = (Long) topicData.get("tutorial_completed_count");
                    }
                    topicData.put("tutorial_completed_count", currentCount + 1);

                    // Update last completed timestamp
                    topicData.put("last_completed_timestamp", currentTimeMillis);

                    // Put the updated topic data back into the main topics_progress map
                    topicsProgress.put(topicDisplayName, topicData);

                    // Update the Firestore document with the modified topics_progress map
                    firestore.collection("users")
                            .document(user.getUid())
                            .update("topics_progress", topicsProgress) // Update the whole map
                            .addOnSuccessListener(unused -> {
                                showToast("Tutorial for '" + topicDisplayName + "' completed!");
                                loadUserProgress(); // Refresh progress display and recalculate overall progress
                            })
                            .addOnFailureListener(e -> showError("Failed to update tutorial completion: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showError("Failed to get user data for tutorial completion: " + e.getMessage()));
    }
    private void toggleFullScreen() {
        isFullScreen = !isFullScreen;

        if (isFullScreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );

            // Hide all UI elements except video
            videoCardContainer.setLayoutParams(new MaterialCardView.LayoutParams(
                    MaterialCardView.LayoutParams.MATCH_PARENT,
                    MaterialCardView.LayoutParams.MATCH_PARENT
            ));
            videoCardContainer.setRadius(0); // Remove rounded corners
            findViewById(R.id.rootLayout).setVisibility(View.GONE);

        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            // Restore original layout
            videoCardContainer.setLayoutParams(new MaterialCardView.LayoutParams(
                    MaterialCardView.LayoutParams.MATCH_PARENT,
                    getResources().getDimensionPixelSize(R.dimen.video_player_height)
            ));
            videoCardContainer.setRadius(getResources().getDimension(R.dimen.card_corner_radius));
            findViewById(R.id.rootLayout).setVisibility(View.VISIBLE);
        }
    }

    private String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void showError(String message) {
        tvTitle.setText("Error");
        tvContent.setText(message);
        tvCodeExample.setText("");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            toggleFullScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isFullScreen) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }
    }
}