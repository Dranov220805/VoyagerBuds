import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.example.voyagerbuds"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.voyagerbuds"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SMTP_USER", "\"${localProperties.getProperty("SMTP_USER")}\"")
        buildConfigField("String", "SMTP_PASSWORD", "\"${localProperties.getProperty("SMTP_PASSWORD")}\"")
        buildConfigField("String", "SMTP_FROM_EMAIL", "\"${localProperties.getProperty("SMTP_FROM_EMAIL")}\"")
        buildConfigField("String", "SMTP_FROM_NAME", "\"${localProperties.getProperty("SMTP_FROM_NAME")}\"")
        // CURRENCY_API_KEY removed - now using free exchange-api (no key required)
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // OSMDroid for map
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.github.MKergall:osmbonuspack:6.9.0")

    // Google Play Services for location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Firebase & Google Auth
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:21.2.0") // Google Sign-In library

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation(libs.preference)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Glide for efficient image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.camera.core)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Guava for CameraX
    implementation("com.google.guava:guava:31.1-android")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime:2.9.0")

    // JavaMail for sending emails
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
