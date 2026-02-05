package com.example.python1;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class Setting_Activity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final String CHANNEL_ID = "notifyPyLearn";
    private static final int NOTIFICATION_CODE = 1;
    private static final int DAILY_GOAL_NOTIFICATION_CODE = 2;
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 100;

    private Switch notificationSwitch, dailyGoalSwitch, biometricSwitch, ttsSwitch;
    private Spinner fontSizeSpinner;
    private Button updateSettingsButton, changePasswordButton, deactivateAccountButton, deleteAccountButton;
    private TextView dailyGoalTimeDisplay; // Changed from dailyGoalValue
    private Button setGoalTimeButton, cancelGoalTimeButton; // New buttons
    private RadioGroup themeRadioGroup;
    private RadioButton radioLight, radioDark, radioSystem;

    private SharedPreferences preferences;
    private static final String PREF_NAME = "user_settings";

    // Variables to store daily goal time
    private int dailyGoalHour = -1;
    private int dailyGoalMinute = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        initViews();
        createNotificationChannel();
        loadSettings();

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Set listeners for the new Daily Goal Time buttons
        setGoalTimeButton.setOnClickListener(v -> showTimePickerDialog());
        cancelGoalTimeButton.setOnClickListener(v -> clearDailyGoalTime());

        dailyGoalSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Enable/disable the "Set Goal Time" and "Cancel Goal" buttons
            setGoalTimeButton.setEnabled(isChecked);
            cancelGoalTimeButton.setEnabled(isChecked);

            if (isChecked) {
                // Check if notifications are enabled
                if (!notificationSwitch.isChecked()) {
                    Toast.makeText(this, "Please enable push notifications first to use daily learning goals",
                            Toast.LENGTH_LONG).show();
                    dailyGoalSwitch.setChecked(false);
                    return;
                }

                // Check if system notifications are enabled
                if (!areNotificationsEnabled()) {
                    Toast.makeText(this, "Please enable app notifications from system settings first",
                            Toast.LENGTH_LONG).show();
                    dailyGoalSwitch.setChecked(false);
                    openNotificationSettings();
                    return;
                }

                // If daily goal switch is turned on and no time is set, prompt user to set it
                if (dailyGoalHour == -1 || dailyGoalMinute == -1) {
                    Toast.makeText(this, "Please set a daily learning goal time.", Toast.LENGTH_SHORT).show();
                    showTimePickerDialog();
                }

            } else {
                // If daily goal switch is turned off, cancel alarm and clear time
                cancelAlarm(DAILY_GOAL_NOTIFICATION_CODE);
                clearDailyGoalTime(); // Also clear the display and stored time
            }
        });

        biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                authenticateUserForBiometric(() -> biometricSwitch.setChecked(true),
                        () -> biometricSwitch.setChecked(false));
            } else {
                biometricSwitch.setChecked(false);
            }
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!areNotificationsEnabled()) {
                    Toast.makeText(this, "Please enable app notifications from system settings", Toast.LENGTH_LONG).show();
                    notificationSwitch.setChecked(false);
                    openNotificationSettings();
                } else {
                    showInstantNotification("Notifications Enabled");
                }
            } else {
                showInstantNotification("Notifications Disabled");
                cancelAlarm(DAILY_GOAL_NOTIFICATION_CODE);
                // If notifications are disabled, also disable and clear daily goal
                dailyGoalSwitch.setChecked(false);
                setGoalTimeButton.setEnabled(false);
                cancelGoalTimeButton.setEnabled(false);
                clearDailyGoalTime();
            }
        });

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int nightMode;
            if (checkedId == R.id.radioLight) {
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioDark) {
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            AppCompatDelegate.setDefaultNightMode(nightMode);
        });

        Button changePasswordButton = findViewById(R.id.changePasswordButton);
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());


        updateSettingsButton.setOnClickListener(v -> {
            // Additional check when saving settings
            if (dailyGoalSwitch.isChecked() && !notificationSwitch.isChecked()) {
                Toast.makeText(this, "Please enable push notifications first to use daily learning goals",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (dailyGoalSwitch.isChecked() && (dailyGoalHour == -1 || dailyGoalMinute == -1)) {
                Toast.makeText(this, "Please set a time for your daily learning goal.", Toast.LENGTH_LONG).show();
                return;
            }

            saveSettings();
            applyFontSize(false);

            if (notificationSwitch.isChecked() && dailyGoalSwitch.isChecked() && dailyGoalHour != -1) {
                scheduleRepeatingNotification(dailyGoalHour, dailyGoalMinute);
            } else {
                cancelAlarm(DAILY_GOAL_NOTIFICATION_CODE);
            }

            Toast.makeText(this, "Settings Updated!", Toast.LENGTH_SHORT).show();
        });

        deactivateAccountButton.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (user != null) {
                Map<String, Object> accountStatus = new HashMap<>();
                accountStatus.put("Account_is_Active", false);
                accountStatus.put("deactivatedAt", FieldValue.serverTimestamp()); // Timestamp

                Map<String, Object> updates = new HashMap<>();
                updates.put("account_status", accountStatus); // Store as a map

                db.collection("users").document(user.getUid())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(this, "Account deactivated", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, Sign_in.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to deactivate account.", Toast.LENGTH_SHORT).show();
                            Log.e("Deactivate", "Error:", e);
                        });
            }
        });

        deleteAccountButton.setOnClickListener(v -> {
            // Implement account deletion
            Toast.makeText(this, "Delete Account clicked", Toast.LENGTH_SHORT).show();
        });

        checkAndRequestNotificationPermission();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        EditText currentPasswordEditText = dialogView.findViewById(R.id.currentPasswordEditText);
        EditText newPasswordEditText = dialogView.findViewById(R.id.newPasswordEditText);
        EditText confirmPasswordEditText = dialogView.findViewById(R.id.confirmPasswordEditText);

        builder.setView(dialogView)
                .setTitle("Change Password")
                .setPositiveButton("Change", null) // We override later to prevent auto-dismiss
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String currentPass = currentPasswordEditText.getText().toString().trim();
                String newPass = newPasswordEditText.getText().toString().trim();
                String confirmPass = confirmPasswordEditText.getText().toString().trim();

                if (currentPass.isEmpty()) {
                    currentPasswordEditText.setError("Enter current password");
                    currentPasswordEditText.requestFocus();
                    return;
                }
                if (newPass.isEmpty() || newPass.length() < 6) {
                    newPasswordEditText.setError("New password must be at least 6 characters");
                    newPasswordEditText.requestFocus();
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    confirmPasswordEditText.setError("Passwords do not match");
                    confirmPasswordEditText.requestFocus();
                    return;
                }

                // Now perform Firebase password update:
                updateFirebasePassword(currentPass, newPass, dialog);
            });
        });

        dialog.show();
    }

    private void updateFirebasePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user is signed in", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }

        // Reauthenticate user with current password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Update password
                user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "Failed to update password: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        notificationSwitch = findViewById(R.id.notificationSwitch);
        dailyGoalSwitch = findViewById(R.id.dailyGoalSwitch);
        biometricSwitch = findViewById(R.id.biometricSwitch);
        ttsSwitch = findViewById(R.id.ttsSwitch);
        fontSizeSpinner = findViewById(R.id.fontSizeSpinner);
        updateSettingsButton = findViewById(R.id.updateSettingsButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        deactivateAccountButton = findViewById(R.id.deactivateAccountButton);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);

        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        radioLight = findViewById(R.id.radioLight);
        radioDark = findViewById(R.id.radioDark);
        radioSystem = findViewById(R.id.radioSystem);

        // Initialize new Daily Goal Time views
        dailyGoalTimeDisplay = findViewById(R.id.dailyGoalTimeDisplay);
        setGoalTimeButton = findViewById(R.id.setGoalTimeButton);
        cancelGoalTimeButton = findViewById(R.id.cancelGoalTimeButton);
    }

    private void loadSettings() {
        // Notification settings
        boolean notifEnabled = preferences.getBoolean("notifications", false);
        notificationSwitch.setChecked(notifEnabled && areNotificationsEnabled());

        // Daily goal settings
        boolean dailyGoalEnabled = preferences.getBoolean("daily_goal_enabled", true);
        dailyGoalHour = preferences.getInt("daily_goal_hour", -1);
        dailyGoalMinute = preferences.getInt("daily_goal_minute", -1);

        dailyGoalSwitch.setChecked(dailyGoalEnabled);
        updateDailyGoalTimeDisplay(); // Update the TextView with stored time
        setGoalTimeButton.setEnabled(dailyGoalEnabled); // Enable/disable buttons based on switch state
        cancelGoalTimeButton.setEnabled(dailyGoalEnabled);

        // Biometric settings
        biometricSwitch.setChecked(preferences.getBoolean("biometric", false));

        // Theme settings
        int themeMode = preferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                radioLight.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                radioDark.setChecked(true);
                break;
            default:
                radioSystem.setChecked(true);
        }

        // TTS settings
        ttsSwitch.setChecked(preferences.getBoolean("tts_enabled", false));

        // Font size
        fontSizeSpinner.setSelection(preferences.getInt("font_size", 1));
        applyFontSize(false);

        // Schedule notification if enabled and time is set
        if (notifEnabled && dailyGoalEnabled && dailyGoalHour != -1) {
            scheduleRepeatingNotification(dailyGoalHour, dailyGoalMinute);
        }
    }

    private void saveSettings() {
        int themeMode;
        if (radioLight.isChecked()) {
            themeMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (radioDark.isChecked()) {
            themeMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("notifications", notificationSwitch.isChecked());
        editor.putBoolean("daily_goal_enabled", dailyGoalSwitch.isChecked());
        editor.putBoolean("biometric", biometricSwitch.isChecked());
        editor.putInt("theme_mode", themeMode);
        editor.putBoolean("tts_enabled", ttsSwitch.isChecked());
        editor.putInt("font_size", fontSizeSpinner.getSelectedItemPosition());

        // Save daily goal time
        if (dailyGoalSwitch.isChecked() && dailyGoalHour != -1) {
            editor.putInt("daily_goal_hour", dailyGoalHour);
            editor.putInt("daily_goal_minute", dailyGoalMinute);
        } else {
            editor.remove("daily_goal_hour");
            editor.remove("daily_goal_minute");
        }
        editor.apply();
    }

    private void applyFontSize(boolean recreateActivity) {
        float fontSizeSp = 16f; // Default medium
        switch (fontSizeSpinner.getSelectedItemPosition()) {
            case 0:
                fontSizeSp = 14f;
                break; // Small
            case 2:
                fontSizeSp = 18f;
                break; // Large
        }

        View rootView = findViewById(android.R.id.content);
        updateFontSizeRecursive(rootView, fontSizeSp);

        if (recreateActivity) {
            recreate();
        }
    }

    private void updateFontSizeRecursive(View view, float fontSizeSp) {
        if (view instanceof TextView) {
            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateFontSizeRecursive(group.getChildAt(i), fontSizeSp);
            }
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATION_PERMISSION);
            }
        }
    }

    private void showInstantNotification(String message) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Settings Updated")
                .setContentText(message)
                .setSmallIcon(R.drawable.app_icon)
                .setAutoCancel(true);

        manager.notify(NOTIFICATION_CODE, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    // Updated to schedule notification at a specific time
    private void scheduleRepeatingNotification(int hourOfDay, int minute) {
        if (!areNotificationsEnabled()) {
            Toast.makeText(this, "Enable notifications in system settings first!", Toast.LENGTH_LONG).show();
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, DailyGoalNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, DAILY_GOAL_NOTIFICATION_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the set time is in the past, set it for the next day
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            // Set alarm to repeat daily
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, // Repeat every day
                    pendingIntent);
            Toast.makeText(this, String.format("Daily goal reminder set for %02d:%02d", hourOfDay, minute), Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelAlarm(int requestCode) {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, DailyGoalNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (manager != null) manager.cancel(pendingIntent);
        Toast.makeText(this, "Daily goal reminder cancelled", Toast.LENGTH_SHORT).show();
    }

    private boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private void openNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    private void authenticateUserForBiometric(Runnable onSuccess, Runnable onFailure) {
        BiometricManager biometricManager = BiometricManager.from(this);

        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                == BiometricManager.BIOMETRIC_SUCCESS) {

            Executor executor = ContextCompat.getMainExecutor(this);

            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            onSuccess.run();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            Toast.makeText(Setting_Activity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            onFailure.run();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Toast.makeText(Setting_Activity.this, "Error: " + errString, Toast.LENGTH_SHORT).show();
                            onFailure.run();
                        }
                    });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Authentication")
                    .setSubtitle("Verify your identity to enable biometric access")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();

            biometricPrompt.authenticate(promptInfo);

        } else {
            Toast.makeText(this, "Biometric or screen lock not available", Toast.LENGTH_LONG).show();
            onFailure.run();
        }
    }

    // Shows a TimePickerDialog to let the user pick a time
    private void showTimePickerDialog() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        if (dailyGoalHour != -1 && dailyGoalMinute != -1) {
            hour = dailyGoalHour;
            minute = dailyGoalMinute;
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHourOfDay, selectedMinute) -> {
                    dailyGoalHour = selectedHourOfDay;
                    dailyGoalMinute = selectedMinute;
                    updateDailyGoalTimeDisplay();
                    Toast.makeText(this, String.format("Daily goal time set to %02d:%02d", dailyGoalHour, dailyGoalMinute), Toast.LENGTH_SHORT).show();
                }, hour, minute, false); // 'false' for 12-hour format, 'true' for 24-hour format
        timePickerDialog.show();
    }

    // Updates the TextView displaying the chosen time
    private void updateDailyGoalTimeDisplay() {
        if (dailyGoalHour != -1 && dailyGoalMinute != -1) {
            dailyGoalTimeDisplay.setText(String.format(Locale.getDefault(), "Goal Time: %02d:%02d", dailyGoalHour, dailyGoalMinute));
        } else {
            dailyGoalTimeDisplay.setText("Not set");
        }
    }

    // Clears the stored daily goal time and updates the display
    private void clearDailyGoalTime() {
        dailyGoalHour = -1;
        dailyGoalMinute = -1;
        updateDailyGoalTimeDisplay();
        cancelAlarm(DAILY_GOAL_NOTIFICATION_CODE); // Cancel any existing alarm
        Toast.makeText(this, "Daily goal time cleared.", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                notificationSwitch.setChecked(true);
            } else {
                notificationSwitch.setChecked(false);
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}