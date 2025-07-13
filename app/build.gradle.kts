plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.arctco.cinematique" // Kotlin DSL uses '=' for assignments
    compileSdk = 34 // Kotlin DSL uses '=' for assignments

    defaultConfig {
        applicationId = "com.arctco.cinematique" // Kotlin DSL uses '=' for assignments
        minSdk = 21 // Kotlin DSL uses '=' for assignments
        targetSdk = 34 // Kotlin DSL uses '=' for assignments
        versionCode = 3
        versionName = "0.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Kotlin DSL uses 'is' prefix for boolean properties
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") // Function call syntax
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Kotlin DSL uses '=' for assignments
        targetCompatibility = JavaVersion.VERSION_1_8 // Kotlin DSL uses '=' for assignments
    }
    kotlinOptions {
        jvmTarget = "1.8" // Kotlin DSL uses '=' for assignments
    }
}

dependencies {
    // Core AndroidX libraries for UI and compatibility
    implementation("androidx.core:core-ktx:1.13.1") // Function call syntax for dependencies
    implementation("androidx.appcompat:appcompat:1.7.0") // Function call syntax for dependencies
    implementation("com.google.android.material:material:1.12.0") // Function call syntax for dependencies
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Function call syntax for dependencies
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Testing dependencies (usually included by default)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}