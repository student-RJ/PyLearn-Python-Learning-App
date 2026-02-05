package com.example.python1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class DailyReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Reminder", "BroadcastReceiver triggered");
        String channelId = "daily_reminder_channel";
        CharSequence name = "Daily Reminder";
        String description = "Reminder notification for daily learning";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        // Create channel for Android O+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        // Intent to open app
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 101, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.app_icon) // change to your icon
                .setContentTitle("Daily Reminder")
                .setContentText("Time to continue learning Python on PyLearn!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(100, builder.build());
    }
}
