package com.example.python1;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PremiumFeaturesActivity extends AppCompatActivity implements PaymentResultListener {

    private RecyclerView rvPremiumFeatures;
    private Button btnSubscribe;
    private CardView cardMonthlyPlan;
    private CardView cardYearlyPlan;
    private TextView tvCurrentPlanStatus;

    private TextView tvMonthlyTitle, tvMonthlyPrice, tvMonthlyPeriod;
    private TextView tvYearlyTitle, tvYearlyPrice, tvYearlyPeriod;

    private int selectedPlan = R.id.card_yearly_plan; // Default selection

    private static final String RAZORPAY_API_KEY = "rzp_live_J4Uu0ICMpDr3Sy";

    private static final int MONTHLY_PRICE = 5000; // ₹50.00 in paise
    private static final int YEARLY_PRICE = 50000; // ₹500.00 in paise

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private String userName = "";
    private String userEmail = "";
    private String userMobile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_features);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Gradient background
        View rootView = findViewById(R.id.toolbar).getParent() instanceof View ?
                (View) findViewById(R.id.toolbar).getParent() : findViewById(android.R.id.content);
        if (rootView != null) {
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{ContextCompat.getColor(this, R.color.premium_background_start),
                            ContextCompat.getColor(this, R.color.premium_background_end)});
            rootView.setBackground(gradientDrawable);
        }

        rvPremiumFeatures = findViewById(R.id.rv_premium_features);
        btnSubscribe = findViewById(R.id.btn_subscribe);
        cardMonthlyPlan = findViewById(R.id.card_monthly_plan);
        cardYearlyPlan = findViewById(R.id.card_yearly_plan);
        tvCurrentPlanStatus = findViewById(R.id.tv_current_plan_status);
        TextView tvTermsPrivacy = findViewById(R.id.tv_terms_privacy);
        TextView tvFeaturesHeading = findViewById(R.id.tv_features_heading);

        tvFeaturesHeading.setText("PyLearn Premium Features:");

        tvMonthlyTitle = cardMonthlyPlan.findViewById(R.id.tv_monthly_title);
        tvMonthlyPrice = cardMonthlyPlan.findViewById(R.id.tv_monthly_price);
        tvMonthlyPeriod = cardMonthlyPlan.findViewById(R.id.tv_monthly_period);

        tvYearlyTitle = cardYearlyPlan.findViewById(R.id.tv_yearly_title);
        tvYearlyPrice = cardYearlyPlan.findViewById(R.id.tv_yearly_price);
        tvYearlyPeriod = cardYearlyPlan.findViewById(R.id.tv_yearly_period);

        rvPremiumFeatures.setLayoutManager(new LinearLayoutManager(this));
        PremiumFeatureAdapter adapter = new PremiumFeatureAdapter(getPremiumFeaturesList());
        rvPremiumFeatures.setAdapter(adapter);

        updatePlanSelection(selectedPlan);

        cardMonthlyPlan.setOnClickListener(v -> updatePlanSelection(R.id.card_monthly_plan));
        cardYearlyPlan.setOnClickListener(v -> updatePlanSelection(R.id.card_yearly_plan));

        btnSubscribe.setOnClickListener(v -> {
            String planType = (selectedPlan == R.id.card_monthly_plan) ? "Monthly" : "Yearly";
            Toast.makeText(PremiumFeaturesActivity.this, "Initiating " + planType + " Subscription...", Toast.LENGTH_SHORT).show();
            startPayment();
        });

        tvTermsPrivacy.setOnClickListener(v -> {
            String termsUrl = "https://www.yourwebsite.com/pykleran/terms";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl));
            startActivity(browserIntent);
            Toast.makeText(PremiumFeaturesActivity.this, "Opening Terms of Service...", Toast.LENGTH_SHORT).show();
        });

        // Initialize Razorpay Checkout
        Checkout.preload(getApplicationContext());

        // Fetch user data from Firestore for prefill
        fetchUserData();
        // Fetch and display current plan status
        fetchCurrentPlanStatus();
    }

    private void fetchUserData() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Assuming profile details are stored in a map named "Profile_details"
                        if (documentSnapshot.contains("Profile_details")) {
                            Object profileDetailsObj = documentSnapshot.get("Profile_details");
                            if (profileDetailsObj instanceof Map) { // Use Map for type checking
                                Map<String, Object> profileDetails = (Map<String, Object>) profileDetailsObj;
                                userName = (String) profileDetails.getOrDefault("name", "");
                                userEmail = (String) profileDetails.getOrDefault("email", "");
                                userMobile = (String) profileDetails.getOrDefault("mobile", "");
                            }
                        } else {
                            // Fallback: try root level fields
                            userName = documentSnapshot.getString("name") != null ? documentSnapshot.getString("name") : "";
                            userEmail = documentSnapshot.getString("email") != null ? documentSnapshot.getString("email") : "";
                            userMobile = documentSnapshot.getString("mobile") != null ? documentSnapshot.getString("mobile") : "";
                        }

                        Log.d("UserData", "Name: " + userName + ", Email: " + userEmail + ", Mobile: " + userMobile);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreError", e.getMessage(), e);
                });
    }

    private void fetchCurrentPlanStatus() {
        if (currentUser == null) {
            tvCurrentPlanStatus.setText("Not logged in");
            return;
        }

        String userId = currentUser.getUid();
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("paymentData")) {
                        Map<String, Object> paymentData = (Map<String, Object>) documentSnapshot.get("paymentData");
                        if (paymentData != null) {
                            Boolean paymentStatus = (Boolean) paymentData.get("paymentStatus");
                            String currentPlan = (String) paymentData.get("currentPlan");
                            if (Boolean.TRUE.equals(paymentStatus) && currentPlan != null) {
                                tvCurrentPlanStatus.setText("You are subscribed (" + currentPlan + ")");
                            } else {
                                tvCurrentPlanStatus.setText("You are not subscribed");
                            }
                        } else {
                            tvCurrentPlanStatus.setText("You are not subscribed");
                        }
                    } else {
                        tvCurrentPlanStatus.setText("You are not subscribed");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreStatus", "Error fetching current plan status: " + e.getMessage());
                    tvCurrentPlanStatus.setText("Failed to load status");
                });
    }

    private void updatePlanSelection(int newSelectedPlanId) {
        selectedPlan = newSelectedPlanId;

        // Reset all to unselected state
        cardMonthlyPlan.setCardBackgroundColor(ContextCompat.getColor(this, R.color.plan_card_background_unselected));
        tvMonthlyTitle.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tvMonthlyPrice.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tvMonthlyPeriod.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        cardYearlyPlan.setCardBackgroundColor(ContextCompat.getColor(this, R.color.plan_card_background_unselected));
        tvYearlyTitle.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tvYearlyPrice.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tvYearlyPeriod.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        // Apply selected state
        if (selectedPlan == R.id.card_monthly_plan) {
            cardMonthlyPlan.setCardBackgroundColor(ContextCompat.getColor(this, R.color.plan_card_background_selected));
            tvMonthlyTitle.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvMonthlyPrice.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvMonthlyPeriod.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            Log.d("PlanSelection", "Monthly plan selected.");
        } else if (selectedPlan == R.id.card_yearly_plan) {
            cardYearlyPlan.setCardBackgroundColor(ContextCompat.getColor(this, R.color.plan_card_background_selected_green));
            tvYearlyTitle.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvYearlyPrice.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvYearlyPeriod.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            Log.d("PlanSelection", "Yearly plan selected.");
        }
    }

    private List<String> getPremiumFeaturesList() {
        List<String> features = new ArrayList<>();
        features.add("✅ Unlock All Quiz Topics");
        features.add("✅ Ad-Free Learning Experience");
        features.add("✅ Personal Customer Support");
        features.add("✅ Early Certificate Access");
        features.add("✅ Access Premium Tutorials");
        features.add("✅ Advanced Progress Tracking");
        features.add("✅ Premium Badge on Profile");
        features.add("✅ Daily Goals & Smart Reminders");
        features.add("✅ Interactive Video Lessons");
        features.add("✅ Practice Problems & Challenges");
        features.add("✅ Exclusive Live Sessions (Coming Soon)");
        features.add("✅ Early Access to New Features");
        features.add("✅ Download & Share Certificates Anytime");
        return features;
    }

    private void startPayment() {
        Checkout checkout = new Checkout();
        checkout.setKeyID(RAZORPAY_API_KEY);
        checkout.setImage(R.drawable.ic_launcher_foreground);

        final PremiumFeaturesActivity activity = this;

        try {
            JSONObject options = new JSONObject();

            String planName = (selectedPlan == R.id.card_monthly_plan) ? "Monthly Plan" : "Yearly Plan";
            int amount = (selectedPlan == R.id.card_monthly_plan) ? MONTHLY_PRICE : YEARLY_PRICE;

            options.put("name", "PyLearn Premium");
            options.put("description", planName);
            options.put("currency", "INR");
            options.put("amount", amount); // Amount in paise

            JSONObject preFill = new JSONObject();

            // Prefill with fetched user info if available
            preFill.put("email", (userEmail.isEmpty()) ? "user@example.com" : userEmail);
            preFill.put("contact", (userMobile.isEmpty()) ? "9999999999" : userMobile);

            options.put("prefill", preFill);

            checkout.open(activity, options);
        } catch (Exception e) {
            Toast.makeText(this, "Error in starting payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ... (your existing code)

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Toast.makeText(this, "Payment Successful: " + razorpayPaymentID, Toast.LENGTH_LONG).show();

        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in, cannot save payment data.", Toast.LENGTH_SHORT).show();
            Log.e("FirestoreSave", "User is null in onPaymentSuccess");
            return;
        }

        String userId = currentUser.getUid();
        String currentPlan = (selectedPlan == R.id.card_monthly_plan) ? "Monthly" : "Yearly";

        // Determine the amount in paise from the selected plan
        int amountInPaise = (selectedPlan == R.id.card_monthly_plan) ? MONTHLY_PRICE : YEARLY_PRICE;
        // Convert amount to Rupees for storage
        double amountInRupees = (double) amountInPaise / 100.0; // Divide by 100 to get Rupees

        Map<String, Object> paymentDataMap = new HashMap<>();
        paymentDataMap.put("paymentStatus", true);
        paymentDataMap.put("currentPlan", currentPlan);
        paymentDataMap.put("transactionId", razorpayPaymentID);
        paymentDataMap.put("timestamp", Timestamp.now());
        paymentDataMap.put("amountPaid", amountInRupees); // Store amount in Rupees here!

        firestore.collection("users")
                .document(userId)
                .update("paymentData", paymentDataMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PremiumFeaturesActivity.this, "Subscription details saved!", Toast.LENGTH_SHORT).show();
                    Log.d("FirestoreSave", "Payment details map saved successfully for user: " + userId);
                    tvCurrentPlanStatus.setText("You are subscribed (" + currentPlan + ")");

                    // --- Navigate to Main Activity ---
                    // Assuming your main activity is named MainActivity.class
                    Intent intent = new Intent(PremiumFeaturesActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clears activity stack
                    startActivity(intent);
                    finish(); // Finish the PremiumFeaturesActivity so the user cannot go back to it
                    // --- End Navigation ---
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PremiumFeaturesActivity.this, "Failed to save subscription details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FirestoreSave", "Error saving payment details map: " + e.getMessage(), e);
                });
    }

// ... (rest of your code)

    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, "Payment failed: " + response, Toast.LENGTH_LONG).show();
        Log.e("RazorpayPayment", "Error code: " + code + ", Response: " + response);
    }

    // RecyclerView Adapter class
    private static class PremiumFeatureAdapter extends RecyclerView.Adapter<PremiumFeatureAdapter.FeatureViewHolder> {

        private final List<String> features;

        public PremiumFeatureAdapter(List<String> features) {
            this.features = features;
        }

        @NonNull
        @Override
        public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_premium_feature, parent, false);
            return new FeatureViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
            String feature = features.get(position);
            holder.tvFeatureDescription.setText(feature);
        }

        @Override
        public int getItemCount() {
            return features.size();
        }

        static class FeatureViewHolder extends RecyclerView.ViewHolder {
            TextView tvFeatureDescription;
            ImageView ivFeatureIcon;

            public FeatureViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFeatureDescription = itemView.findViewById(R.id.tv_feature_description);
                ivFeatureIcon = itemView.findViewById(R.id.iv_feature_icon);
            }
        }
    }
}