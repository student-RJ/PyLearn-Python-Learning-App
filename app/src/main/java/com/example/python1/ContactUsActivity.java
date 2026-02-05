package com.example.python1;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ContactUsActivity extends BaseActivity {

    private LinearLayout emailLayout, callLayout, whatsappLayout,TelegramLayout;
    private ImageView backButton;
    private EditText subjectEditText, messageEditText;
    private Button attachButton, submitButton;
    private Uri screenshotUri = null;
    private ProgressDialog progressDialog;


    private static final int PICK_IMAGE_REQUEST = 1;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        // Initialize UI Components
        emailLayout = findViewById(R.id.email_support);
        callLayout = findViewById(R.id.call_support);
        whatsappLayout = findViewById(R.id.whatsapp_support);
        backButton = findViewById(R.id.back_button);
        TelegramLayout=findViewById(R.id.telegram_support);
        subjectEditText = findViewById(R.id.ticket_subject);
        messageEditText = findViewById(R.id.ticket_message);
        attachButton = findViewById(R.id.attach_screenshot_button);
        submitButton = findViewById(R.id.submit_ticket_button);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("support_screenshots");

        attachButton.setOnClickListener(v -> openFileChooser());

        submitButton.setOnClickListener(v -> submitSupportTicket());

        // Set Click Listeners
        emailLayout.setOnClickListener(v -> sendEmail());
        callLayout.setOnClickListener(v -> makePhoneCall());
        whatsappLayout.setOnClickListener(v -> openWhatsAppChat());
        backButton.setOnClickListener(v -> finish());

        TelegramLayout.setOnClickListener(view -> openTelegramChat());
        // FAQ Setup
        ExpandableListView faqListView = findViewById(R.id.faq_expandable_list);
        List<String> faqQuestions = new ArrayList<>();
        HashMap<String, List<String>> faqAnswers = new HashMap<>();

// Sample questions and answers
        // Sample questions and answers
        faqQuestions.add("How to reset my password?");
        faqQuestions.add("How to contact support?");
        faqQuestions.add("What is PyLearn App for?");
        faqQuestions.add("Can I use PyLearn offline?");
        faqQuestions.add("How to report a bug?");
        faqQuestions.add("Where can I find learning resources?");

        List<String> ans1 = new ArrayList<>();
        ans1.add("Go to login screen > Tap on 'Forgot Password' > Enter your email.");

        List<String> ans2 = new ArrayList<>();
        ans2.add("You can contact support via Email, WhatsApp, Telegram or Phone from this page.");

        List<String> ans3 = new ArrayList<>();
        ans3.add("PyLearn helps you learn Python programming through tutorials and quizzes.");

        List<String> ans4 = new ArrayList<>();
        ans4.add("Currently, PyLearn requires an internet connection to access tutorials and quizzes.");

        List<String> ans5 = new ArrayList<>();
        ans5.add("You can report bugs by contacting support through any of the available options.");

        List<String> ans6 = new ArrayList<>();
        ans6.add("Learning resources are available within the app under the Tutorials section.");

        faqAnswers.put(faqQuestions.get(0), ans1);
        faqAnswers.put(faqQuestions.get(1), ans2);
        faqAnswers.put(faqQuestions.get(2), ans3);
        faqAnswers.put(faqQuestions.get(3), ans4);
        faqAnswers.put(faqQuestions.get(4), ans5);
        faqAnswers.put(faqQuestions.get(5), ans6);


// Set Adapter
        faqListView.setAdapter(new FAQExpandableListAdapter(faqQuestions, faqAnswers));
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting ticket...");
        progressDialog.setCancelable(false);


    }
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            screenshotUri = data.getData();
            Toast.makeText(this, "Screenshot attached", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitSupportTicket() {
        String subject = subjectEditText.getText().toString().trim();
        String message = messageEditText.getText().toString().trim();

        if (subject.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show(); // Show loading

        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference ticketRef = db.collection("users").document(userId)
                .collection("support_tickets").document();

        Map<String, Object> ticketData = new HashMap<>();
        ticketData.put("subject", subject);
        ticketData.put("message", message);
        ticketData.put("timestamp", FieldValue.serverTimestamp());

        if (screenshotUri != null) {
            StorageReference fileRef = storageRef.child("ticket_screenshot/" + userId + "/" + ticketRef.getId() + ".jpg");

            fileRef.putFile(screenshotUri)
                    .addOnSuccessListener(taskSnapshot ->
                            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                ticketData.put("screenshot_url", uri.toString());
                                saveTicketToFirestore(ticketRef, ticketData);
                            })
                    )
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveTicketToFirestore(ticketRef, ticketData);
        }
    }



    private void saveTicketToFirestore(DocumentReference ticketRef, Map<String, Object> ticketData) {
        ticketRef.set(ticketData)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Support ticket submitted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to submit ticket", Toast.LENGTH_SHORT).show();
                });
    }


    // Function to send an email
    private void sendEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:sd08xsd11@gmail.com")); // Change to your support email
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Support Request");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello, I need help with...");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email apps found", Toast.LENGTH_SHORT).show();
        }
    }

    // Function to make a phone call
    private void makePhoneCall() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:+918308669670")); // Change to your support number
        startActivity(callIntent);
    }

    // Function to open WhatsApp chat
    private void openWhatsAppChat() {
        String phoneNumber = "+918308669670"; // Change to your WhatsApp number
        String message = "Hello, I need support.";

        try {
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse("https://wa.me/" + phoneNumber + "?text=" + Uri.encode(message)));
            startActivity(whatsappIntent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }
    private void openTelegramChat() {
        String telegramUsername = "RohanJadhav667"; // Change to your Telegram username or channel

        try {
            Intent telegramIntent = new Intent(Intent.ACTION_VIEW);
            telegramIntent.setData(Uri.parse("https://t.me/" + telegramUsername));
            startActivity(telegramIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Telegram app not found", Toast.LENGTH_SHORT).show();
        }
    }
}
