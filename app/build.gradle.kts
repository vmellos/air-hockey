import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "dev.vmellos.airhockey"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.vmellos.airhockey"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val localProps = project.rootProject.file("local.properties").let { file ->
                if (file.exists()) {
                    Properties().apply { file.inputStream().use { load(it) } }
                } else {
                    Properties()
                }
            }
            val keystoreProp = localProps.getProperty("KEYSTORE_PATH") ?: System.getenv("KEYSTORE_PATH")
            val keystoreFile = keystoreProp?.let { path ->
                val f = file(path)
                if (f.exists()) f else project.rootProject.file(path).takeIf { it.exists() }
            }
            if (keystoreFile != null) {
                storeFile = keystoreFile
                storePassword = localProps.getProperty("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = localProps.getProperty("KEY_ALIAS") ?: System.getenv("KEY_ALIAS") ?: ""
                keyPassword = localProps.getProperty("KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD") ?: ""
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            val localProps = project.rootProject.file("local.properties").let { file ->
                if (file.exists()) {
                    Properties().apply { file.inputStream().use { load(it) } }
                } else {
                    Properties()
                }
            }
            val admobReleaseId = localProps.getProperty("ADMOB_APP_ID")
                ?: System.getenv("ADMOB_APP_ID")
                ?: "ca-app-pub-6935322996335458~6357365605"
            manifestPlaceholders["admobAppId"] = admobReleaseId

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Room Database for Match History & Leaderboard
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Google Mobile Ads (AdMob) & UMP Consent
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Google Play In-App Billing
    implementation(libs.play.billing.ktx)

    // Google Play In-App Updates
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // Offline QR Code Generation & Scanning
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}