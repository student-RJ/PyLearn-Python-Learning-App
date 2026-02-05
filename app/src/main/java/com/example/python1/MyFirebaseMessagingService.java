package com.example.python1;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

        private static final String CHANNEL_ID = "pylearn_channel";
        private static final String CHANNEL_NAME = "PyLearn Notifications";
        private static final String CHANNEL_DESC = "Notifications for quiz reminders and updates";

        @Override
        public void onMessageReceived(RemoteMessage remoteMessage) {
            super.onMessageReceived(remoteMessage);

            Log.d("FCM_MESSAGE", "Message received");

            // Make sure notification is present (can be data-only)
            if (remoteMessage.getNotification() != null) {
                String title = remoteMessage.getNotification().getTitle();
                String message = remoteMessage.getNotification().getBody();

                Log.d("FCM_MESSAGE", "Title: " + title + ", Message: " + message);

                showNotification(title, message);
            } else if (remoteMessage.getData().size() > 0) {
                // Handle data-only notification
                String title = remoteMessage.getData().get("title");
                String message = remoteMessage.getData().get("message");

                Log.d("FCM_DATA", "Data Message: " + title + " - " + message);

                showNotification(title, message);
            }
        }

        private void showNotification(String title, String message) {
            // Create notification channel if needed (Android 8+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(CHANNEL_DESC);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.back_icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat manager = NotificationManagerCompat.from(this);

            // Android 13+ permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    manager.notify(101, builder.build());
                } else {
                    Log.w("FCM", "Notification permission not granted.");
                }
            } else {
                manager.notify(101, builder.build());
            }
        }

        @Override
        public void onNewToken(@NonNull String token) {
            super.onNewToken(token);
            Log.d("FCM_TOKEN", "New Token: " + token);
            // Save the token to Firestore or your backend if needed
        }
    }
