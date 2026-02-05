package com.example.python1;

public class UserProgress {
    private String userId;
    private String username;
    private String profileImageUrl;
    private double percentage;
    private String badges; // New field for badges


    public UserProgress() {

    }

    public UserProgress(String userId, String username, String profileImageUrl, double percentage) {
        this.userId = userId;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        this.percentage = percentage;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public double getPercentage() { return percentage; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    @Override
    public String toString() {
        return "UserProgress{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", percentage=" + percentage +
                '}';
    }
}

