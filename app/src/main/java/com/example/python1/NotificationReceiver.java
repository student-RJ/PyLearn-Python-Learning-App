package com.example.python1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Build and show notification here
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notifyPyLearn")
                .setSmallIcon(R.drawable.app_icon) // your app icon
                .setContentTitle("PyLearn Daily Reminder")
                .setContentText("Don't forget to continue your Python learning today!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(101, builder.build());
    }
}

