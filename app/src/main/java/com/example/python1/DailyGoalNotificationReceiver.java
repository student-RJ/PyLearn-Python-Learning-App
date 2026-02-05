package com.example.python1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class DailyGoalNotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "daily_goal_channel";
    private static final String TAG = "GeminiNotification";

    // Store API key in strings.xml or BuildConfig for security
    // For now, replace with your key but consider moving to secure storage
    private static final String GEMINI_API_KEY = "AIzaSyD6rJ68fDcmBX9hyTjka9tY4goq4snX1D4"; // Your original key
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

    // Fallback notifications for variety when API fails
    private static final String[][] FALLBACK_NOTIFICATIONS = {
            {"Learning Time! 📚", "Your daily goal is waiting for you!"},
            {"Study Break 💡", "Take 30 minutes to expand your knowledge today!"},
            {"Goal Crusher 🎯", "Ready to tackle your learning objectives?"},
            {"Brain Training 🧠", "Feed your mind with something new!"},
            {"Knowledge Quest ⭐", "Every expert was once a beginner. Start now!"},
            {"Study Sprint 🚀", "Small steps daily lead to big achievements!"},
            {"Mind Fuel ⚡", "Charge up your brain with today's learning!"},
            {"Skill Builder 💪", "Consistency is key to mastering new skills!"},
            {"Learning Journey 🌟", "Another day, another opportunity to grow!"},
            {"Focus Mode 🔥", "Turn your learning time into power time!"}
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);
        Log.d(TAG, "Daily notification receiver triggered");

        // Generate notification using Gemini AI with improved fallback
        new GeminiNotificationTask(context).execute();
    }

    private class GeminiNotificationTask extends AsyncTask<Void, Void, String[]> {
        private Context context;

        public GeminiNotificationTask(Context context) {
            this.context = context;
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            try {
                Log.d(TAG, "Attempting to generate Gemini notification");
                String[] result = generateGeminiNotification(context);
                if (result != null && result.length == 2 && !result[0].isEmpty() && !result[1].isEmpty()) {
                    Log.d(TAG, "Gemini notification generated successfully");
                    return result;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating Gemini notification: " + e.getMessage(), e);
            }

            // Return varied fallback notification
            Log.d(TAG, "Using fallback notification");
            return getRandomFallbackNotification(context);
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null && result.length == 2) {
                showNotification(context, result[0], result[1]);
            } else {
                // Last resort fallback
                String[] fallback = getRandomFallbackNotification(context);
                showNotification(context, fallback[0], fallback[1]);
            }
        }
    }

    private String[] getRandomFallbackNotification(Context context) {
        // Add some personalization even to fallback notifications
        String userContext = getUserContext(context);
        Random random = new Random();

        // Select random fallback
        String[] baseNotification = FALLBACK_NOTIFICATIONS[random.nextInt(FALLBACK_NOTIFICATIONS.length)];

        // Add some personalization based on time/context
        String personalizedMessage = personalizeMessage(baseNotification[1], userContext, context);

        return new String[]{baseNotification[0], personalizedMessage};
    }

    private String personalizeMessage(String baseMessage, String userContext, Context context) {
        try {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            // Add time-based personalization
            if (hour >= 6 && hour < 12) {
                baseMessage = "Good morning! " + baseMessage;
            } else if (hour >= 12 && hour < 17) {
                baseMessage = "Afternoon boost: " + baseMessage;
            } else if (hour >= 17 && hour < 21) {
                baseMessage = "Evening progress: " + baseMessage;
            }

            // Add streak motivation if available
            SharedPreferences prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
            int streak = prefs.getInt("learning_streak", 0);
            if (streak > 1) {
                baseMessage += " Keep your " + streak + "-day streak going!";
            }

        } catch (Exception e) {
            Log.e(TAG, "Error personalizing message", e);
        }

        return baseMessage;
    }

    private String[] generateGeminiNotification(Context context) throws Exception {
        // Get user context for personalization
        String userContext = getUserContext(context);
        Log.d(TAG, "User context: " + userContext);

        // Create prompt for Gemini
        String prompt = createGeminiPrompt(userContext);
        Log.d(TAG, "Generated prompt: " + prompt);

        // Call Gemini API
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        // Configure generation settings for more creative responses
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.9); // Increased for more variety
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 200);
        requestBody.put("generationConfig", generationConfig);

        // Add safety settings
        JSONArray safetySettings = new JSONArray();
        requestBody.put("safetySettings", safetySettings);

        Log.d(TAG, "Sending request to Gemini API");

        // Make HTTP request
        URL url = new URL(GEMINI_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(15000); // 15 seconds

        // Send request
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(requestBody.toString());
        writer.flush();
        writer.close();

        // Check response code
        int responseCode = connection.getResponseCode();
        Log.d(TAG, "API Response Code: " + responseCode);

        if (responseCode != 200) {
            throw new Exception("API returned error code: " + responseCode);
        }

        // Read response
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        String responseStr = response.toString();
        Log.d(TAG, "API Response: " + responseStr);

        // Parse response
        JSONObject jsonResponse = new JSONObject(responseStr);

        if (!jsonResponse.has("candidates")) {
            throw new Exception("No candidates in API response");
        }

        JSONArray candidates = jsonResponse.getJSONArray("candidates");
        if (candidates.length() == 0) {
            throw new Exception("Empty candidates array");
        }

        JSONObject firstCandidate = candidates.getJSONObject(0);

        if (!firstCandidate.has("content")) {
            throw new Exception("No content in candidate");
        }

        JSONObject content_response = firstCandidate.getJSONObject("content");
        JSONArray parts_response = content_response.getJSONArray("parts");
        String generatedText = parts_response.getJSONObject(0).getString("text");

        Log.d(TAG, "Generated text: " + generatedText);

        // Parse the generated text to extract title and message
        return parseGeminiResponse(generatedText);
    }

    private String getUserContext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);

        // Get current time info
        Calendar calendar = Calendar.getInstance();
        String timeOfDay = getTimeOfDay(calendar.get(Calendar.HOUR_OF_DAY));
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());

        // Get user preferences
        int dailyGoal = prefs.getInt("daily_goal", 30);
        boolean isWeekend = (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);

        // Get learning streak or progress
        int learningStreak = prefs.getInt("learning_streak", 1);

        // Add randomness to context for variety
        Random random = new Random();
        String[] motivationalWords = {"achieve", "excel", "grow", "improve", "succeed", "advance"};
        String randomWord = motivationalWords[random.nextInt(motivationalWords.length)];

        return String.format("Time: %s, Day: %s, Daily Goal: %d minutes, Weekend: %s, Learning Streak: %d days, Focus: %s",
                timeOfDay, dayOfWeek, dailyGoal, isWeekend, learningStreak, randomWord);
    }

    private String getTimeOfDay(int hour) {
        if (hour >= 5 && hour < 12) return "Morning";
        else if (hour >= 12 && hour < 17) return "Afternoon";
        else if (hour >= 17 && hour < 21) return "Evening";
        else return "Night";
    }

    private String createGeminiPrompt(String userContext) {
        String[] promptVariations = {
                "Create a unique motivational learning reminder. Context: %s. Generate a catchy title (max 8 words) and inspiring message (max 30 words) to encourage daily learning. Be creative and fresh! Format: TITLE: [title] MESSAGE: [message]",

                "Generate a personalized study reminder that feels different from yesterday. User context: %s. Create an engaging title (under 8 words) and motivational message (under 30 words) that's energizing and unique. Format: TITLE: [title] MESSAGE: [message]",

                "Create a fresh daily learning notification that stands out. Context: %s. Make a compelling title (8 words max) and uplifting message (30 words max) that motivates with creativity. Format: TITLE: [title] MESSAGE: [message]",

                "Generate an innovative learning reminder with personality. Given: %s. Craft an attention-grabbing title (max 8 words) and positive message (max 30 words) that feels genuine and different. Format: TITLE: [title] MESSAGE: [message]",

                "Create a personalized study notification with flair. Context: %s. Design an inspiring title (under 8 words) and encouraging message (under 30 words) that promotes learning with originality. Format: TITLE: [title] MESSAGE: [message]",

                "Generate a creative daily goal reminder that surprises. Context: %s. Make an exciting title (8 words max) and motivational message (30 words max) that feels fresh and personal. Format: TITLE: [title] MESSAGE: [message]"
        };

        Random random = new Random();
        String selectedPrompt = promptVariations[random.nextInt(promptVariations.length)];

        return String.format(selectedPrompt, userContext);
    }

    private String[] parseGeminiResponse(String response) {
        try {
            // Clean the response
            response = response.trim();
            Log.d(TAG, "Parsing response: " + response);

            String title = "";
            String message = "";

            // Try multiple parsing approaches
            if (response.contains("TITLE:") && response.contains("MESSAGE:")) {
                // Method 1: Standard format
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (line.trim().startsWith("TITLE:")) {
                        title = line.replace("TITLE:", "").trim();
                    } else if (line.trim().startsWith("MESSAGE:")) {
                        message = line.replace("MESSAGE:", "").trim();
                    }
                }

                // If that didn't work, try splitting by MESSAGE:
                if (title.isEmpty() || message.isEmpty()) {
                    String[] parts = response.split("MESSAGE:");
                    if (parts.length >= 2) {
                        title = parts[0].replace("TITLE:", "").trim();
                        message = parts[1].trim();
                    }
                }
            } else {
                // Method 2: Try to extract meaningful content even without format
                String[] sentences = response.split("[.!?]");
                if (sentences.length >= 2) {
                    title = sentences[0].trim();
                    message = sentences[1].trim();
                } else if (response.length() > 10 && response.length() < 200) {
                    // Use first part as title, rest as message
                    String[] words = response.split(" ");
                    if (words.length > 6) {
                        title = String.join(" ", java.util.Arrays.copyOfRange(words, 0, 6));
                        message = String.join(" ", java.util.Arrays.copyOfRange(words, 6, words.length));
                    } else {
                        title = "Learning Time!";
                        message = response;
                    }
                }
            }

            // Validate and clean up
            if (title.isEmpty() || title.length() > 60) {
                title = "Daily Learning Goal";
            }

            if (message.isEmpty() || message.length() > 150) {
                message = "Time to continue your learning journey!";
            }

            // Add emoji if not present in title
            if (!title.matches(".*[📚📖💡🎯⭐🚀💪🔥⚡✨🧠🌟].*")) {
                String[] emojis = {"📚", "💡", "🎯", "⭐", "🚀", "💪", "🔥", "⚡", "✨", "🧠", "🌟"};
                title += " " + emojis[new Random().nextInt(emojis.length)];
            }

            Log.d(TAG, "Parsed - Title: '" + title + "', Message: '" + message + "'");
            return new String[]{title, message};

        } catch (Exception e) {
            Log.e(TAG, "Error parsing Gemini response", e);
            return null; // Return null to trigger fallback
        }
    }

    private void showNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification1)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 100, 500})
                .setLights(0xFF0000FF, 1000, 500);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Use random notification ID to avoid replacing previous notifications during testing
        int notificationId = 1001 + (int)(Math.random() * 100);
        notificationManager.notify(notificationId, builder.build());

        Log.d(TAG, "Notification displayed - Title: '" + title + "', Message: '" + message + "'");
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Daily Goal Channel";
            String description = "AI-powered daily goal reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLightColor(0xFF0000FF);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}