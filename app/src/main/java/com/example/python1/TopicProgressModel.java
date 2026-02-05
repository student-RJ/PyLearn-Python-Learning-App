package com.example.python1;

public class TopicProgressModel {
    private String topic;
    private long score;
    private long total;
    private double percentage;
    private double averageScore;
    private long quizzesAttempted;

    private long bestTimeSpent;
    private long highestScore;
    private long totalScore;
    private long totalTimeSpent;
    private boolean quizCompleted;

    // Constructor with all fields
    public TopicProgressModel(String topic, long score, long total, double percentage,
                              double averageScore, long quizzesAttempted,
                              long bestTimeSpent, long highestScore,
                              long totalScore, long totalTimeSpent,
                              boolean quizCompleted) {
        this.topic = topic;
        this.score = score;
        this.total = total;
        this.percentage = percentage;
        this.averageScore = averageScore;
        this.quizzesAttempted = quizzesAttempted;
        this.bestTimeSpent = bestTimeSpent;
        this.highestScore = highestScore;
        this.totalScore = totalScore;
        this.totalTimeSpent = totalTimeSpent;
        this.quizCompleted = quizCompleted;
    }

    // Getters
    public String getTopic() { return topic; }
    public long getScore() { return score; }
    public long getTotal() { return total; }
    public double getPercentage() { return percentage; }
    public double getAverageScore() { return averageScore; }
    public long getQuizzesAttempted() { return quizzesAttempted; }
    public long getBestTimeSpent() { return bestTimeSpent; }
    public long getHighestScore() { return highestScore; }
    public long getTotalScore() { return totalScore; }
    public long getTotalTimeSpent() { return totalTimeSpent; }
    public boolean isQuizCompleted() { return quizCompleted; }

    // (Optional) setters if needed
}
