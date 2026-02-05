package com.example.python1;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import org.json.*;
import java.util.*;
import android.graphics.Typeface;
import android.view.Gravity;
import android.util.Log;

public class DailyChallengeActivity extends AppCompatActivity {

    private static final String TAG = "DailyChallengeActivity";

    // XML View References
    private TextView challengeTitle;
    private TextView challengeDescription;
    private TextView challengeTopic;
    private TextView challengeDifficulty;
    private TextView challengeType;
    private MaterialButton nextChallengeBtn;
    private MaterialButton submitAnswerBtn;
    private TextView scoreText;
    private CircularProgressIndicator loadingProgress;
    private MaterialCardView loadingCard;
    private MaterialCardView challengeContentCard;
    private MaterialCardView progressCard;
    private LinearProgressIndicator challengeProgress;
    private TextView progressText;
    private TextView progressPercentage;
    private LinearLayout progressSteps;

    // Challenge Type Containers
    private LinearLayout multipleChoiceContainer;
    private RadioGroup multipleChoiceOptions;
    private LinearLayout trueFalseContainer;
    private MaterialButton trueButton;
    private MaterialButton falseButton;
    private LinearLayout fillBlankContainer;
    private TextInputEditText fillBlankInput;
    private MaterialCardView codeChallenge;
    private EditText codeInput;

    // Back button
    private MaterialButton backButton;

    // Gemini API Helper
    private GeminiAPIHelper geminiAPIHelper;

    private List<PythonChallenge> dailyChallenges;
    private int currentChallengeIndex = 0;
    private int totalScore = 0;
    private int challengesGenerated = 0;
    private final int TOTAL_CHALLENGES = 5;

    // User progress tracking
    private UserProgress userProgress;
    private String[] pythonTopics = {
            "data_types", "variables", "loops", "functions", "lists",
            "dictionaries", "classes", "file_handling", "error_handling",
            "modules", "decorators", "generators"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_challenge);

        initViews();
        setupGeminiAI();
        loadUserProgress();
        generateDailyChallenges();
    }

    private void initViews() {
        // Header views
        challengeTitle = findViewById(R.id.challenge_title);
        challengeDescription = findViewById(R.id.challenge_description);
        challengeTopic = findViewById(R.id.challenge_topic);
        challengeDifficulty = findViewById(R.id.challenge_difficulty);
        challengeType = findViewById(R.id.challenge_type);
        scoreText = findViewById(R.id.score_text);
        backButton = findViewById(R.id.back_button);

        // Loading views
        loadingProgress = findViewById(R.id.loading_progress);
        loadingCard = findViewById(R.id.loading_card);

        // Challenge content views
        challengeContentCard = findViewById(R.id.challenge_content_card);

        // Progress views
        progressCard = findViewById(R.id.progress_card);
        challengeProgress = findViewById(R.id.challenge_progress);
        progressText = findViewById(R.id.progress_text);
        progressPercentage = findViewById(R.id.progress_percentage);
        progressSteps = findViewById(R.id.progress_steps);

        // Button views
        nextChallengeBtn = findViewById(R.id.next_challenge_btn);
        submitAnswerBtn = findViewById(R.id.submit_answer_btn);

        // Challenge type containers
        multipleChoiceContainer = findViewById(R.id.multiple_choice_container);
        multipleChoiceOptions = findViewById(R.id.multiple_choice_options);
        trueFalseContainer = findViewById(R.id.true_false_container);
        trueButton = findViewById(R.id.true_button);
        falseButton = findViewById(R.id.false_button);
        fillBlankContainer = findViewById(R.id.fill_blank_container);
        fillBlankInput = findViewById(R.id.fill_blank_input);
        codeChallenge = findViewById(R.id.code_challenge_container);
        codeInput = findViewById(R.id.code_input);

        // Set click listeners
        nextChallengeBtn.setOnClickListener(v -> nextChallenge());
        submitAnswerBtn.setOnClickListener(v -> submitCurrentAnswer());
        backButton.setOnClickListener(v -> finish());

        // True/False button listeners
        trueButton.setOnClickListener(v -> {
            trueButton.setSelected(true);
            falseButton.setSelected(false);
        });

        falseButton.setOnClickListener(v -> {
            falseButton.setSelected(true);
            trueButton.setSelected(false);
        });
    }

    private void setupGeminiAI() {
        geminiAPIHelper = new GeminiAPIHelper();
        Log.d(TAG, "Gemini API Helper initialized");
    }

    private void loadUserProgress() {
        userProgress = new UserProgress();
        userProgress.completedTopics = Arrays.asList("data_types", "variables");
        userProgress.currentLevel = "beginner";
        userProgress.weakAreas = Arrays.asList("loops", "functions");
        userProgress.strongAreas = Arrays.asList("data_types");
        Log.d(TAG, "User progress loaded: " + userProgress.currentLevel);
    }

    private void generateDailyChallenges() {
        showLoading(true);
        dailyChallenges = new ArrayList<>();
        challengesGenerated = 0;

        // Update loading text
        TextView loadingText = findViewById(R.id.loading_text);
        if (loadingText != null) {
            loadingText.setText("Generating personalized challenges...");
        }

        Log.d(TAG, "Starting challenge generation with Gemini API");

        // Generate challenges sequentially
        generateNextChallenge();
    }

    private void generateNextChallenge() {
        if (challengesGenerated >= TOTAL_CHALLENGES) {
            Log.d(TAG, "All challenges generated successfully");
            showLoading(false);
            displayCurrentChallenge();
            return;
        }

        ChallengeType[] challengeTypes = {
                ChallengeType.MULTIPLE_CHOICE,
                ChallengeType.TRUE_FALSE,
                ChallengeType.CODE_SNIPPET,
                ChallengeType.FILL_BLANK,
                ChallengeType.CODE_OUTPUT
        };

        ChallengeType currentType = challengeTypes[challengesGenerated];
        String topic = getPersonalizedTopic();

        Log.d(TAG, "Generating challenge " + (challengesGenerated + 1) + "/" + TOTAL_CHALLENGES +
                " - Type: " + currentType + ", Topic: " + topic);

        String prompt = buildPromptForChallengeType(currentType, topic);

        generateChallengeWithGemini(prompt, currentType, () -> {
            challengesGenerated++;
            generateNextChallenge();
        });
    }

    private String buildPromptForChallengeType(ChallengeType type, String topic) {
        String[] weakAreasArray = userProgress.weakAreas.toArray(new String[0]);

        switch (type) {
            case MULTIPLE_CHOICE:
                return GeminiAPIHelper.PromptBuilder.buildMultipleChoicePrompt(topic, userProgress.currentLevel, weakAreasArray);
            case TRUE_FALSE:
                return GeminiAPIHelper.PromptBuilder.buildTrueFalsePrompt(topic, userProgress.currentLevel);
            case CODE_SNIPPET:
                return GeminiAPIHelper.PromptBuilder.buildCodeSnippetPrompt(topic, userProgress.currentLevel);
            case FILL_BLANK:
                return GeminiAPIHelper.PromptBuilder.buildFillBlankPrompt(topic, userProgress.currentLevel);
            case CODE_OUTPUT:
                return GeminiAPIHelper.PromptBuilder.buildCodeOutputPrompt(topic, userProgress.currentLevel);
            default:
                return GeminiAPIHelper.PromptBuilder.buildMultipleChoicePrompt(topic, userProgress.currentLevel, weakAreasArray);
        }
    }

    private void generateChallengeWithGemini(String prompt, ChallengeType type, Runnable onComplete) {
        Log.d(TAG, "Sending prompt to Gemini for " + type + ": " + prompt.substring(0, Math.min(100, prompt.length())) + "...");

        geminiAPIHelper.generatePythonChallenge(prompt, new GeminiAPIHelper.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Gemini API success for " + type + ". Response length: " + response.length());
                Log.d(TAG, "Raw Gemini response: " + response);

                runOnUiThread(() -> {
                    try {
                        PythonChallenge challenge = parseChallengeFromJSON(response, type);
                        dailyChallenges.add(challenge);
                        Log.d(TAG, "Successfully parsed and added " + type + " challenge");

                        // Update loading progress
                        updateLoadingProgress();

                        onComplete.run();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse Gemini response for " + type, e);
                        Log.e(TAG, "Problematic response: " + response);

                        // Use fallback and continue
                        PythonChallenge fallback = createFallbackChallenge(type);
                        dailyChallenges.add(fallback);
                        Log.d(TAG, "Added fallback challenge for " + type);

                        Toast.makeText(DailyChallengeActivity.this,
                                "Using backup question for " + type.name().replace("_", " "),
                                Toast.LENGTH_SHORT).show();

                        updateLoadingProgress();
                        onComplete.run();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Gemini API error for " + type + ": " + error);

                runOnUiThread(() -> {
                    // Use fallback and continue
                    PythonChallenge fallback = createFallbackChallenge(type);
                    dailyChallenges.add(fallback);
                    Log.d(TAG, "Added fallback challenge due to API error for " + type);

                    Toast.makeText(DailyChallengeActivity.this,
                            "Network issue - using backup question",
                            Toast.LENGTH_SHORT).show();

                    updateLoadingProgress();
                    onComplete.run();
                });
            }
        });
    }

    private void updateLoadingProgress() {
        TextView loadingText = findViewById(R.id.loading_text);
        if (loadingText != null) {
            loadingText.setText("Generated " + challengesGenerated + "/" + TOTAL_CHALLENGES + " challenges...");
        }

        if (loadingProgress != null) {
            loadingProgress.setProgress((challengesGenerated * 100) / TOTAL_CHALLENGES);
        }
    }

    private String getPersonalizedTopic() {
        List<String> availableTopics = new ArrayList<>(Arrays.asList(pythonTopics));

        // Prioritize weak areas
        if (!userProgress.weakAreas.isEmpty()) {
            Collections.shuffle(userProgress.weakAreas);
            String selectedTopic = userProgress.weakAreas.get(0);
            Log.d(TAG, "Selected weak area topic: " + selectedTopic);
            return selectedTopic;
        }

        // Remove completed topics
        availableTopics.removeAll(userProgress.completedTopics);

        if (availableTopics.isEmpty()) {
            availableTopics = Arrays.asList(pythonTopics);
        }

        Collections.shuffle(availableTopics);
        String selectedTopic = availableTopics.get(0);
        Log.d(TAG, "Selected random topic: " + selectedTopic);
        return selectedTopic;
    }

    private PythonChallenge parseChallengeFromJSON(String jsonResponse, ChallengeType type) {
        try {
            // Clean the response - remove any markdown formatting
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            Log.d(TAG, "Parsing JSON for " + type + ": " + cleanJson);

            JSONObject json = new JSONObject(cleanJson);
            PythonChallenge challenge = new PythonChallenge();

            challenge.type = type;
            challenge.question = json.optString("question", "Question not found");
            challenge.explanation = json.optString("explanation", "Explanation not provided");
            challenge.topic = json.optString("topic", "Python");
            challenge.difficulty = json.optString("difficulty", userProgress.currentLevel);
            challenge.points = json.optInt("points", 10);

            switch (type) {
                case MULTIPLE_CHOICE:
                case CODE_SNIPPET:
                case CODE_OUTPUT:
                    if (json.has("options")) {
                        JSONArray optionsArray = json.getJSONArray("options");
                        challenge.options = new ArrayList<>();
                        for (int i = 0; i < optionsArray.length(); i++) {
                            challenge.options.add(optionsArray.getString(i));
                        }
                    }
                    challenge.correctAnswerIndex = json.optInt("correct_answer", 0);
                    if (json.has("code_snippet")) {
                        challenge.codeSnippet = json.getString("code_snippet");
                    }
                    break;

                case TRUE_FALSE:
                    challenge.correctAnswerBoolean = json.optBoolean("correct_answer", true);
                    break;

                case FILL_BLANK:
                    challenge.correctAnswerText = json.optString("correct_answer", "answer");
                    if (json.has("code_with_blank")) {
                        challenge.codeWithBlank = json.getString("code_with_blank");
                    }
                    break;
            }

            Log.d(TAG, "Successfully parsed challenge: " + challenge.question);
            return challenge;

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error for " + type, e);
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }

    private PythonChallenge createFallbackChallenge(ChallengeType type) {
        PythonChallenge challenge = new PythonChallenge();
        challenge.type = type;
        challenge.topic = getPersonalizedTopic();
        challenge.difficulty = userProgress.currentLevel;

        switch (type) {
            case MULTIPLE_CHOICE:
                String[][] mcQuestions = {
                        {"What is the correct way to create a list in Python?", "[]", "{}", "()", "< >", "0", "Square brackets [] are used to create lists in Python", "10"},
                        {"Which method adds an item to the end of a list?", "append()", "add()", "insert()", "push()", "0", "append() method adds items to the end of a list", "10"},
                        {"What keyword defines a function in Python?", "def", "function", "func", "define", "0", "'def' keyword is used to define functions", "10"}
                };
                String[] mcQ = mcQuestions[new Random().nextInt(mcQuestions.length)];
                challenge.question = mcQ[0];
                challenge.options = Arrays.asList(mcQ[1], mcQ[2], mcQ[3], mcQ[4]);
                challenge.correctAnswerIndex = Integer.parseInt(mcQ[5]);
                challenge.explanation = mcQ[6];
                challenge.points = Integer.parseInt(mcQ[7]);
                break;

            case TRUE_FALSE:
                String[][] tfQuestions = {
                        {"Python is case-sensitive.", "true", "Python distinguishes between uppercase and lowercase letters", "5"},
                        {"Lists in Python are mutable.", "true", "Lists can be modified after creation", "5"},
                        {"Python requires semicolons to end statements.", "false", "Python does not require semicolons", "5"}
                };
                String[] tfQ = tfQuestions[new Random().nextInt(tfQuestions.length)];
                challenge.question = tfQ[0];
                challenge.correctAnswerBoolean = Boolean.parseBoolean(tfQ[1]);
                challenge.explanation = tfQ[2];
                challenge.points = Integer.parseInt(tfQ[3]);
                break;

            case CODE_SNIPPET:
                challenge.question = "Complete this function to print 'Hello World':";
                challenge.codeSnippet = "def greet():\n    print('___')";
                challenge.options = Arrays.asList("Hello World", "hello world", "HELLO WORLD", "Hello world");
                challenge.correctAnswerIndex = 0;
                challenge.explanation = "The exact string 'Hello World' should be printed";
                challenge.points = 15;
                break;

            case FILL_BLANK:
                challenge.question = "Complete the syntax to define a function:";
                challenge.codeWithBlank = "___ my_function():\n    pass";
                challenge.correctAnswerText = "def";
                challenge.explanation = "'def' keyword is used to define functions in Python";
                challenge.points = 12;
                break;

            case CODE_OUTPUT:
                challenge.question = "What will this code output?";
                challenge.codeSnippet = "x = [1, 2, 3]\ny = x\ny.append(4)\nprint(len(x))";
                challenge.options = Arrays.asList("3", "4", "7", "Error");
                challenge.correctAnswerIndex = 1;
                challenge.explanation = "y and x reference the same list object, so modifying y affects x";
                challenge.points = 20;
                break;
        }

        Log.d(TAG, "Created fallback challenge for " + type);
        return challenge;
    }

    private void displayCurrentChallenge() {
        if (currentChallengeIndex >= dailyChallenges.size()) {
            showCompletionMessage();
            return;
        }

        PythonChallenge current = dailyChallenges.get(currentChallengeIndex);
        Log.d(TAG, "Displaying challenge " + (currentChallengeIndex + 1) + ": " + current.question);

        // Update header information
        challengeTitle.setText("Challenge " + (currentChallengeIndex + 1));
        challengeDescription.setText(current.question);
        challengeTopic.setText("Topic: " + current.topic);
        challengeDifficulty.setText("Level: " + current.difficulty);
        challengeType.setText("Type: " + current.type.toString().replace("_", " "));

        // Hide all challenge containers first
        hideAllChallengeContainers();

        // Show appropriate challenge container and setup
        switch (current.type) {
            case MULTIPLE_CHOICE:
                setupMultipleChoiceChallenge(current);
                break;
            case TRUE_FALSE:
                setupTrueFalseChallenge(current);
                break;
            case CODE_SNIPPET:
                setupCodeSnippetChallenge(current);
                break;
            case FILL_BLANK:
                setupFillBlankChallenge(current);
                break;
            case CODE_OUTPUT:
                setupCodeOutputChallenge(current);
                break;
        }

        updateProgressDisplay();
        updateScore();
        submitAnswerBtn.setVisibility(View.VISIBLE);
        nextChallengeBtn.setVisibility(View.GONE);
    }

    private void hideAllChallengeContainers() {
        multipleChoiceContainer.setVisibility(View.GONE);
        trueFalseContainer.setVisibility(View.GONE);
        fillBlankContainer.setVisibility(View.GONE);
        codeChallenge.setVisibility(View.GONE);
    }

    private void setupMultipleChoiceChallenge(PythonChallenge challenge) {
        multipleChoiceContainer.setVisibility(View.VISIBLE);
        multipleChoiceOptions.removeAllViews(); // This is correct now, as the XML RadioGroup is empty

        if (challenge.options != null) {
            for (int i = 0; i < challenge.options.size(); i++) {
                // Dynamically create the MaterialCardView for each option
                MaterialCardView cardView = new MaterialCardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.card_margin_bottom)); // Define a dimen for margin
                cardView.setLayoutParams(cardParams);
                cardView.setRadius(getResources().getDimension(R.dimen.card_corner_radius)); // Define dimen for radius
                cardView.setCardElevation(0f); // Or use your desired elevation
                cardView.setStrokeColor(getResources().getColor(R.color.radio_stroke_color)); // Use your color resource
                cardView.setStrokeWidth((int) getResources().getDimension(R.dimen.card_stroke_width)); // Define dimen for stroke width

                // Create the inner LinearLayout
                LinearLayout innerLayout = new LinearLayout(this);
                innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                innerLayout.setOrientation(LinearLayout.HORIZONTAL);
                innerLayout.setPadding(
                        (int) getResources().getDimension(R.dimen.card_padding),
                        (int) getResources().getDimension(R.dimen.card_padding),
                        (int) getResources().getDimension(R.dimen.card_padding),
                        (int) getResources().getDimension(R.dimen.card_padding)
                ); // Define a dimen for padding

                // Create the MaterialRadioButton
                com.google.android.material.radiobutton.MaterialRadioButton radioButton = new com.google.android.material.radiobutton.MaterialRadioButton(this);
                radioButton.setId(View.generateViewId()); // Generate a unique ID
                radioButton.setFocusable(false);
                radioButton.setClickable(false); // Make the card clickable, not the radio button directly
                // Set tint using a ColorStateList to handle checked/unchecked states
                radioButton.setButtonTintList(ContextCompat.getColorStateList(this, R.color.radio_button_color));


                // Create the TextView for the option text
                TextView optionTextView = new TextView(this);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f // Weight for filling space
                );
                textParams.leftMargin = (int) getResources().getDimension(R.dimen.text_margin_left); // Define dimen for margin
                optionTextView.setLayoutParams(textParams);
                optionTextView.setText(challenge.options.get(i));
                optionTextView.setTextSize(14); // Use sp unit for text size
                optionTextView.setTextColor(getResources().getColor(R.color.text_primary));
                optionTextView.setTypeface(Typeface.MONOSPACE); // Keep monospace for code options
                optionTextView.setLineSpacing(0f, 1.2f); // lineSpacingExtra in XML is equivalent to this factor

                // Add radio button and text view to inner layout
                innerLayout.addView(radioButton);
                innerLayout.addView(optionTextView);

                // Add inner layout to card view
                cardView.addView(innerLayout);

                // Add the whole card view to the RadioGroup
                multipleChoiceOptions.addView(cardView);

                // Make the card clickable and link it to the radio button
                final int currentOptionIndex = i;
                cardView.setOnClickListener(v -> {
                    // This finds the radio button inside the clicked card
                    ((RadioGroup) cardView.getParent()).check(radioButton.getId());
                });
            }
        }
    }

    private void setupTrueFalseChallenge(PythonChallenge challenge) {
        trueFalseContainer.setVisibility(View.VISIBLE);
        trueButton.setSelected(false);
        falseButton.setSelected(false);
    }

    private void setupCodeSnippetChallenge(PythonChallenge challenge) {
        multipleChoiceContainer.setVisibility(View.VISIBLE);
        multipleChoiceOptions.removeAllViews();

        // Add code snippet display if available
        if (challenge.codeSnippet != null && !challenge.codeSnippet.isEmpty()) {
            TextView codeView = new TextView(this);
            codeView.setText(challenge.codeSnippet);
            codeView.setTypeface(Typeface.MONOSPACE);
            codeView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            codeView.setTextColor(getResources().getColor(android.R.color.white));
            codeView.setPadding(16, 16, 16, 16);
            codeView.setTextSize(14);

            LinearLayout parent = (LinearLayout) multipleChoiceContainer.getParent();
            int index = parent.indexOfChild(multipleChoiceContainer);
            parent.addView(codeView, index);
        }

        // Add options
        if (challenge.options != null) {
            for (int i = 0; i < challenge.options.size(); i++) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setText(challenge.options.get(i));
                radioButton.setId(View.generateViewId());
                radioButton.setTextSize(16);
                radioButton.setPadding(16, 12, 16, 12);
                multipleChoiceOptions.addView(radioButton);
            }
        }
    }

    private void setupCodeOutputChallenge(PythonChallenge challenge) {
        setupCodeSnippetChallenge(challenge);
    }

    private void setupFillBlankChallenge(PythonChallenge challenge) {
        fillBlankContainer.setVisibility(View.VISIBLE);
        fillBlankInput.setText("");

        // Show code with blank if available
        if (challenge.codeWithBlank != null && !challenge.codeWithBlank.isEmpty()) {
            challengeDescription.setText(challenge.codeWithBlank);
        }
    }

    private void submitCurrentAnswer() {
        if (currentChallengeIndex >= dailyChallenges.size()) return;

        PythonChallenge current = dailyChallenges.get(currentChallengeIndex);

        switch (current.type) {
            case MULTIPLE_CHOICE:
            case CODE_SNIPPET:
            case CODE_OUTPUT:
                checkMultipleChoice(multipleChoiceOptions, current.correctAnswerIndex, current.explanation, current.points);
                break;
            case TRUE_FALSE:
                checkTrueFalse(current);
                break;
            case FILL_BLANK:
                checkFillBlank(fillBlankInput, current);
                break;
        }
    }

    private void checkMultipleChoice(RadioGroup radioGroup, int correctAnswerIndex, String explanation, int points) {
        int selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        if (selectedRadioButtonId == -1) {
            Toast.makeText(this, "Please select an option.", Toast.LENGTH_SHORT).show();
            return;
        }

        View selectedView = radioGroup.findViewById(selectedRadioButtonId);
        int selectedIndex = radioGroup.indexOfChild(selectedView);

        if (selectedIndex == correctAnswerIndex) {
            showCorrectAnswer(points, explanation, true);
        } else {
            showWrongAnswer(explanation);
        }
    }

    private void checkTrueFalse(PythonChallenge challenge) {
        boolean userAnswer;
        if (trueButton.isSelected()) {
            userAnswer = true;
        } else if (falseButton.isSelected()) {
            userAnswer = false;
        } else {
            Toast.makeText(this, "Please select True or False.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userAnswer == challenge.correctAnswerBoolean) {
            showCorrectAnswer(challenge.points, challenge.explanation, true);
        } else {
            showWrongAnswer(challenge.explanation);
        }
    }

    private void checkFillBlank(TextInputEditText editText, PythonChallenge challenge) {
        String answer = editText.getText().toString().trim();
        String correct = challenge.correctAnswerText.trim();

        if (answer.equalsIgnoreCase(correct)) {
            showCorrectAnswer(challenge.points, challenge.explanation, true);
        } else {
            showWrongAnswer(challenge.explanation);
        }
    }

    private void showCorrectAnswer(int points, String explanation, boolean isCorrect) {
        totalScore += points;
        showExplanationDialog("Correct! +" + points + " points", explanation, true);
        nextChallengeBtn.setVisibility(View.VISIBLE);
        submitAnswerBtn.setVisibility(View.GONE);
        updateScore();
    }

    private void showWrongAnswer(String explanation) {
        showExplanationDialog("Wrong Answer", explanation, false);
        nextChallengeBtn.setVisibility(View.VISIBLE);
        submitAnswerBtn.setVisibility(View.GONE);
    }

    private void showExplanationDialog(String title, String explanation, boolean isCorrect) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(explanation)
                .setPositiveButton("Continue", null)
                .setIcon(isCorrect ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void nextChallenge() {
        currentChallengeIndex++;
        nextChallengeBtn.setVisibility(View.GONE);
        displayCurrentChallenge();
    }

    private void updateScore() {
        scoreText.setText("Score: " + totalScore);
    }

    private void updateProgressDisplay() {
        if (progressCard != null) {
            progressCard.setVisibility(View.VISIBLE);

            int totalChallenges = dailyChallenges.size();
            int completedChallenges = currentChallengeIndex + 1;
            int percentage = (completedChallenges * 100) / totalChallenges;

            progressText.setText("Challenge " + completedChallenges + " of " + totalChallenges);
            progressPercentage.setText(percentage + "%");
            challengeProgress.setProgress(completedChallenges);
            challengeProgress.setMax(totalChallenges);

            updateProgressSteps(completedChallenges, totalChallenges);
        }
    }

    private void updateProgressSteps(int completed, int total) {
        if (progressSteps != null) {
            for (int i = 0; i < progressSteps.getChildCount() && i < total; i++) {
                TextView step = (TextView) progressSteps.getChildAt(i);
                if (i < completed) {
                    step.setText("🟢");
                } else {
                    step.setText("⚪");
                }
            }
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingCard.setVisibility(View.VISIBLE);
            challengeContentCard.setVisibility(View.GONE);
            progressCard.setVisibility(View.GONE);
            submitAnswerBtn.setVisibility(View.GONE);
            nextChallengeBtn.setVisibility(View.GONE);
        } else {
            loadingCard.setVisibility(View.GONE);
            challengeContentCard.setVisibility(View.VISIBLE);
        }
    }

    private void showCompletionMessage() {
        challengeTitle.setText("Daily Python Challenge Complete!");
        challengeDescription.setText("Excellent work! You've completed today's personalized challenges.");

        hideAllChallengeContainers();
        updateUserProgress();

        LinearLayout completionLayout = new LinearLayout(this);
        completionLayout.setOrientation(LinearLayout.VERTICAL);
        completionLayout.setGravity(Gravity.CENTER);
        completionLayout.setPadding(32, 32, 32, 32);

        TextView finalScore = new TextView(this);
        finalScore.setText("🎉 Final Score: " + totalScore + " points 🎉");
        finalScore.setTextSize(20);
        finalScore.setGravity(Gravity.CENTER);
        finalScore.setTypeface(null, Typeface.BOLD);

        MaterialButton homeBtn = new MaterialButton(this);
        homeBtn.setText("🏠 Back to Learning");
        homeBtn.setOnClickListener(v -> finish());

        completionLayout.addView(finalScore);
        completionLayout.addView(homeBtn);

        LinearLayout parentLayout = (LinearLayout) challengeContentCard.getParent();
        if (parentLayout != null) {
            parentLayout.removeAllViews();
            parentLayout.addView(completionLayout);
        }

        nextChallengeBtn.setVisibility(View.GONE);
        submitAnswerBtn.setVisibility(View.GONE);
    }

    private void updateUserProgress() {
        // Save user progress to SharedPreferences or database
        Log.d(TAG, "Challenge completed with score: " + totalScore);
    }

    // Inner classes
    private static class PythonChallenge {
        ChallengeType type;
        String question;
        String explanation;
        String topic;
        String difficulty;
        int points;

        List<String> options;
        int correctAnswerIndex;
        String codeSnippet;

        boolean correctAnswerBoolean;

        String correctAnswerText;
        String codeWithBlank;
    }

    private static class UserProgress {
        List<String> completedTopics;
        String currentLevel;
        List<String> weakAreas;
        List<String> strongAreas;
    }

    private enum ChallengeType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        CODE_SNIPPET,
        FILL_BLANK,
        CODE_OUTPUT
    }
}