package com.example.python1;

import java.util.List;

public class OpenAIResponse {
    public List<Choice> choices;

    public class Choice {
        public Message message;

        public class Message {
            public String content;
        }
    }
}

