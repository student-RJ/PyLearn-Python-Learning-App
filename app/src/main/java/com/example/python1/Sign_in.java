package com.example.python1;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;

import java.util.concurrent.Executor;


public class Sign_in extends BaseActivity {
    private SharedPreferences preferences;
    private static final String PREF_NAME = "user_settings";

    private TextInputEditText emailInput, passwordInput;
    private MaterialCardView swipeThumb;
    private TextView forgotPassword, signUpButton;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private BiometricPrompt biometricPrompt; // Unused, but kept as per your original code
    private BiometricPrompt.PromptInfo promptInfo; // Unused, but kept as per your original code
    private Executor executor; // Unused, but kept as per your original code
    private float initialX;
    private int maxSwipeDistance;
    private VelocityTracker velocityTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        swipeThumb = findViewById(R.id.swipeThumb);
        forgotPassword = findViewById(R.id.forgotPassword);
        signUpButton = findViewById(R.id.signUpButton);
        LottieAnimationView animationView = findViewById(R.id.animationView); // Initialize LottieAnimationView

        animationView.setAnimation(R.raw.login_animation1);
        animationView.playAnimation();
        animationView.loop(true);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.googleSignInButton).setOnClickListener(v -> googleSignIn());

        // Swipe to Sign In Logic
        swipeThumb.post(() -> {
            View parent = (View) swipeThumb.getParent();
            maxSwipeDistance = parent.getWidth() - swipeThumb.getWidth() - dpToPx(8);
        });

        swipeThumb.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getRawX() - v.getX();
                    velocityTracker = VelocityTracker.obtain();
                    velocityTracker.addMovement(event);
                    break;

                case MotionEvent.ACTION_MOVE:
                    velocityTracker.addMovement(event);
                    float newX = event.getRawX() - initialX;
                    newX = Math.max(0, Math.min(newX, maxSwipeDistance));
                    v.setX(newX);
                    break;

                case MotionEvent.ACTION_UP:
                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000);
                    float velocityX = velocityTracker.getXVelocity();
                    velocityTracker.recycle();

                    if (v.getX() >= maxSwipeDistance * 0.8 || velocityX > 1000) {
                        performHaptic();
                        animateToFull(v);
                    } else {
                        animateToStart(v);
                    }
                    break;
            }
            return true;
        });

        forgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(Sign_in.this, Sign_Up.class);
            startActivity(intent);
        });

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean biometricEnabled = preferences.getBoolean("biometric", false);
        if (biometricEnabled) {
            promptBiometricAuthentication();
        }

        findViewById(R.id.twitterSignInButton).setOnClickListener(v -> twitterSignIn());

        findViewById(R.id.githubSignInButton).setOnClickListener(v -> githubSignIn());
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void performHaptic() {
        swipeThumb.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }
    private void animateToFull(View v) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, "x", maxSwipeDistance);
        animator.setDuration(250);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        if (validateInputs(email, password)) {
            signInUser(email, password);
        } else {
            animateToStart(v); // fallback if invalidokkk
        }
    }
    private void animateToStart(View v) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, "x", 0);
        animator.setDuration(200);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }
    private void animateSwipeBack(View v) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, "x", 0);
        animator.setDuration(200); // Duration for returning
        animator.setInterpolator(new DecelerateInterpolator()); // Smooth return
        animator.start();
    }
    private void twitterSignIn() {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("twitter.com");

        Task<AuthResult> pendingResultTask = mAuth.getPendingAuthResult();
        if (pendingResultTask != null) {
            // There's a pending result from a previous sign-in attempt
            pendingResultTask.addOnSuccessListener(authResult -> navigateToMain())
                    .addOnFailureListener(e -> Toast.makeText(Sign_in.this, "Twitter sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Start a fresh sign-in flow
            mAuth.startActivityForSignInWithProvider(Sign_in.this, provider.build())
                    .addOnSuccessListener(authResult -> navigateToMain())
                    .addOnFailureListener(e -> Toast.makeText(Sign_in.this, "Twitter sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
    private void githubSignIn() {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("github.com");
        Task<AuthResult> pendingResultTask = mAuth.getPendingAuthResult();
        if (pendingResultTask != null) {
            // There's a pending result from a previous sign-in attempt
            pendingResultTask.addOnSuccessListener(authResult -> navigateToMain())
                    .addOnFailureListener(e -> Toast.makeText(Sign_in.this, "GitHub sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Start a fresh sign-in flow
            mAuth.startActivityForSignInWithProvider(Sign_in.this, provider.build())
                    .addOnSuccessListener(authResult -> navigateToMain())
                    .addOnFailureListener(e -> Toast.makeText(Sign_in.this, "GitHub sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
    private void navigateToMain() {
        Toast.makeText(Sign_in.this, "Sign-in successful", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(Sign_in.this, MainActivity.class));
        finish();
    }
    private void promptBiometricAuthentication() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                == BiometricManager.BIOMETRIC_SUCCESS) {

            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            runOnUiThread(() -> {
                                Toast.makeText(Sign_in.this, "Authentication successful", Toast.LENGTH_SHORT).show();
                                // Proceed with sign-in or auto-login
                                onBiometricSuccess();
                            });
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            runOnUiThread(() -> Toast.makeText(Sign_in.this, "Authentication failed", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            runOnUiThread(() -> Toast.makeText(Sign_in.this, "Error: " + errString, Toast.LENGTH_SHORT).show());
                        }
                    });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Login")
                    .setSubtitle("Authenticate to sign in")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show();
        }
    }
    private void onBiometricSuccess() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return false;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return false;
        }
        return true;
    }
    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null && user.isEmailVerified()) {
                            // Email is verified
                            Toast.makeText(Sign_in.this, "Sign-in successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Sign_in.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Email not verified
                            Toast.makeText(Sign_in.this, "Please verify your email. We’ve already sent you a verification link. Check your inbox.", Toast.LENGTH_LONG).show();
                            mAuth.signOut(); // Sign out the unverified user
                            animateSwipeBack(swipeThumb); // Reset swipe thumb if used
                        }

                    } else {
                        Toast.makeText(Sign_in.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        animateSwipeBack(swipeThumb); // Reset swipe thumb if used
                    }
                });
    }

    private void showForgotPasswordDialog() {
        Dialog forgotPasswordDialog = new Dialog(this);
        forgotPasswordDialog.setContentView(R.layout.dialog_forget);

        EditText emailBox = forgotPasswordDialog.findViewById(R.id.emailBox);
        Button btnCancel = forgotPasswordDialog.findViewById(R.id.btnCancel);
        Button btnReset = forgotPasswordDialog.findViewById(R.id.btnReset);

        btnCancel.setOnClickListener(v -> forgotPasswordDialog.dismiss());

        btnReset.setOnClickListener(v -> {
            String email = emailBox.getText().toString().trim();
            if (!email.isEmpty()) {
                resetPassword(email, forgotPasswordDialog);
            } else {
                emailBox.setError("Please enter your email address");
            }
        });

        forgotPasswordDialog.show();
    }
    private void resetPassword(String email, Dialog dialog) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Sign_in.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(Sign_in.this, "Error sending password reset email", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void googleSignIn() {
        // It's good practice to revoke access before starting new sign-in flow for consistent behavior.
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, task -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(Sign_in.this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(Sign_in.this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Sign_in.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(Sign_in.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}