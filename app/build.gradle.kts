import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun quotedBuildConfig(value: String): String =
    "\"${value.trim().trim('"').replace("\\", "\\\\").replace("\"", "\\\"")}\""

val bundledGitHubClientId = "Iv23liBz9KwjI8S24igW"
val githubClientId = sequenceOf(
    localProperties.getProperty("GITHUB_CLIENT_ID"),
    System.getenv("GITHUB_CLIENT_ID"),
    bundledGitHubClientId
).firstOrNull { !it.isNullOrBlank() }.orEmpty()
val configuredVersionName = providers.gradleProperty("GITHUB_ROCK_VERSION_NAME").orNull
    ?.trim()?.takeIf(String::isNotBlank) ?: "0.1.0"
val configuredVersionCode = providers.gradleProperty("GITHUB_ROCK_VERSION_CODE").orNull
    ?.toIntOrNull()?.takeIf { it > 0 } ?: 1

android {
    namespace = "com.sayanthrock.githubrock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sayanthrock.githubrock"
        minSdk = 29
        targetSdk = 36
        versionCode = configuredVersionCode
        versionName = configuredVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField(
            "String",
            "GITHUB_CLIENT_ID",
            quotedBuildConfig(githubClientId)
        )
        buildConfigField("String", "GITHUB_API_VERSION", "\"2022-11-28\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    val keystorePath = System.getenv("GITHUB_ROCK_KEYSTORE_PATH")
    if (!keystorePath.isNullOrBlank()) {
        signingConfigs.create("githubRelease") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("GITHUB_ROCK_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("GITHUB_ROCK_KEY_ALIAS")
            keyPassword = System.getenv("GITHUB_ROCK_KEY_PASSWORD")
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("githubRelease")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging.resources.excludes += setOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "META-INF/LICENSE.md",
        "META-INF/LICENSE-notice.md"
    )

    testOptions.unitTests.isIncludeAndroidResources = true
    lint.abortOnError = true
}

kapt { correctErrorTypes = true }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.work)
    implementation(libs.coil.compose)
    implementation(libs.androidx.security)
    implementation(libs.androidx.browser)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
