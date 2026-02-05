package com.example.python1;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class QuizActivity extends BaseActivity {
    private TextView tvQuestion, tvQuestionProgress, tvTimer, tvStatus;
    private RadioGroup radioGroupOptions;
    private RadioButton rbOption1, rbOption2, rbOption3, rbOption4;
    private Button btnPrevious, btnNext, btnMarkForReview, btnSubmit;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;
    private List<QuestionModel> questionList;
    private Map<Integer, String> userAnswers;
    private List<Integer> markedForReview;
    private int currentQuestionIndex = 0;
    private CountDownTimer sessionTimer;
    private long totalSessionTime = 0; // Total time limit for the quiz session
    private String selectedTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvQuestion = findViewById(R.id.tvQuestion);
        tvQuestionProgress = findViewById(R.id.tvQuestionProgress);
        tvTimer = findViewById(R.id.tvTimer);
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        rbOption1 = findViewById(R.id.rbOption1);
        rbOption2 = findViewById(R.id.rbOption2);
        rbOption3 = findViewById(R.id.rbOption3);
        rbOption4 = findViewById(R.id.rbOption4);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnMarkForReview = findViewById(R.id.btnMarkForReview);
        btnSubmit = findViewById(R.id.btnSubmit);

        selectedTopic = getIntent().getStringExtra("selectedTopic");
        if (selectedTopic == null || selectedTopic.isEmpty()) {
            Toast.makeText(this, "Error: No topic selected!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        databaseReference = FirebaseDatabase.getInstance("https://python1-32390-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("quiz").child(selectedTopic);

        questionList = new ArrayList<>();
        userAnswers = new HashMap<>();
        markedForReview = new ArrayList<>();

        fetchQuizDetails(); // Fetch questions and time limit

        btnNext.setOnClickListener(v -> nextQuestion());
        btnPrevious.setOnClickListener(v -> previousQuestion());
        btnMarkForReview.setOnClickListener(v -> markForReview());
        btnSubmit.setOnClickListener(v -> submitQuiz());
    }

    private void fetchQuizDetails() {
        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot dataSnapshot = task.getResult();

                // Fetch time limit for the session
                if (dataSnapshot.child("time_limit").exists()) {
                    totalSessionTime = dataSnapshot.child("time_limit").getValue(Long.class) * 1000; // Convert to milliseconds
                } else {
                    totalSessionTime = 40000; // Default to 30 seconds if not found
                }

                // Fetch questions
                for (DataSnapshot questionSnapshot : dataSnapshot.child("questions").getChildren()) {
                    QuestionModel question = questionSnapshot.getValue(QuestionModel.class);
                    if (question != null) {
                        questionList.add(question);
                    }
                }

                if (!questionList.isEmpty()) {
                    startSessionTimer();
                    displayQuestion();
                } else {
                    Toast.makeText(this, "No questions available for " + selectedTopic, Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Failed to load quiz data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startSessionTimer() {
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        sessionTimer = new CountDownTimer(totalSessionTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60));

                if (millisUntilFinished <= 5000) {
                    tvTimer.setTextColor(getResources().getColor(R.color.red));
                }
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                submitQuiz(); // Auto-submit when time is up
            }
        }.start();
    }

    private void displayQuestion() {
        if (currentQuestionIndex < questionList.size()) {
            QuestionModel question = questionList.get(currentQuestionIndex);
            tvQuestion.setText(question.getQuestion());
            rbOption1.setText(question.getOptions().get(0));
            rbOption2.setText(question.getOptions().get(1));
            rbOption3.setText(question.getOptions().get(2));
            rbOption4.setText(question.getOptions().get(3));
            tvQuestionProgress.setText("Question " + (currentQuestionIndex + 1) + "/" + questionList.size());
            tvStatus.setText("");

            // Update Progress Bar
            int progress = (int) (((double) (currentQuestionIndex + 1) / questionList.size()) * 100);
            progressBar.setProgress(progress);

            // **Disable Previous Button for First Question**
            if (currentQuestionIndex == 0) {
                btnPrevious.setEnabled(false);
                btnPrevious.setAlpha(0.5f); // Transparent effect
            } else {
                btnPrevious.setEnabled(true);
                btnPrevious.setAlpha(1f);
            }

            // **Handle "Next" Button for Last Question**
            if (currentQuestionIndex == questionList.size() - 1) {
                btnNext.setText("Finish Exam");
                btnNext.setOnClickListener(v -> showFinishDialog());
            } else {
                btnNext.setText("Next");
                btnNext.setOnClickListener(v -> nextQuestion());
            }

            // Clear previous selections
            radioGroupOptions.clearCheck();

            // Restore user’s previous selection
            if (userAnswers.containsKey(currentQuestionIndex)) {
                String savedAnswer = userAnswers.get(currentQuestionIndex);
                if (rbOption1.getText().toString().equals(savedAnswer)) rbOption1.setChecked(true);
                if (rbOption2.getText().toString().equals(savedAnswer)) rbOption2.setChecked(true);
                if (rbOption3.getText().toString().equals(savedAnswer)) rbOption3.setChecked(true);
                if (rbOption4.getText().toString().equals(savedAnswer)) rbOption4.setChecked(true);
            }

            // Show "Marked for Review" status if marked
            if (markedForReview.contains(currentQuestionIndex)) {
                tvStatus.setText("Marked for Review");
                tvStatus.setTextColor(getResources().getColor(R.color.orange));
            }
        }
    }

    // **Show Confirmation Dialog Before Submitting**
    private void showFinishDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Finish Exam?");
        builder.setMessage("Are you sure you want to submit the quiz?");
        builder.setPositiveButton("Yes", (dialog, which) -> submitQuiz());
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // **Show Marked for Review Questions at the End**
    private void checkReviewQuestions() {
        if (!markedForReview.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Review Marked Questions");
            builder.setMessage("You have " + markedForReview.size() + " marked questions. Do you want to review them before submitting?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                currentQuestionIndex = markedForReview.get(0);
                displayQuestion();
            });
            builder.setNegativeButton("No", (dialog, which) -> submitQuiz());
            builder.show();
        } else {
            submitQuiz();
        }
    }


    private void nextQuestion() {
        saveUserAnswer();
        if (currentQuestionIndex < questionList.size() - 1) {
            currentQuestionIndex++;
            displayQuestion();
        } else {
            Toast.makeText(this, "This is the last question", Toast.LENGTH_SHORT).show();
        }
    }

    private void previousQuestion() {
        saveUserAnswer();
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            displayQuestion();
        }
    }

    private void markForReview() {
        int selectedId = radioGroupOptions.getCheckedRadioButtonId();

        if (!userAnswers.containsKey(currentQuestionIndex)) { // Only allow marking if unanswered
            if (!markedForReview.contains(currentQuestionIndex)) {
                markedForReview.add(currentQuestionIndex);
                tvStatus.setText("Marked for Review");
                tvStatus.setTextColor(getResources().getColor(R.color.orange));
            } else {
                markedForReview.remove((Integer) currentQuestionIndex);
                tvStatus.setText("");
            }
        } else {
            Toast.makeText(this, "Answered questions cannot be marked for review", Toast.LENGTH_SHORT).show();
        }
    }



    private void saveUserAnswer() {
        int selectedId = radioGroupOptions.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            userAnswers.put(currentQuestionIndex, selectedRadioButton.getText().toString());
        }
    }

    // At the end of the submitQuiz() method, after calculating the results:

    private void submitQuiz() {
        saveUserAnswer(); // Save last selected answer

        int correctAnswers = 0;
        int incorrectAnswers = 0;
        List<String> correctQuestions = new ArrayList<>();
        List<String> incorrectQuestions = new ArrayList<>();
        List<String> unattemptedQuestions = new ArrayList<>();

        for (int i = 0; i < questionList.size(); i++) {
            QuestionModel question = questionList.get(i);
            String correctAnswer = question.getAnswer();
            String userAnswer = userAnswers.get(i);

            if (userAnswer == null || userAnswer.isEmpty()) {
                unattemptedQuestions.add("Q: " + question.getQuestion() + "\nCorrect: " + correctAnswer);
            } else if (userAnswer.equals(correctAnswer)) {
                correctAnswers++;
                correctQuestions.add("Q: " + question.getQuestion() + "\n✅ Your Answer: " + userAnswer);
            } else {
                incorrectAnswers++;
                incorrectQuestions.add("Q: " + question.getQuestion() + "\n❌ Your Answer: " + userAnswer + "\n✅ Correct: " + correctAnswer);
            }
        }

        int totalQuestions = questionList.size();
        int overallProgress = (int) (((double) correctAnswers / totalQuestions) * 100);

        // Pass all data to ResultActivity
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("totalQuestions", totalQuestions);
        intent.putExtra("correctAnswers", correctAnswers);
        intent.putExtra("incorrectAnswers", incorrectAnswers);
        intent.putExtra("unattemptedAnswers", unattemptedQuestions.size());
        intent.putExtra("overallProgress", overallProgress);
        intent.putExtra("totalSessionTime", totalSessionTime);
        intent.putExtra("selectedTopic", selectedTopic);
        intent.putExtra("selectedTopic", selectedTopic); // ✅ Pass the selected topic
        intent.putStringArrayListExtra("correctQuestions", (ArrayList<String>) correctQuestions);
        intent.putStringArrayListExtra("incorrectQuestions", (ArrayList<String>) incorrectQuestions);
        intent.putStringArrayListExtra("unattemptedQuestions", (ArrayList<String>) unattemptedQuestions);

        if (sessionTimer != null) sessionTimer.cancel(); // Cancel timer if still running

        startActivity(intent);
        finish();
    }

}
