package com.example.python1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Sign_Up extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 123;
    private static final int RC_TWITTER_SIGN_IN = 456;
    private static final String TERMS_URL = "https://gpmumbai.ac.in/gpmweb/";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText fullNameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button signUpButton, googleSignInButton, twitterSignInButton;
    private TextView signInLink, termsLink, strengthText;
    private CheckBox termsCheckbox;
    private ProgressBar progressBar;
    private View strengthIndicator1, strengthIndicator2, strengthIndicator3;
    private GoogleSignInClient googleSignInClient;
    ImageView backArrow ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Views
        initializeViews();

        // Set up password strength checker
        setupPasswordStrengthChecker();

        // Set up click listeners
        setupClickListeners();
        findViewById(R.id.twitterSignInButton).setOnClickListener(v -> twitterSignIn());

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Sign_Up.this, Sign_in.class);
                startActivity(intent);
                finish(); // Optional: close the current activity
            }
        });

    }


    private void twitterSignIn() {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("twitter.com");

        Task<AuthResult> pendingResultTask = mAuth.getPendingAuthResult();
        if (pendingResultTask != null) {
            // There's a pending result from a previous sign-in attempt
            pendingResultTask.addOnSuccessListener(authResult -> navigateToMain())
                    .addOnFailureListener(e -> Toast.makeText(Sign_Up.this, "Twitter sign-up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Start a fresh sign-in flow
            mAuth.startActivityForSignInWithProvider(Sign_Up.this, provider.build())
                    .addOnSuccessListener(authResult -> navigateToMain())
                    .addOnFailureListener(e -> Toast.makeText(Sign_Up.this, "Twitter sign-up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
    private void navigateToMain() {
        Toast.makeText(Sign_Up.this, "Sign-Up successful", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(Sign_Up.this, MainActivity.class));
        finish();
    }

    private void initializeViews() {
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        signUpButton = findViewById(R.id.signUpButton);
        signInLink = findViewById(R.id.signInLink);
        termsLink = findViewById(R.id.termsLink);
        termsCheckbox = findViewById(R.id.termsCheckbox);
        progressBar = findViewById(R.id.progressBar);
        strengthText = findViewById(R.id.strengthText);
        strengthIndicator1 = findViewById(R.id.strengthIndicator1);
        strengthIndicator2 = findViewById(R.id.strengthIndicator2);
        strengthIndicator3 = findViewById(R.id.strengthIndicator3);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        twitterSignInButton = findViewById(R.id.twitterSignInButton);
    }

    private void setupPasswordStrengthChecker() {
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);

        switch (strength) {
            case 0:
                strengthText.setText("Weak");
                strengthIndicator1.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                strengthIndicator2.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                strengthIndicator3.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                break;
            case 1:
                strengthText.setText("Medium");
                strengthIndicator1.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                strengthIndicator2.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                strengthIndicator3.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                break;
            case 2:
                strengthText.setText("Strong");
                strengthIndicator1.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                strengthIndicator2.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                strengthIndicator3.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                break;
        }
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*\\d.*")) strength++; // contains digit
        if (password.matches(".*[!@#$%^&*].*")) strength++; // contains special char
        return Math.min(strength, 2); // cap at 2 (strong)
    }

    private void setupClickListeners() {
        signUpButton.setOnClickListener(v -> attemptSignUp());
        signInLink.setOnClickListener(v -> navigateToSignIn());
        termsLink.setOnClickListener(v -> openTermsAndConditions());

        // Social login buttons
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        twitterSignInButton.setOnClickListener(v -> signInWithTwitter());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToSignIn();
    }

    private void attemptSignUp() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!validateInputs(fullName, email, password, confirmPassword)) return;

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please accept terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Update user profile with display name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            // Save all user data to Firestore
                                            saveUserDataToFirestore(user.getUid(), fullName, email);

                                            // Send email verification
                                            sendEmailVerification(user);
                                        } else {
                                            showLoading(false);
                                            Toast.makeText(Sign_Up.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(Sign_Up.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String fullName, String email) {
        // Create profile_details map
        Map<String, Object> profileDetails = new HashMap<>();
        profileDetails.put("name", fullName);
        profileDetails.put("email", email);
        profileDetails.put("signup_method", "google");
        profileDetails.put("created_at", Timestamp.now());

        // Create terms acceptance map
        Map<String, Object> termsData = new HashMap<>();
        termsData.put("accepted", true);
        termsData.put("accepted_at", Timestamp.now());

        // Create main user data map to hold both profile_details and term_condition_status maps
        Map<String, Object> userData = new HashMap<>();
        userData.put("Profile_details", profileDetails);
        userData.put("term_condition_status", termsData);

        // Save entire data inside the userId document under "users" collection
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Sign_Up.this, "User data saved successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Sign_Up.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        boolean valid = true;

        if (fullName.isEmpty()) {
            fullNameInput.setError("Full name is required");
            valid = false;
        }

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            valid = false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            valid = false;
        } else if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters");
            valid = false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords don't match");
            valid = false;
        }

        return valid;
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(Sign_Up.this,
                                "Verification email sent to " + user.getEmail(),
                                Toast.LENGTH_LONG).show();
                        navigateToSignIn();
                    } else {
                        Toast.makeText(Sign_Up.this,
                                "Failed to send verification email",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        showLoading(true);
        // Always force account selection by signing out first
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    private void signInWithTwitter() {
        showLoading(true);
        // Twitter implementation would go here
        // For now, just show a message and hide loading
        showLoading(false);
        Toast.makeText(this, "Twitter sign-in will be implemented", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    // Ensure we have the email
                    String email = account.getEmail();
                    if (email != null && !email.isEmpty()) {
                        firebaseAuthWithGoogle(account.getIdToken(), email, account.getDisplayName());
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Could not get email from Google account", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (ApiException e) {
                showLoading(false);
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken, String email, String displayName) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if this is a new user
                            if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                // For new users, save all data to Firestore
                                String name = displayName != null ? displayName : "Google User";
                                saveUserDataToFirestore(user.getUid(), name, email);

                                // Update Firebase user profile
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(profileUpdates);
                            }
                            navigateToMainActivity();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openTermsAndConditions() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL));
        startActivity(browserIntent);
    }

    private void navigateToSignIn() {
        startActivity(new Intent(Sign_Up.this, Sign_in.class));
        finish();
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(Sign_Up.this, MainActivity.class));
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        signUpButton.setEnabled(!show);
        googleSignInButton.setEnabled(!show);
        twitterSignInButton.setEnabled(!show);
    }
}