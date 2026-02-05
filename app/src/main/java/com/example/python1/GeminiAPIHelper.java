package com.example.python1;

import android.util.Log;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class GeminiAPIHelper {

    private static final String TAG = "GeminiAPIHelper";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent";
    private static final String API_KEY = "AIzaSyD6rJ68fDcmBX9hyTjka9tY4goq4snX1D4"; // Replace with your actual API key

    private OkHttpClient client;
    private static Random random = new Random();

    public GeminiAPIHelper() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public void generatePythonChallenge(String prompt, GeminiCallback callback) {
        Log.d(TAG, "Generating Python challenge with prompt: " + prompt);
        try {
            JSONObject requestBody = createRequestBody(prompt);
            Log.d(TAG, "Request Body: " + requestBody.toString());

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + "?key=" + API_KEY)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error during Gemini API call: " + e.getMessage(), e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Gemini API Response Code: " + response.code());
                    Log.d(TAG, "Gemini API Response Body: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            String extractedText = extractTextFromResponse(responseBody);
                            Log.d(TAG, "Extracted text from Gemini response: " + extractedText);
                            callback.onSuccess(extractedText);
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse Gemini response JSON: " + e.getMessage(), e);
                            callback.onError("Failed to parse response: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Gemini API returned an error: " + response.code() + " " + response.message() + "\nResponse Body: " + responseBody);
                        callback.onError("API error: " + response.code() + " " + response.message() + " - " + responseBody);
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Failed to create request JSON: " + e.getMessage(), e);
            callback.onError("Failed to create request: " + e.getMessage());
        }
    }

    private JSONObject createRequestBody(String prompt) throws JSONException {
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        // Add generation config for better and more varied responses
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.8); // Increased for more creativity
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 1024);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private String extractTextFromResponse(String responseBody) throws JSONException {
        JSONObject response = new JSONObject(responseBody);
        if (response.has("error")) {
            JSONObject error = response.getJSONObject("error");
            String errorMessage = error.optString("message", "Unknown API error");
            int errorCode = error.optInt("code", -1);
            throw new JSONException("Gemini API error (code: " + errorCode + "): " + errorMessage);
        }

        JSONArray candidates = response.getJSONArray("candidates");

        if (candidates.length() > 0) {
            JSONObject candidate = candidates.getJSONObject(0);
            if (candidate.has("safetyRatings")) {
                JSONArray safetyRatings = candidate.getJSONArray("safetyRatings");
                Log.w(TAG, "Gemini response includes safety ratings: " + safetyRatings.toString());
            }
            if (candidate.has("finishReason")) {
                String finishReason = candidate.getString("finishReason");
                if ("SAFETY".equals(finishReason) || "BLOCKLIST".equals(finishReason)) {
                    throw new JSONException("Gemini API blocked content due to safety concerns or blocklist.");
                }
            }

            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");

            if (parts.length() > 0) {
                JSONObject part = parts.getJSONObject(0);
                return part.getString("text");
            }
        }

        throw new JSONException("No valid content 'parts' found in Gemini response");
    }

    public static class PromptBuilder {

        // Arrays for adding variety to questions
        private static final String[] VARIABLE_NAMES = {"data", "items", "values", "numbers", "elements", "collection", "arr", "lst"};
        private static final String[] FUNCTION_NAMES = {"calculate", "process", "handle", "manage", "compute", "analyze"};
        private static final String[] CONTEXTS = {"student grades", "product prices", "temperatures", "scores", "ages", "distances"};

        public static String buildMultipleChoicePrompt(String topic, String level, String[] userWeakAreas) {
            String weakAreasContext = "";
            if (userWeakAreas != null && userWeakAreas.length > 0) {
                weakAreasContext = " Focus on these areas where the user needs improvement: " +
                        String.join(", ", userWeakAreas) + ".";
            }

            // Add randomization elements
            String randomSeed = "Random seed: " + System.currentTimeMillis() + ". ";
            String varietyPrompt = "Create a UNIQUE and DIFFERENT question than previous ones. Use creative examples and varied scenarios. ";
            String randomContext = "Use context related to: " + getRandomElement(CONTEXTS) + ". ";

            return String.format(
                    "%s%s%s%sCreate a Python multiple choice question about '%s' for %s level learners.%s " +
                            "Return ONLY a valid JSON object with this exact structure:\n" +
                            "{\n" +
                            "  \"question\": \"[Your creative question here]\",\n" +
                            "  \"options\": [\"option1\", \"option2\", \"option3\", \"option4\"],\n" +
                            "  \"correct_answer\": [0-3],\n" +
                            "  \"explanation\": \"[Clear explanation]\",\n" +
                            "  \"topic\": \"%s\",\n" +
                            "  \"difficulty\": \"%s\",\n" +
                            "  \"points\": %d\n" +
                            "}\n" +
                            "Make sure the question tests practical Python knowledge and avoid repeating similar patterns.",
                    randomSeed, varietyPrompt, randomContext, "",
                    topic, level, weakAreasContext, topic, level, getRandomPoints(5, 15)
            );
        }

        public static String buildCodeSnippetPrompt(String topic, String level) {
            String randomSeed = "Random seed: " + System.currentTimeMillis() + ". ";
            String varietyPrompt = "Create a UNIQUE code completion challenge. Use different variable names and scenarios. ";
            String randomVar = "Use variable name: " + getRandomElement(VARIABLE_NAMES) + ". ";
            String randomFunc = "Use function name containing: " + getRandomElement(FUNCTION_NAMES) + ". ";

            return String.format(
                    "%s%s%s%sCreate a Python code completion challenge about '%s' for %s level. " +
                            "Show incomplete code with blanks to fill. Return ONLY valid JSON:\n" +
                            "{\n" +
                            "  \"question\": \"[Your creative question]\",\n" +
                            "  \"code_snippet\": \"[Your code with blanks marked as ___]\",\n" +
                            "  \"options\": [\"option1\", \"option2\", \"option3\", \"option4\"],\n" +
                            "  \"correct_answer\": [0-3],\n" +
                            "  \"explanation\": \"[Clear explanation]\",\n" +
                            "  \"topic\": \"%s\",\n" +
                            "  \"difficulty\": \"%s\",\n" +
                            "  \"points\": %d\n" +
                            "}",
                    randomSeed, varietyPrompt, randomVar, randomFunc,
                    topic, level, topic, level, getRandomPoints(10, 20)
            );
        }

        public static String buildCodeOutputPrompt(String topic, String level) {
            String randomSeed = "Random seed: " + System.currentTimeMillis() + ". ";
            String varietyPrompt = "Create a UNIQUE 'predict the output' question. Use different code patterns and examples. ";
            String randomVar = "Use variable: " + getRandomElement(VARIABLE_NAMES) + ". ";

            return String.format(
                    "%s%s%sCreate a 'predict the output' Python question about '%s' for %s level. " +
                            "Show working code and ask what it outputs. Return ONLY valid JSON:\n" +
                            "{\n" +
                            "  \"question\": \"What will this Python code output?\",\n" +
                            "  \"code_snippet\": \"[Your unique code example]\",\n" +
                            "  \"options\": [\"output1\", \"output2\", \"output3\", \"output4\"],\n" +
                            "  \"correct_answer\": [0-3],\n" +
                            "  \"explanation\": \"[Detailed explanation]\",\n" +
                            "  \"topic\": \"%s\",\n" +
                            "  \"difficulty\": \"%s\",\n" +
                            "  \"points\": %d\n" +
                            "}",
                    randomSeed, varietyPrompt, randomVar,
                    topic, level, topic, level, getRandomPoints(15, 25)
            );
        }

        public static String buildTrueFalsePrompt(String topic, String level) {
            String randomSeed = "Random seed: " + System.currentTimeMillis() + ". ";
            String varietyPrompt = "Create a UNIQUE true/false statement. Avoid common examples. ";

            return String.format(
                    "%s%sCreate a Python true/false question about '%s' for %s level. " +
                            "Return ONLY valid JSON:\n" +
                            "{\n" +
                            "  \"question\": \"[Your unique statement to evaluate]\",\n" +
                            "  \"correct_answer\": [true/false],\n" +
                            "  \"explanation\": \"[Clear explanation why it's true or false]\",\n" +
                            "  \"topic\": \"%s\",\n" +
                            "  \"difficulty\": \"%s\",\n" +
                            "  \"points\": %d\n" +
                            "}",
                    randomSeed, varietyPrompt,
                    topic, level, topic, level, getRandomPoints(5, 10)
            );
        }

        public static String buildFillBlankPrompt(String topic, String level) {
            String randomSeed = "Random seed: " + System.currentTimeMillis() + ". ";
            String varietyPrompt = "Create a UNIQUE fill-in-the-blank question. Use different syntax patterns. ";
            String randomVar = "Use variable: " + getRandomElement(VARIABLE_NAMES) + ". ";

            return String.format(
                    "%s%s%sCreate a fill-in-the-blank Python question about '%s' for %s level. " +
                            "Return ONLY valid JSON:\n" +
                            "{\n" +
                            "  \"question\": \"Complete the Python syntax:\",\n" +
                            "  \"code_with_blank\": \"[Your code with ___ for the blank]\",\n" +
                            "  \"correct_answer\": \"[correct word/phrase]\",\n" +
                            "  \"explanation\": \"[Explanation of the answer]\",\n" +
                            "  \"topic\": \"%s\",\n" +
                            "  \"difficulty\": \"%s\",\n" +
                            "  \"points\": %d\n" +
                            "}",
                    randomSeed, varietyPrompt, randomVar,
                    topic, level, topic, level, getRandomPoints(8, 15)
            );
        }

        // Helper methods for randomization
        private static String getRandomElement(String[] array) {
            return array[random.nextInt(array.length)];
        }

        private static int getRandomPoints(int min, int max) {
            return min + random.nextInt(max - min + 1);
        }
    }
}