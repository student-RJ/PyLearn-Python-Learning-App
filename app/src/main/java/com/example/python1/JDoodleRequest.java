package com.example.python1;

public class JDoodleRequest {
    private String clientId;
    private String clientSecret;
    private String script;
    private String language;
    private String versionIndex;

    // Constructor, getters, and setters
    public JDoodleRequest(String clientId, String clientSecret, String script, String language, String versionIndex) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.script = script;
        this.language = language;
        this.versionIndex = versionIndex;
    }

    // Getters and Setters...
}

