import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sentry)
}

fun loadPropertiesFile(fileName: String): Properties {
    val credentialsFile = rootProject.file("properties/$fileName")
    val properties = Properties()

    if (credentialsFile.exists()) {
        properties.load(FileInputStream(credentialsFile))
    } else {
        throw FileNotFoundException("Please make sure you have $fileName in properties/")
    }

    if (properties.isEmpty) {
        throw FileNotFoundException("The file $fileName is empty")
    }

    return properties
}

val airportsProperties = loadPropertiesFile("airports.properties")
val trackingProperties = loadPropertiesFile("tracking.properties")


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

        val apiKey = airportsProperties.getProperty("API_KEY", "")
        if (apiKey.isNullOrBlank()) throw FileNotFoundException("Please make sure you filled in API_KEY entry in your airport.properties")
        val baseUrl = airportsProperties.getProperty("BASE_URL", "")
        if (baseUrl.isNullOrBlank()) throw FileNotFoundException("Please make sure you filled in BASE_URL entry in your airport.properties")

        val sentryDsn = trackingProperties.getProperty("SENTRY_DSN", "")
        if (sentryDsn.isNullOrBlank()) throw FileNotFoundException("Please make sure you filled in SENTRY_DSN entry in your tracking.properties")

        buildConfigField("String", "AIRPORTS_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "AIRPORTS_BASE_URL", "\"$baseUrl\"")

        manifestPlaceholders["SENTRY_DSN"] = sentryDsn
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

sentry {
    val organisation = trackingProperties.getProperty("SENTRY_ORGANISATION", "")
    org.set(organisation)
    projectName.set("android")
    includeSourceContext.set(true)
}
