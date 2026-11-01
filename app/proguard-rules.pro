# ===================================================================
# Proguard / R8 Optimization & Obfuscation Rules for Air Hockey
# ===================================================================

# -------------------------------------------------------------------
# 1. General & Attributes
# -------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**

# -------------------------------------------------------------------
# 2. Kotlin Coroutines
# -------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# -------------------------------------------------------------------
# 3. AndroidX Compose & Lifecycle
# -------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.compose.material.icons.** { *; }
-keepclassmembers class androidx.compose.material.icons.** { *; }
-keep class androidx.compose.ui.graphics.vector.** { *; }

# -------------------------------------------------------------------
# 4. AndroidX DataStore Preferences
# -------------------------------------------------------------------
-dontwarn androidx.datastore.**
-keep class androidx.datastore.preferences.protobuf.** { *; }

# -------------------------------------------------------------------
# 5. CameraX & ZXing (QR Code Offline)
# -------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# -------------------------------------------------------------------
# 6. Air Hockey Domain & Network Protocol Models
# -------------------------------------------------------------------
-keep class dev.vmellos.airhockey.feature.network.ControlMessages$* { *; }
-keep class dev.vmellos.airhockey.feature.network.TelemetryFrame { *; }
-keep class dev.vmellos.airhockey.feature.network.GuestInputMessage { *; }
-keep class dev.vmellos.airhockey.feature.game.MatchState { *; }
-keep class dev.vmellos.airhockey.feature.physics.PuckSnapshot { *; }
-keep class dev.vmellos.airhockey.feature.physics.MalletSnapshot { *; }
-keep class dev.vmellos.airhockey.core.preferences.UserPreferences { *; }
