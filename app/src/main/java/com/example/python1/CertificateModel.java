package com.example.python1;

public class CertificateModel {
    private String topic;
    private String date;
    private int score;
    private String certificateUrl;
    private String id;
    private boolean isPreviewVisible; // new// this exists but no getter/setter

    public CertificateModel() {
        // Required empty constructor for Firebase
    }

    // Add 'id' parameter here if you want
    public CertificateModel(String topic, String date, int score, String certificateUrl) {
        this.topic = topic;
        this.date = date;
        this.score = score;
        this.certificateUrl = certificateUrl;
        this.isPreviewVisible = false; // default

    }

    public boolean isPreviewVisible() {
        return isPreviewVisible;
    }

    public void setPreviewVisible(boolean previewVisible) {
        isPreviewVisible = previewVisible;
    }


    // Existing getters
    public String getTopic() {
        return topic;
    }

    public String getDate() {
        return date;
    }

    public int getScore() {
        return score;
    }

    public String getCertificateUrl() {
        return certificateUrl;
    }

    // Add getter and setter for id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
