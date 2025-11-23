import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.julian.automaticclockwidget"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.julian.automaticclockwidget"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ... other config

        // Load properties file
        val credentialsFile = rootProject.file("properties/airports.properties")
        val credentials = Properties()

        if (credentialsFile.exists()) {
            credentials.load(FileInputStream(credentialsFile))
        }

        if(credentials.isEmpty) throw FileNotFoundException("Please make sure you have airports.properties in properties/")
        val apiKey = credentials.getProperty("API_KEY", "")
        if(apiKey.isNullOrBlank()) throw FileNotFoundException("Please make sure you filled in API_KEY entry in your airport.properties")
        val baseUrl = credentials.getProperty("BASE_URL", "")
        if(baseUrl.isNullOrBlank()) throw FileNotFoundException("Please make sure you filled in BASE_URL entry in your airport.properties")

        // Inject into BuildConfig
        buildConfigField("String", "AIRPORTS_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "AIRPORTS_BASE_URL", "\"$baseUrl\"")
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
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    implementation(libs.biweekly)
    implementation(project.dependencies.platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.interceptor)
    implementation(libs.date.time)
    implementation(libs.kotlin.serialization)
    implementation(libs.glance)
    implementation(libs.androidx.work.ktx)
}