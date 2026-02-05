package com.example.python1;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CertificatesActivity extends BaseActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private CertificateAdapter adapter;
    private List<CertificateModel> certificateList = new ArrayList<>();
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificates);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recycler_certificates);
        emptyView = findViewById(R.id.tv_empty_certificates);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CertificateAdapter(this, certificateList);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.refresh_tone);
            mp.start();
            mp.setOnCompletionListener(MediaPlayer::release);
            loadCertificates();
        });

        loadCertificates();
        Toolbar toolbar = findViewById(R.id.toolbar_certificate);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish()); // back

    }

    private void loadCertificates() {
        swipeRefreshLayout.setRefreshing(true);
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    certificateList.clear();
                    if (documentSnapshot.exists()) {
                        // 1. Get payment status first
                        Map<String, Object> paymentData = (Map<String, Object>) documentSnapshot.get("paymentData");
                        boolean isPaidUser = false;
                        if (paymentData != null && paymentData.containsKey("paymentStatus")) {
                            Boolean status = (Boolean) paymentData.get("paymentStatus");
                            isPaidUser = status != null && status;
                        }

                        // 2. Get progress
                        Map<String, Object> topicsProgress = (Map<String, Object>) documentSnapshot.get("topics_progress");

                        if (topicsProgress != null && !topicsProgress.isEmpty()) {
                            emptyView.setVisibility(TextView.GONE);

                            for (String topic : topicsProgress.keySet()) {
                                Map<String, Object> topicData = (Map<String, Object>) topicsProgress.get(topic);

                                Boolean quizCompleted = (Boolean) topicData.get("quiz_completed");
                                Number highestScoreNumber = (Number) topicData.get("highest_score");

                                if (quizCompleted != null && quizCompleted && highestScoreNumber != null) {
                                    int highestScore = highestScoreNumber.intValue();

                                    if ((isPaidUser && highestScore >= 1) || (!isPaidUser && highestScore >= 70)) {
                                        String date = java.text.DateFormat.getDateInstance().format(new java.util.Date());

                                        certificateList.add(new CertificateModel(topic, date, highestScore, ""));
                                    }
                                }
                            }
                        }

                        if (certificateList.isEmpty()) {
                            emptyView.setVisibility(TextView.VISIBLE);
                        }

                    } else {
                        emptyView.setVisibility(TextView.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    emptyView.setVisibility(TextView.VISIBLE);
                    Log.e("Certificates", "Failed to load certificates", e);
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

}
