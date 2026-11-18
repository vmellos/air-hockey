import java.io.ByteArrayInputStream
import java.util.Arrays
import java.util.Properties
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

@CacheableTask
abstract class PackageProtectedAssetsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rawAssetsDir: DirectoryProperty

    @get:Classpath
    abstract val packerClasspath: ConfigurableFileCollection

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val vaultSecret: Property<String>

    @get:OutputDirectory
    abstract val outputAssetsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputKotlinDir: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun packageAssets() {
        val assetsOut = outputAssetsDir.get().asFile
        val kotlinOut = outputKotlinDir.get().asFile
        val secretBytes = vaultSecret.get().toByteArray(Charsets.UTF_8)
        try {
            execOperations.javaexec {
                classpath = packerClasspath
                mainClass.set("dev.vmellos.airhockey.assetcrypto.AssetPackerCli")
                args(
                    "--input-dir", rawAssetsDir.get().asFile.absolutePath,
                    "--output-assets-dir", assetsOut.absolutePath,
                    "--output-kotlin-dir", kotlinOut.absolutePath,
                    "--package", "dev.vmellos.airhockey.core.assets",
                    "--class-name", "GeneratedAssetVaultKey",
                    "--variant", variantName.get()
                )
                standardInput = ByteArrayInputStream(secretBytes)
            }
        } finally {
            Arrays.fill(secretBytes, 0.toByte())
        }
    }
}

val assetPackerClasspath = configurations.create("assetPackerClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val localPropsProvider = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText
    .map { text -> Properties().apply { text.reader().use { load(it) } } }
    .orElse(Properties())

val configuredVaultSecretProvider = providers.gradleProperty("ASSET_VAULT_SECRET")
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .orElse(
        localPropsProvider
            .map { props -> props.getProperty("ASSET_VAULT_SECRET")?.trim().orEmpty() }
            .filter { it.isNotEmpty() }
    )
    .orElse(
        providers.environmentVariable("ASSET_VAULT_SECRET")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    )

android {
    namespace = "dev.vmellos.airhockey"
    compileSdk = 36

    val localProps = project.rootProject.file("local.properties").let { file ->
        if (file.exists()) {
            Properties().apply { file.inputStream().use { load(it) } }
        } else {
            Properties()
        }
    }

    val useProductionAds = (
        project.findProperty("USE_PRODUCTION_ADS")?.toString()
            ?: localProps.getProperty("USE_PRODUCTION_ADS")
            ?: System.getenv("USE_PRODUCTION_ADS")
            ?: "false"
        ).toBooleanStrictOrNull() ?: false

    val leaderboardEloId = localProps.getProperty("LEADERBOARD_ELO_ID")
        ?: System.getenv("LEADERBOARD_ELO_ID")
        ?: ""
    val leaderboardStagesId = localProps.getProperty("LEADERBOARD_STAGES_ID")
        ?: System.getenv("LEADERBOARD_STAGES_ID")
        ?: ""

    if (useProductionAds && (leaderboardEloId.isBlank() || leaderboardStagesId.isBlank())) {
        throw GradleException(
            "USE_PRODUCTION_ADS=true requires LEADERBOARD_ELO_ID and LEADERBOARD_STAGES_ID in local.properties or environment variables."
        )
    }

    if (useProductionAds && !configuredVaultSecretProvider.isPresent) {
        throw GradleException(
            "USE_PRODUCTION_ADS=true requires ASSET_VAULT_SECRET in local.properties, Gradle property, or environment variables."
        )
    }

    defaultConfig {
        applicationId = "dev.vmellos.airhockey"
        minSdk = 26
        targetSdk = 36
        versionCode = 28
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val pgsAppId = localProps.getProperty("PLAY_GAMES_APP_ID")
            ?: System.getenv("PLAY_GAMES_APP_ID")
            ?: "925142400705"
        manifestPlaceholders["playGamesAppId"] = pgsAppId

        buildConfigField("boolean", "USE_PRODUCTION_ADS", useProductionAds.toString())
        resValue("string", "leaderboard_elo_id", leaderboardEloId)
        resValue("string", "leaderboard_stages_id", leaderboardStagesId)
    }

    signingConfigs {
        create("release") {
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
                if (useProductionAds) {
                    throw GradleException(
                        "USE_PRODUCTION_ADS=true requires a valid release keystore configured via KEYSTORE_PATH."
                    )
                }
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            val admobReleaseId = localProps.getProperty("ADMOB_APP_ID")
                ?: System.getenv("ADMOB_APP_ID")
                ?: "ca-app-pub-6935322996335458~9861411597"
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
        resValues = true
    }
    androidResources {
        noCompress += "lpkg"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    val rawAssetsDirectory = layout.projectDirectory.dir("src/main/raw_assets")
    val appNamespace = android.namespace ?: "dev.vmellos.airhockey"
    onVariants { variant ->
        val capName = variant.name.replaceFirstChar { it.uppercase() }
        val variantSecret = configuredVaultSecretProvider.orElse(
            "airhockey-local-vault-v1:$appNamespace:${variant.name}"
        )
        val packTask = tasks.register<PackageProtectedAssetsTask>("packageProtectedAssets$capName") {
            rawAssetsDir.set(rawAssetsDirectory)
            packerClasspath.from(assetPackerClasspath)
            variantName.set(variant.name)
            vaultSecret.set(variantSecret)
        }
        variant.sources.assets?.addGeneratedSourceDirectory(
            packTask,
            PackageProtectedAssetsTask::outputAssetsDir
        )
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            packTask,
            PackageProtectedAssetsTask::outputKotlinDir
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":asset-crypto"))
    assetPackerClasspath(project(":asset-crypto"))
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

    // Google Play Games Services v2 & Tasks Coroutines
    implementation(libs.play.services.games.v2)
    implementation(libs.kotlinx.coroutines.play.services)

    // Offline QR Code Generation & Scanning
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // 3D Sceneview (Google Filament)
    implementation(libs.sceneview)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}