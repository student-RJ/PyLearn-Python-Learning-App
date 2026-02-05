// Message.java
package com.example.python1; // Replace with your actual package name

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {
    private String text;
    private boolean isUser;
    private String timestamp;
    private String avatarUrl; // New field for user's avatar URL
    private String userName;  // New field for user's name

    // Constructor for AI messages (no avatar/name needed)
    public Message(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        this.avatarUrl = null; // Default to null for AI messages
        this.userName = isUser ? "You" : "AI"; // Default based on type
    }

    // New constructor for User messages (with avatar and name)
    public Message(String text, boolean isUser, String avatarUrl, String userName) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        this.avatarUrl = avatarUrl;
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getUserName() {
        return userName;
    }
}