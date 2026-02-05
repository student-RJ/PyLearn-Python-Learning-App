package com.example.python1;

public class ChatMessage {
    public String content;
    public boolean isUser; // true if user message, false if bot message

    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
    }
}

