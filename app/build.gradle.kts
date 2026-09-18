import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services) // 파이어베이
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val mapboxAccessToken = providers.gradleProperty("MAPBOX_ACCESS_TOKEN")
    .orElse(localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: "")
    .get()

val kakaoRestApiKey = providers.gradleProperty("KAKAO_REST_API_KEY")
    .orElse(localProperties.getProperty("KAKAO_REST_API_KEY") ?: "")
    .get()

android {
    namespace = "com.example.safepath_test1"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.safepath_test1"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "mapbox_access_token", mapboxAccessToken)
        resValue("string", "kakao_rest_api_key", kakaoRestApiKey)
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.mapbox.maps:android-ndk27:11.30.0")
    implementation("com.mapbox.extension:maps-compose-ndk27:11.30.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    // 파이어베이스
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
}
