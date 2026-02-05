 package com.example.python1;

public class UserProfile {

    private String name;
    private String mobile;
    private String email;
    private String address;
    private String dob;
    private String gender;
    private String profilePicture;
    private int progress;

    // Default constructor (required for Firestore)
    public UserProfile() {}

    // Constructor with parameters
    public UserProfile(String name, String mobile, String email, String address, String dob, String gender, String profilePicture, int progress) {
        this.name = name;
        this.mobile = mobile;
        this.email = email;
        this.address = address;
        this.dob = dob;
        this.gender = gender;
        this.profilePicture = profilePicture;
        this.progress = progress;
    }

    // Getters and setters for all fields

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
    // Optional: Override toString() for easier logging/debugging
    @Override
    public String toString() {
        return "UserProfile{" +
                "name='" + name + '\'' +
                ", mobile='" + mobile + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", dob='" + dob + '\'' +
                ", gender='" + gender + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", progress=" + progress +
                '}';
    }
}
