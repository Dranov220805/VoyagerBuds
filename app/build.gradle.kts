plugins {
    alias(libs.plugins.android.application)
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // OSMDroid for map
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    
    // Google Play Services for location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

