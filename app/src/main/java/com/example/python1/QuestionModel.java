package com.example.python1;

import java.util.List;

public class QuestionModel {
    private String question;
    private List<String> options;
    private String answer; // Add this field to store the correct answer

    public QuestionModel() {
        // Default constructor (needed for Firebase)
    }

    public QuestionModel(String question, List<String> options, String answer) {
        this.question = question;
        this.options = options;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getAnswer() { // Add this getter method
        return answer;
    }
}
