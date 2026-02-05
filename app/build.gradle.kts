import java.util.regex.Pattern.compile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.python1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.python1"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)


    testImplementation(libs.junit)


    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.play.services.auth)
    implementation(libs.facebook.android.sdk)
    implementation(libs.facebook.login)
    implementation(libs.cardview)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.picasso)

    // ✅ Keep only ONE Glide version (Use latest)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")


    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")




    implementation("com.squareup.picasso:picasso:2.71828")
    implementation(libs.cloudinary.android.v302)


    implementation("androidx.media3:media3-exoplayer:1.6.1")  // Latest ExoPlayer
    implementation("androidx.media3:media3-exoplayer-dash:1.6.1")  // For DASH streaming
    implementation("androidx.media3:media3-ui:1.6.1")



    implementation("com.github.jinatonic.confetti:confetti:1.1.2")

    implementation("com.google.firebase:firebase-messaging:24.1.1")




    implementation("de.hdodenhof:circleimageview:3.1.0")

    implementation("com.google.android.gms:play-services-ads:24.2.0")

    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.exifinterface:exifinterface:1.3.6")

    implementation("com.razorpay:checkout:1.6.33")// Or the latest version

    implementation("com.airbnb.android:lottie:6.4.1")

    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.github.lzyzsd:circleprogress:1.2.1")


    implementation("com.google.ai.client.generativeai:generativeai:0.8.0")

    implementation("com.google.code.gson:gson:2.10.1")
}
