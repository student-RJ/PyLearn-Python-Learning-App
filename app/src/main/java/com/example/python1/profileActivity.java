package com.example.python1;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class profileActivity extends BaseActivity {
    private static final String COUNTRY_CODE = "+91";
    private static final int MOBILE_NUMBER_LENGTH = 10; // This is for the digits AFTER the country code

    private EditText nameEditText, emailEditText, dobEditText, addressEditText;
    private TextInputEditText mobileEditText; // Changed to TextInputEditText for consistency if you prefer

    private MaterialButtonToggleGroup genderToggleGroup;
    private Button updateProfileButton;
    private ImageView profileImageView, editProfileImageView;

    private static final int PICK_IMAGE_REQUEST = 1;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TextInputEditText etInstagram, etFacebook, etTwitter;
    private Cloudinary cloudinary;

    private ProgressBar userProgressBar;
    private TextView progressPercentage;

    private boolean isLoadingProfile = false; // Flag to prevent TextWatcher from re-triggering during load

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize Cloudinary
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dw02yjba1",
                "api_key", "521444467154148",
                "api_secret", "goAsI8uGoS2zK1Jc65qrD0YXtzc"
        ));

        // Bind views
        nameEditText = findViewById(R.id.name_edit_text);
        mobileEditText = findViewById(R.id.mobile_edit_text); // Make sure this is linked to TextInputEditText in XML if changed
        emailEditText = findViewById(R.id.email_edit_text);
        dobEditText = findViewById(R.id.dob_edit_text);
        addressEditText = findViewById(R.id.address_edit_text);

        MaterialButton maleRadio = findViewById(R.id.male_radio); // This line can be removed if not directly used
        updateProfileButton = findViewById(R.id.update_profile_button);
        profileImageView = findViewById(R.id.profile_image);
        MaterialButton backButton = findViewById(R.id.back_button);
        editProfileImageView = findViewById(R.id.edit_profile_pic);
        genderToggleGroup = findViewById(R.id.gender_radio_group);

        etInstagram = findViewById(R.id.instagram_username_edit_text);
        etFacebook = findViewById(R.id.facebook_username_edit_text);
        etTwitter = findViewById(R.id.twitter_handle_edit_text);

        // Progress Section
        userProgressBar = findViewById(R.id.user_progress_bar);
        progressPercentage = findViewById(R.id.progress_percentage);

        // Mobile number setup
        mobileEditText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            // Removed lastValid as it might cause issues if text is cleared unexpectedly
            // during the loading phase.

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                // Prevent re-entry if already formatting or loading profile
                if (isFormatting || isLoadingProfile) return;
                isFormatting = true;

                String input = s.toString();

                // If the input is empty, reset to country code and space
                if (input.isEmpty()) {
                    mobileEditText.setText(COUNTRY_CODE + " ");
                    safeSetSelection(mobileEditText, mobileEditText.getText().length());
                    isFormatting = false;
                    return;
                }

                // Ensure input always starts with country code + space
                if (!input.startsWith(COUNTRY_CODE + " ")) {
                    // Prepend if missing, and adjust selection
                    String digits = input.replaceAll("[^0-9]", "");
                    String newText = COUNTRY_CODE + " " + digits;
                    mobileEditText.setText(newText);
                    safeSetSelection(mobileEditText, newText.length());
                    isFormatting = false;
                    return;
                }

                // Get only digits after country code + space
                String currentDigits = input.substring((COUNTRY_CODE + " ").length());
                String digits = currentDigits.replaceAll("[^0-9]", "");

                // Limit digits to max 10
                if (digits.length() > MOBILE_NUMBER_LENGTH) {
                    digits = digits.substring(0, MOBILE_NUMBER_LENGTH);
                }

                // Construct new formatted text: country code + space + digits
                String newText = COUNTRY_CODE + " " + digits;

                // Only update if the formatted text is different to avoid infinite loop
                if (!newText.equals(input)) {
                    mobileEditText.setText(newText);
                }

                // Set cursor at the end
                safeSetSelection(mobileEditText, newText.length());

                isFormatting = false;
            }
        });
        // Email setup with auto-suggestion
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && emailEditText.getText().toString().isEmpty()) {
                emailEditText.setHint("example@gmail.com");
            }
        });

        emailEditText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;

                String email = s.toString();
                if (!email.contains("@") && !email.isEmpty()) {
                    isEditing = true;
                    emailEditText.setText(email + "@gmail.com");
                    safeSetSelection(emailEditText, email.length()); // Cursor before @gmail.com
                    isEditing = false;
                }
            }
        });

        dobEditText.setOnClickListener(v -> showDatePicker());
        updateProfileButton.setOnClickListener(v -> updateProfile());
        backButton.setOnClickListener(v -> finish());
        editProfileImageView.setOnClickListener(v -> openImagePicker());

        loadUserProfile(); // Initial load of profile data and image
        calculateProfileCompletion(); // Initial calculation
    }

    // Helper method to safely set selection
    private void safeSetSelection(EditText editText, int position) {
        try {
            if (position >= 0 && position <= editText.getText().length()) {
                editText.setSelection(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private int calculateProfileCompletion() {
        int totalFields = 9;
        int completedFields = 0;

        if (!isEmpty(nameEditText)) completedFields++;

        String mobileText = mobileEditText.getText().toString().trim();
        String mobileDigitsOnly = mobileText.replace(COUNTRY_CODE, "").trim().replaceAll("[^0-9]", "");
        if (mobileDigitsOnly.length() == MOBILE_NUMBER_LENGTH) {
            completedFields++;
        }

        if (!isEmpty(emailEditText)) completedFields++;

        if (genderToggleGroup.getCheckedButtonId() != -1) {
            completedFields++;
        }

        if (!isEmpty(dobEditText)) completedFields++;
        if (!isEmpty(addressEditText)) completedFields++;
        if (!isEmpty(etInstagram)) completedFields++;
        if (!isEmpty(etFacebook)) completedFields++;
        if (!isEmpty(etTwitter)) completedFields++;

        int completionPercent = (completedFields * 100) / totalFields;

        userProgressBar.setProgress(completionPercent);
        progressPercentage.setText(completionPercent + "% Completed");

        return completionPercent;
    }

    private boolean isEmpty(EditText et) {
        return et.getText() == null || et.getText().toString().trim().isEmpty();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year1, month1, day1) ->
                dobEditText.setText(year1 + "-" + (month1 + 1) + "-" + day1),
                year, month, day).show();
    }

    private void loadUserProfile() {
        if (currentUser != null) {
            isLoadingProfile = true; // Set flag to true before loading
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Map<String, Object> profileDetails = (Map<String, Object>) doc.get("Profile_details");
                            if (profileDetails != null) {
                                nameEditText.setText((String) profileDetails.get("name"));
                                dobEditText.setText((String) profileDetails.get("dob"));
                                addressEditText.setText((String) profileDetails.get("address"));
                                emailEditText.setText((String) profileDetails.get("email"));

                                String mobile = (String) profileDetails.get("mobile");
                                if (mobile != null && !mobile.trim().isEmpty()) {
                                    // Load mobile number directly as it's stored
                                    // Ensure it's formatted as +91 XXXXXXXXXX (or whatever format your TextWatcher expects)
                                    // Assuming Firestore stores it as "+91 XXXXXXXXXX" or just "XXXXXXXXXX"
                                    // If stored as "XXXXXXXXXX" (10 digits), prepend "+91 "
                                    if (mobile.length() == MOBILE_NUMBER_LENGTH && !mobile.startsWith(COUNTRY_CODE)) {
                                        mobileEditText.setText(COUNTRY_CODE + " " + mobile);
                                    } else {
                                        mobileEditText.setText(mobile);
                                    }
                                    safeSetSelection(mobileEditText, mobileEditText.getText().length());
                                } else {
                                    // If mobile is null or empty in Firestore, initialize with country code
                                    mobileEditText.setText(COUNTRY_CODE + " ");
                                    safeSetSelection(mobileEditText, mobileEditText.getText().length());
                                }

                                String instagramUrl = (String) profileDetails.get("instagramUrl");
                                String facebookUrl = (String) profileDetails.get("facebookUrl");
                                String twitterUrl = (String) profileDetails.get("twitterUrl");

                                if (instagramUrl != null && !instagramUrl.isEmpty()) {
                                    etInstagram.setText(extractUsernameFromUrl(instagramUrl));
                                }
                                if (facebookUrl != null && !facebookUrl.isEmpty()) {
                                    etFacebook.setText(extractUsernameFromUrl(facebookUrl));
                                }
                                if (twitterUrl != null && !twitterUrl.isEmpty()) {
                                    etTwitter.setText(extractUsernameFromUrl(twitterUrl));
                                }

                                String gender = (String) profileDetails.get("gender");
                                if (gender != null) {
                                    switch (gender) {
                                        case "Male":
                                            genderToggleGroup.check(R.id.male_radio);
                                            break;
                                        case "Female":
                                            genderToggleGroup.check(R.id.female_radio);
                                            break;
                                        case "Other":
                                            genderToggleGroup.check(R.id.other_radio);
                                            break;
                                        default:
                                            genderToggleGroup.clearChecked();
                                            break;
                                    }
                                } else {
                                    genderToggleGroup.clearChecked();
                                }

                                String profileImageUrl = (String) profileDetails.get("profileImageUrl");
                                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                    Glide.with(this)
                                            .load(profileImageUrl)
                                            .placeholder(R.drawable.profile)
                                            .error(R.drawable.profile)
                                            .circleCrop()
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .skipMemoryCache(true)
                                            .into(profileImageView);
                                } else {
                                    profileImageView.setImageResource(R.drawable.profile);
                                }

                                calculateProfileCompletion();
                            } else {
                                Toast.makeText(this, "Profile details not found in Firestore for user.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "User document not found in Firestore.", Toast.LENGTH_SHORT).show();
                        }
                        isLoadingProfile = false; // Set flag to false after loading
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isLoadingProfile = false; // Set flag to false on failure too
                    });
        }
    }

    private String extractUsernameFromUrl(String url) {
        if (url != null && !url.isEmpty()) {
            Uri uri = Uri.parse(url);
            String path = uri.getLastPathSegment();
            if (path != null && !path.isEmpty()) {
                return path;
            }
        }
        return "";
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageToCloudinary(imageUri);
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        runOnUiThread(() -> {
            showProgressDialog("Uploading image...");
        });

        new Thread(() -> {
            Bitmap bitmap = null;
            InputStream imageStream = null;
            InputStream exifStream = null;

            try {
                imageStream = getContentResolver().openInputStream(imageUri);
                bitmap = android.graphics.BitmapFactory.decodeStream(imageStream);
                if (imageStream != null) imageStream.close();

                int orientation = ExifInterface.ORIENTATION_UNDEFINED;
                exifStream = getContentResolver().openInputStream(imageUri);
                if (exifStream != null) {
                    ExifInterface exifInterface = new ExifInterface(exifStream);
                    orientation = exifInterface.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED
                    );
                    if (exifStream != null) exifStream.close();
                }

                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix.preScale(-1.0f, 1.0f);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix.preScale(1.0f, -1.0f);
                        break;
                }

                if (!matrix.isIdentity() && bitmap != null) {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (bitmap != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                }
                byte[] byteArray = stream.toByteArray();

                Map uploadResult = cloudinary.uploader().upload(byteArray, ObjectUtils.asMap(
                        "folder", "Profile Image",
                        "quality", "auto",
                        "angle", "auto_rotate"
                ));

                String uploadedUrl = (String) uploadResult.get("secure_url");

                runOnUiThread(() -> {
                    dismissProgressDialog();
                    if (currentUser != null) {
                        db.collection("users").document(currentUser.getUid())
                                .update("Profile_details.profileImageUrl", uploadedUrl)
                                .addOnSuccessListener(aVoid -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        Glide.with(profileActivity.this)
                                                .load(uploadedUrl)
                                                .placeholder(R.drawable.profile)
                                                .error(R.drawable.profile)
                                                .circleCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .into(profileImageView);
                                    }
                                    Toast.makeText(profileActivity.this, "Profile image updated!", Toast.LENGTH_SHORT).show();
                                    calculateProfileCompletion();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(profileActivity.this, "Failed to update image URL in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgressDialog();
                    Toast.makeText(profileActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                try {
                    if (imageStream != null) imageStream.close();
                    if (exifStream != null) exifStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Removed the unused loadProfileImage method using Picasso.
    private void updateProfile() {
        String name = nameEditText.getText().toString().trim();
        String mobile = mobileEditText.getText().toString().replaceAll("\\s+", "");
        String email = emailEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        int genderId = genderToggleGroup.getCheckedButtonId();

        String instagramUsername = etInstagram.getText().toString().trim();
        String facebookUsername = etFacebook.getText().toString().trim();
        String twitterHandle = etTwitter.getText().toString().trim();

        // Validate required fields
        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }
        // Mobile validation (10 digits after +91)
        String mobileDigitsOnly = mobile.replace(COUNTRY_CODE, "").trim().replaceAll("[^0-9]", "");
        if (mobileDigitsOnly.length() != MOBILE_NUMBER_LENGTH) {
            mobileEditText.setError("Please enter a valid 10-digit mobile number");
            mobileEditText.requestFocus();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        if (dob.isEmpty()) {
            dobEditText.setError("Date of birth is required");
            dobEditText.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            addressEditText.setError("Address is required");
            addressEditText.requestFocus();
            return;
        }

        if (genderId == -1) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }

        String instagramUrl = instagramUsername.isEmpty() ? "" :
                instagramUsername.startsWith("http") ? instagramUsername :
                        "https://www.instagram.com/" + instagramUsername;

        String facebookUrl = facebookUsername.isEmpty() ? "" :
                facebookUsername.startsWith("http") ? facebookUsername :
                        "https://www.facebook.com/" + facebookUsername;

        String twitterUrl = twitterHandle.isEmpty() ? "" :
                twitterHandle.startsWith("http") ? twitterHandle :
                        "https://twitter.com/" + twitterHandle;

        MaterialButton selectedGender = findViewById(genderId);
        String gender = selectedGender.getText().toString();

        int completionPercent = calculateProfileCompletion();

        Map<String, Object> profileDetails = new HashMap<>();
        profileDetails.put("name", name);
        // Store mobile number including the country code and space if that's your consistent format
        profileDetails.put("mobile", COUNTRY_CODE + " " + mobileDigitsOnly);
        profileDetails.put("email", email);
        profileDetails.put("dob", dob);
        profileDetails.put("address", address);
        profileDetails.put("gender", gender);
        profileDetails.put("instagramUrl", instagramUrl);
        profileDetails.put("facebookUrl", facebookUrl);
        profileDetails.put("twitterUrl", twitterUrl);
        profileDetails.put("profileCompletion", completionPercent);
        profileDetails.put("lastUpdated", FieldValue.serverTimestamp());

        if (currentUser != null) {
            showProgressDialog("Updating profile...");

            db.collection("users").document(currentUser.getUid())
                    .set(new HashMap<String, Object>() {{ put("Profile_details", profileDetails); }}, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        dismissProgressDialog();
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();
                        Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    // Helper methods for progress dialogs (assuming they are implemented in BaseActivity)
    private void showProgressDialog(String message) {
    }
    private void dismissProgressDialog() {
    }
}