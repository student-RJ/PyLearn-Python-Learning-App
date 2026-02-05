package com.example.python1;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.hdodenhof.circleimageview.CircleImageView;

public class LeaderboardActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private ArrayList<UserProgress> leaderboardList;
    private ArrayList<UserProgress> topUsers;
    private FirebaseFirestore db;

    private TextView user1Name, user2Name, user3Name;
    private CircleImageView user1Profile, user2Profile, user3Profile;
    private TextView user1Rank, user2Rank, user3Rank;
    private TextView user1Percentage, user2Percentage, user3Percentage;
    private ImageView user1Trophy;

    private TextView tvCurrentUserRank, tvCurrentUsername, tvCurrentUserPercentage;
    private CircleImageView imgCurrentUserProfile;
    private View currentUserCard;

    private ProgressBar loadingProgress;
    private TextView emptyStateTextView;
    private ChipGroup chipGroupLeaderboardFilters;

    private FirebaseUser currentUser;
    private SwipeRefreshLayout swipeRefreshLayout;

    private InterstitialAd mInterstitialAd;
    private static final String TAG = "LeaderboardActivity";

    public enum LeaderboardFilter {
        ALL_TIME, DAILY, WEEKLY, FRIENDS
    }

    private LeaderboardFilter currentFilter = LeaderboardFilter.ALL_TIME;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        recyclerView = findViewById(R.id.recycler_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        leaderboardList = new ArrayList<>();
        topUsers = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Bind top 3 views
        user1Name = findViewById(R.id.user1_name);
        user2Name = findViewById(R.id.user2_name);
        user3Name = findViewById(R.id.user3_name);
        user1Profile = findViewById(R.id.user1_profile);
        user2Profile = findViewById(R.id.user2_profile);
        user3Profile = findViewById(R.id.user3_profile);
        user1Rank = findViewById(R.id.user1_rank);
        user2Rank = findViewById(R.id.user2_rank);
        user3Rank = findViewById(R.id.user3_rank);
        user1Percentage = findViewById(R.id.user1_percentage);
        user2Percentage = findViewById(R.id.user2_percentage);
        user3Percentage = findViewById(R.id.user3_percentage);
        user1Trophy = findViewById(R.id.user1_trophy);

        // Bind current user card views
        currentUserCard = findViewById(R.id.current_user_card);
        tvCurrentUserRank = findViewById(R.id.tvCurrentUserRank);
        imgCurrentUserProfile = findViewById(R.id.imgCurrentUserProfile);
        tvCurrentUsername = findViewById(R.id.tvCurrentUsername);
        tvCurrentUserPercentage = findViewById(R.id.tvCurrentUserPercentage);

        // Bind loading and empty state views
        loadingProgress = findViewById(R.id.leaderboard_loading_progress);
        emptyStateTextView = findViewById(R.id.leaderboard_empty_state);

        // Bind chip group
        chipGroupLeaderboardFilters = findViewById(R.id.chipGroup_leaderboard_filters);
        setupChipGroupListener();

        // Set click listeners for top 3 profiles
        user1Profile.setOnClickListener(v -> onProfileClick(0));
        user2Profile.setOnClickListener(v -> onProfileClick(1));
        user3Profile.setOnClickListener(v -> onProfileClick(2));

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.refresh_tone);
            if (mp != null) {
                mp.start();
                mp.setOnCompletionListener(MediaPlayer::release);
            }
            loadLeaderboardData(currentFilter);
        });

        // Load leaderboard data initially
        loadLeaderboardData(currentFilter);

        // Initial loading of trophy GIF
        loadTrophyGifFromCloudinary();
        new Handler().postDelayed(() -> {
            user1Trophy.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> user1Trophy.setVisibility(View.GONE), 5000);
        }, 300);

        // Initialize the Interstitial Ad
        loadInterstitialAd();
    }

    private void setupChipGroupListener() {
        chipGroupLeaderboardFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all_time) {
                currentFilter = LeaderboardFilter.ALL_TIME;
            } else if (checkedId == R.id.chip_daily) {
                currentFilter = LeaderboardFilter.DAILY;
            } else if (checkedId == R.id.chip_weekly) {
                currentFilter = LeaderboardFilter.WEEKLY;
            } else if (checkedId == R.id.chip_friends) {
                currentFilter = LeaderboardFilter.FRIENDS;
                Toast.makeText(this, "Friends leaderboard selected (logic not yet implemented)", Toast.LENGTH_SHORT).show();
            }
            loadLeaderboardData(currentFilter);
        });
    }

    private void loadLeaderboardData(LeaderboardFilter filter) {
        Log.d(TAG, "loadLeaderboardData called for filter: " + filter.name());
        swipeRefreshLayout.setRefreshing(true);
        loadingProgress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateTextView.setVisibility(View.GONE);
        user1Trophy.setVisibility(View.GONE);

        switch (filter) {
            case DAILY:
                loadDailyLeaderboard();
                break;
            case WEEKLY:
                loadWeeklyLeaderboard();
                break;
            case ALL_TIME:
            default:
                loadAllTimeLeaderboard();
                break;
        }
    }

    private void loadAllTimeLeaderboard() {
        db.collection("users")
                .limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);
                    loadingProgress.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        ArrayList<UserProgress> fetchedUsers = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userId = document.getId();
                            Map<String, Object> profileDetails = (Map<String, Object>) document.get("Profile_details");
                            String name = null;
                            String profileUrl = null;
                            if (profileDetails != null) {
                                name = (String) profileDetails.get("name");
                                profileUrl = (String) profileDetails.get("profileImageUrl");
                            }

                            Double percentage = null;
                            Object progressData = document.get("overall_progress");
                            if (progressData instanceof Map) {
                                Map<String, Object> progressMap = (Map<String, Object>) progressData;
                                Object percentObj = progressMap.get("percentage");
                                if (percentObj instanceof Number) {
                                    percentage = ((Number) percentObj).doubleValue();
                                }
                            }

                            if (name != null && percentage != null) {
                                UserProgress userProgress = new UserProgress(userId, name, profileUrl, percentage);
                                fetchedUsers.add(userProgress);
                            }
                        }

                        processLeaderboardData(fetchedUsers);
                    } else {
                        handleLoadError(task.getException());
                    }
                });
    }

    private void loadDailyLeaderboard() {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<UserProgress> fetchedUsers = new ArrayList<>();
                        AtomicInteger pendingTasks = new AtomicInteger(task.getResult().size());

                        if (pendingTasks.get() == 0) {
                            swipeRefreshLayout.setRefreshing(false);
                            loadingProgress.setVisibility(View.GONE);
                            processLeaderboardData(fetchedUsers);
                            return;
                        }

                        for (QueryDocumentSnapshot userDoc : task.getResult()) {
                            String userId = userDoc.getId();
                            Map<String, Object> profileDetails = (Map<String, Object>) userDoc.get("Profile_details");
                            String name = null;
                            String profileUrl = null;
                            if (profileDetails != null) {
                                name = (String) profileDetails.get("name");
                                profileUrl = (String) profileDetails.get("profileImageUrl");
                            }

                            if (name != null) {
                                // Query daily progress for today
                                String finalName = name;
                                String finalProfileUrl = profileUrl;
                                db.collection("users").document(userId)
                                        .collection("daily_progress")
                                        .document(todayDate)
                                        .get()
                                        .addOnCompleteListener(dailyTask -> {
                                            if (dailyTask.isSuccessful() && dailyTask.getResult().exists()) {
                                                Object percentObj = dailyTask.getResult().get("percentage");
                                                if (percentObj instanceof Number) {
                                                    double percentage = ((Number) percentObj).doubleValue();
                                                    UserProgress userProgress = new UserProgress(userId, finalName, finalProfileUrl, percentage);
                                                    synchronized (fetchedUsers) {
                                                        fetchedUsers.add(userProgress);
                                                    }
                                                }
                                            }

                                            if (pendingTasks.decrementAndGet() == 0) {
                                                swipeRefreshLayout.setRefreshing(false);
                                                loadingProgress.setVisibility(View.GONE);
                                                processLeaderboardData(fetchedUsers);
                                            }
                                        });
                            } else {
                                if (pendingTasks.decrementAndGet() == 0) {
                                    swipeRefreshLayout.setRefreshing(false);
                                    loadingProgress.setVisibility(View.GONE);
                                    processLeaderboardData(fetchedUsers);
                                }
                            }
                        }
                    } else {
                        handleLoadError(task.getException());
                    }
                });
    }

    private void loadWeeklyLeaderboard() {
        // Get the dates for the current week (Sunday to Saturday)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Date weekStart = calendar.getTime();

        calendar.add(Calendar.DAY_OF_WEEK, 6);
        Date weekEnd = calendar.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        ArrayList<String> weekDates = new ArrayList<>();

        Calendar temp = Calendar.getInstance();
        temp.setTime(weekStart);
        while (!temp.getTime().after(weekEnd)) {
            weekDates.add(dateFormat.format(temp.getTime()));
            temp.add(Calendar.DAY_OF_MONTH, 1);
        }

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<UserProgress> fetchedUsers = new ArrayList<>();
                        AtomicInteger pendingTasks = new AtomicInteger(task.getResult().size());

                        if (pendingTasks.get() == 0) {
                            swipeRefreshLayout.setRefreshing(false);
                            loadingProgress.setVisibility(View.GONE);
                            processLeaderboardData(fetchedUsers);
                            return;
                        }

                        for (QueryDocumentSnapshot userDoc : task.getResult()) {
                            String userId = userDoc.getId();
                            Map<String, Object> profileDetails = (Map<String, Object>) userDoc.get("Profile_details");
                            String name = null;
                            String profileUrl = null;
                            if (profileDetails != null) {
                                name = (String) profileDetails.get("name");
                                profileUrl = (String) profileDetails.get("profileImageUrl");
                            }

                            if (name != null) {
                                String finalName = name;
                                String finalProfileUrl = profileUrl;
                                calculateWeeklyProgress(userId, weekDates, (weeklyPercentage) -> {
                                    if (weeklyPercentage > 0) {
                                        UserProgress userProgress = new UserProgress(userId, finalName, finalProfileUrl, weeklyPercentage);
                                        synchronized (fetchedUsers) {
                                            fetchedUsers.add(userProgress);
                                        }
                                    }

                                    if (pendingTasks.decrementAndGet() == 0) {
                                        swipeRefreshLayout.setRefreshing(false);
                                        loadingProgress.setVisibility(View.GONE);
                                        processLeaderboardData(fetchedUsers);
                                    }
                                });
                            } else {
                                if (pendingTasks.decrementAndGet() == 0) {
                                    swipeRefreshLayout.setRefreshing(false);
                                    loadingProgress.setVisibility(View.GONE);
                                    processLeaderboardData(fetchedUsers);
                                }
                            }
                        }
                    } else {
                        handleLoadError(task.getException());
                    }
                });
    }

    private void calculateWeeklyProgress(String userId, ArrayList<String> weekDates, WeeklyProgressCallback callback) {
        AtomicInteger pendingDays = new AtomicInteger(weekDates.size());
        Map<String, Double> dailyPercentages = new HashMap<>();

        for (String date : weekDates) {
            db.collection("users").document(userId)
                    .collection("daily_progress")
                    .document(date)
                    .get()
                    .addOnCompleteListener(dailyTask -> {
                        if (dailyTask.isSuccessful() && dailyTask.getResult().exists()) {
                            Object percentObj = dailyTask.getResult().get("percentage");
                            if (percentObj instanceof Number) {
                                double percentage = ((Number) percentObj).doubleValue();
                                synchronized (dailyPercentages) {
                                    dailyPercentages.put(date, percentage);
                                }
                            }
                        }

                        if (pendingDays.decrementAndGet() == 0) {
                            // Calculate average weekly percentage
                            double totalPercentage = 0;
                            int daysWithData = 0;
                            for (Double percentage : dailyPercentages.values()) {
                                totalPercentage += percentage;
                                daysWithData++;
                            }
                            double weeklyAverage = daysWithData > 0 ? totalPercentage / daysWithData : 0;
                            callback.onWeeklyProgressCalculated(weeklyAverage);
                        }
                    });
        }
    }

    private interface WeeklyProgressCallback {
        void onWeeklyProgressCalculated(double weeklyPercentage);
    }

    private void processLeaderboardData(ArrayList<UserProgress> fetchedUsers) {
        Log.d(TAG, "Total UserProgress objects created: " + fetchedUsers.size());

        // Sort list by percentage (descending)
        Collections.sort(fetchedUsers, (a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));

        topUsers.clear();
        leaderboardList.clear();

        if (fetchedUsers.size() > 0) {
            for (int i = 0; i < Math.min(3, fetchedUsers.size()); i++) {
                topUsers.add(fetchedUsers.get(i));
            }
            if (fetchedUsers.size() > 3) {
                leaderboardList.addAll(fetchedUsers.subList(3, fetchedUsers.size()));
            }
        }

        updateUI();

        if (fetchedUsers.isEmpty()) {
            emptyStateTextView.setText("No scores yet for this period. Be the first to join the ranks!");
            emptyStateTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            currentUserCard.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            currentUserCard.setVisibility(View.VISIBLE);
        }

        if (topUsers.size() > 0) {
            user1Trophy.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> user1Trophy.setVisibility(View.GONE), 5000);
        }
    }

    private void handleLoadError(Exception exception) {
        swipeRefreshLayout.setRefreshing(false);
        loadingProgress.setVisibility(View.GONE);
        Log.e(TAG, "Failed to load data from Firestore: " + exception.getMessage(), exception);
        Toast.makeText(this, "Failed to load data: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        emptyStateTextView.setText("Failed to load leaderboard. Please try again.");
        emptyStateTextView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        currentUserCard.setVisibility(View.GONE);
    }

    private void updateUI() {
        // Reset top 3 visibility
        findViewById(R.id.user1_section).setVisibility(View.GONE);
        findViewById(R.id.user2_section).setVisibility(View.GONE);
        findViewById(R.id.user3_section).setVisibility(View.GONE);

        if (topUsers.size() > 0) {
            UserProgress user1 = topUsers.get(0);
            user1Name.setText(user1.getUsername());
            user1Rank.setText("1st 👑");
            user1Percentage.setText(String.format("%.2f", user1.getPercentage()) + "%");
            loadImage(user1.getProfileImageUrl(), user1Profile);
            findViewById(R.id.user1_section).setVisibility(View.VISIBLE);
        }

        if (topUsers.size() > 1) {
            UserProgress user2 = topUsers.get(1);
            user2Name.setText(user2.getUsername());
            user2Rank.setText("2nd 🥈");
            user2Percentage.setText(String.format("%.2f", user2.getPercentage()) + "%");
            loadImage(user2.getProfileImageUrl(), user2Profile);
            findViewById(R.id.user2_section).setVisibility(View.VISIBLE);
        }

        if (topUsers.size() > 2) {
            UserProgress user3 = topUsers.get(2);
            user3Name.setText(user3.getUsername());
            user3Rank.setText("3rd 🥉");
            user3Percentage.setText(String.format("%.2f", user3.getPercentage()) + "%");
            loadImage(user3.getProfileImageUrl(), user3Profile);
            findViewById(R.id.user3_section).setVisibility(View.VISIBLE);
        }

        // Update the current user card
        UserProgress currentUserProgress = null;
        int currentUserRank = -1;
        if (currentUser != null) {
            for (int i = 0; i < topUsers.size(); i++) {
                if (topUsers.get(i).getUserId().equals(currentUser.getUid())) {
                    currentUserProgress = topUsers.get(i);
                    currentUserRank = i + 1;
                    break;
                }
            }
            if (currentUserProgress == null) {
                for (int i = 0; i < leaderboardList.size(); i++) {
                    if (leaderboardList.get(i).getUserId().equals(currentUser.getUid())) {
                        currentUserProgress = leaderboardList.get(i);
                        currentUserRank = i + 4;
                        break;
                    }
                }
            }
        }

        if (currentUserProgress != null) {
            tvCurrentUserRank.setText("You: " + (currentUserRank != -1 ? formatRank(currentUserRank) : "N/A"));
            tvCurrentUsername.setText(currentUserProgress.getUsername());
            tvCurrentUserPercentage.setText(String.format("%.2f", currentUserProgress.getPercentage()) + "%");
            loadImage(currentUserProgress.getProfileImageUrl(), imgCurrentUserProfile);
            currentUserCard.setVisibility(View.VISIBLE);
        } else {
            currentUserCard.setVisibility(View.GONE);
        }

        if (adapter == null) {
            adapter = new LeaderboardAdapter(leaderboardList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(leaderboardList);
        }
    }

    private String formatRank(int rank) {
        if (rank == 1) return "1st";
        if (rank == 2) return "2nd";
        if (rank == 3) return "3rd";
        int lastDigit = rank % 10;
        int secondLastDigit = (rank / 10) % 10;
        if (secondLastDigit == 1) {
            return rank + "th";
        }
        switch (lastDigit) {
            case 1: return rank + "st";
            case 2: return rank + "nd";
            case 3: return rank + "rd";
            default: return rank + "th";
        }
    }

    private void loadTrophyGifFromCloudinary() {
        String cloudinaryGifUrl = "https://res.cloudinary.com/dw02yjba1/image/upload/v1745568831/gif_jzx3nq.gif";
        Glide.with(this)
                .asGif()
                .load(cloudinaryGifUrl)
                .into(user1Trophy);
    }

    private void loadImage(String url, CircleImageView imageView) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.profile);
        }
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-6237833466905647/9481963684", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                mInterstitialAd = ad;
            }

            @Override
            public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError adError) {
                mInterstitialAd = null;
            }
        });
    }

    private void onProfileClick(int position) {
        if (position >= 0 && position < topUsers.size()) {
            String selectedUserId = topUsers.get(position).getUserId();
            String selectedUserRank = formatRank(position + 1);

            // Check if current user is subscribed
            if (currentUser != null) {
                db.collection("users").document(currentUser.getUid())
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Map<String, Object> paymentData = (Map<String, Object>) document.get("paymentData");
                                    boolean isSubscribed = false;

                                    if (paymentData != null) {
                                        String currentPlan = (String) paymentData.get("currentPlan");
                                        Boolean paymentStatus = (Boolean) paymentData.get("paymentStatus");
                                        isSubscribed = currentPlan != null && !currentPlan.isEmpty()
                                                && paymentStatus != null && paymentStatus;
                                    }

                                    if (!isSubscribed && mInterstitialAd != null) {
                                        // Show ad for non-subscribed users
                                        mInterstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                                            @Override
                                            public void onAdDismissedFullScreenContent() {
                                                navigateToUserProfile(selectedUserId, selectedUserRank);
                                                loadInterstitialAd();
                                            }

                                            @Override
                                            public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                                                super.onAdFailedToShowFullScreenContent(adError);
                                                navigateToUserProfile(selectedUserId, selectedUserRank);
                                                loadInterstitialAd();
                                            }
                                        });
                                        mInterstitialAd.show(this);
                                    } else {
                                        // Directly navigate for subscribed users or if no ad available
                                        navigateToUserProfile(selectedUserId, selectedUserRank);
                                    }
                                } else {
                                    // Document doesn't exist, treat as non-subscribed
                                    if (mInterstitialAd != null) {
                                        mInterstitialAd.show(this);
                                    } else {
                                        navigateToUserProfile(selectedUserId, selectedUserRank);
                                    }
                                }
                            } else {
                                // Error occurred, treat as non-subscribed
                                if (mInterstitialAd != null) {
                                    mInterstitialAd.show(this);
                                } else {
                                    navigateToUserProfile(selectedUserId, selectedUserRank);
                                }
                            }
                        });
            } else {
                // No user logged in, show ad if available
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(this);
                } else {
                    navigateToUserProfile(selectedUserId, selectedUserRank);
                }
            }
        }
    }

    // Helper method to navigate to user profile
    private void navigateToUserProfile(String userId, String userRank) {
        Intent intent = new Intent(LeaderboardActivity.this, User_leaderboard_profile.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USER_RANK", userRank);
        startActivity(intent);
    }
}