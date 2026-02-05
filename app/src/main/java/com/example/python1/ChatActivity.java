package com.example.python1;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final String API_KEY = "AIzaSyD6rJ68fDcmBX9hyTjka9tY4goq4snX1D4"; // REPLACE WITH YOUR ACTUAL KEY

    private RecyclerView recyclerViewChatMessages;
    private EditText editMessage;
    private FloatingActionButton btnSend;
    private LinearLayout typingIndicatorLayout;
    private ImageView typingAnimationImageView;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private GenerativeModelFutures model;
    private Executor backgroundExecutor;

    private String currentUserName = "You"; // Default name
    private String currentUserAvatarUrl = null; // Default avatar URL

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // --- Firebase Initialization ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- UI Initialization ---
        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnSettings = findViewById(R.id.btn_settings);

        recyclerViewChatMessages = findViewById(R.id.recycler_chat_messages);
        editMessage = findViewById(R.id.edit_message);
        btnSend = findViewById(R.id.btn_send);
        typingIndicatorLayout = findViewById(R.id.typing_indicator);
        typingAnimationImageView = findViewById(R.id.typing_dots_animation);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChatMessages.setLayoutManager(layoutManager);
        recyclerViewChatMessages.setAdapter(messageAdapter);

        // --- Fetch User Profile Data ---
        fetchUserProfileData();

        // --- Header Button Listeners ---
        btnBack.setOnClickListener(v -> onBackPressed());

        btnSettings.setOnClickListener(v -> Toast.makeText(ChatActivity.this, "Settings clicked (Implement later)", Toast.LENGTH_SHORT).show());


        // --- Gemini API Setup ---
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash-latest", API_KEY);
        model = GenerativeModelFutures.from(gm);
        backgroundExecutor = Executors.newSingleThreadExecutor();

        // --- Send Button Listener ---
        btnSend.setOnClickListener(v -> sendMessage());

        // Optional: Send message on Enter key press in EditText
        editMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Optional: Add a welcoming message on startup
        messageList.add(new Message("Hello! I'm your AI Assistant. How can I help you today?", false));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerViewChatMessages.scrollToPosition(messageList.size() - 1);
    }

    private void fetchUserProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference docRef = db.collection("users").document(userId);
// Assuming "profile" is the document ID for profile details
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map<String, Object> profileDetails = (Map<String, Object>) document.get("Profile_details");
                        if (profileDetails != null) {
                            currentUserName = (String) profileDetails.get("name");
                            currentUserAvatarUrl = (String) profileDetails.get("profileImageUrl");

                            Log.d(TAG, "User profile fetched: Name = " + currentUserName + ", Avatar = " + currentUserAvatarUrl);
                        } else {
                            Log.d(TAG, "Profile_details map is null.");
                        }
                    } else {
                        Log.d(TAG, "No such document for user profile.");
                    }
                } else {
                    Log.d(TAG, "Failed to fetch user profile: ", task.getException());
                    Toast.makeText(ChatActivity.this, "Failed to load user profile.", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Log.d(TAG, "No current user logged in.");
            // Handle case where user is not logged in, maybe redirect to login activity
        }
    }

    private void sendMessage() {
        String userMessage = editMessage.getText().toString().trim();
        if (userMessage.isEmpty()) {
            Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user message with fetched profile data
        // Pass the fetched userName and avatarUrl to the Message constructor
        messageList.add(new Message(userMessage, true, currentUserAvatarUrl, currentUserName));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerViewChatMessages.scrollToPosition(messageList.size() - 1);
        editMessage.setText("");

        // Show typing indicator and start animation
        typingIndicatorLayout.setVisibility(View.VISIBLE);
        startTypingAnimation();
        recyclerViewChatMessages.scrollToPosition(messageList.size() - 1);

        // Create a Content object from the user message
        Content userContent = new Content.Builder()
                .addText(userMessage)
                .build();

        // Make API call on a background thread
        Futures.addCallback(
                model.generateContent(userContent),
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(@NonNull GenerateContentResponse result) {
                        runOnUiThread(() -> {
                            // Hide typing indicator and stop animation
                            typingIndicatorLayout.setVisibility(View.GONE);
                            stopTypingAnimation();

                            String aiResponse = result.getText();
                            if (aiResponse == null || aiResponse.isEmpty()) {
                                aiResponse = "I'm sorry, I couldn't generate a response.";
                            }
                            messageList.add(new Message(aiResponse, false));
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            recyclerViewChatMessages.scrollToPosition(messageList.size() - 1);
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        runOnUiThread(() -> {
                            // Hide typing indicator and stop animation
                            typingIndicatorLayout.setVisibility(View.GONE);
                            stopTypingAnimation();

                            Log.e(TAG, "API call failed", t);
                            String errorMessage = "Error: Could not get response from AI. " + t.getLocalizedMessage();
                            if (t.getMessage() != null && t.getMessage().contains("403 Forbidden")) {
                                errorMessage = "Error: Invalid API Key or API not enabled. Please check your key.";
                            } else if (t.getMessage() != null && t.getMessage().contains("rate limit")) {
                                errorMessage = "Error: Rate limit exceeded. Please wait a moment and try again.";
                            }
                            messageList.add(new Message(errorMessage, false));
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            recyclerViewChatMessages.scrollToPosition(messageList.size() - 1);
                            Toast.makeText(ChatActivity.this, "Error communicating with AI.", Toast.LENGTH_LONG).show();
                        });
                    }
                },
                backgroundExecutor
        );
    }

    private void startTypingAnimation() {
        Drawable drawable = typingAnimationImageView.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
    }

    private void stopTypingAnimation() {
        Drawable drawable = typingAnimationImageView.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).stop();
        }
    }
}