package dev.vmellos.airhockey.core.localization

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Utilitário de localização herdado e refinado do projeto iptv_app,
 * com suporte nativo a pt-BR, en e es, e CompositionLocalProvider para Compose.
 */
object LocaleHelper {

    const val SYSTEM_DEFAULT = "SYSTEM"

    val SUPPORTED_LANGUAGES = listOf("pt-BR", "en", "es")

    fun getDeviceSystemLocale(): Locale {
        return try {
            val locales = Resources.getSystem().configuration.locales
            if (!locales.isEmpty) locales.get(0) else Locale.getDefault()
        } catch (_: Throwable) {
            Locale.getDefault()
        }
    }

    fun resolveSystemLanguage(systemLocale: Locale = getDeviceSystemLocale()): String {
        val lang = systemLocale.language.lowercase(Locale.ROOT)
        return when {
            lang.startsWith("pt") -> "pt-BR"
            lang.startsWith("es") -> "es"
            lang.startsWith("en") -> "en"
            else -> "en"
        }
    }

    fun resolveLanguage(
        configuredLanguage: String?,
        systemLocale: Locale = getDeviceSystemLocale()
    ): String {
        if (configuredLanguage.isNullOrBlank() || configuredLanguage.equals(SYSTEM_DEFAULT, ignoreCase = true)) {
            return resolveSystemLanguage(systemLocale)
        }
        return configuredLanguage
    }

    fun getLanguageDisplayName(languageCode: String): String {
        val effective = resolveLanguage(languageCode)
        return when {
            effective.startsWith("pt", ignoreCase = true) -> "Português (Brasil)"
            effective.startsWith("es", ignoreCase = true) -> "Español"
            else -> "English"
        }
    }

    fun parseLocale(languageCode: String, systemLocale: Locale = getDeviceSystemLocale()): Locale {
        val effectiveLang = resolveLanguage(languageCode, systemLocale)
        return when {
            effectiveLang.startsWith("pt", ignoreCase = true) -> Locale.forLanguageTag("pt-BR")
            effectiveLang.startsWith("es", ignoreCase = true) -> Locale.forLanguageTag("es")
            else -> Locale.forLanguageTag("en")
        }
    }

    fun applySystemLocale(context: Context, languageCode: String) {
        val effectiveLang = resolveLanguage(languageCode)
        val tag = when {
            effectiveLang.startsWith("pt", ignoreCase = true) -> "pt-BR"
            effectiveLang.startsWith("es", ignoreCase = true) -> "es"
            else -> "en"
        }
        val localeListCompat = LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(localeListCompat)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
            localeManager?.applicationLocales = LocaleList.forLanguageTags(tag)
        }
    }

    fun createLocalizedContext(baseContext: Context, locale: Locale): Context {
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val localizedConfigContext = baseContext.createConfigurationContext(config)
        return object : ContextWrapper(baseContext) {
            override fun getResources(): Resources = localizedConfigContext.resources
        }
    }
}

@Composable
fun ProvideAppLocale(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val currentContext = LocalContext.current
    val systemConfig = LocalConfiguration.current
    val locale = remember(languageCode, systemConfig.locales) {
        LocaleHelper.parseLocale(languageCode)
    }

    val localizedConfiguration = remember(locale, systemConfig) {
        Configuration(currentContext.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }

    val localizedContext = remember(locale, currentContext) {
        LocaleHelper.createLocalizedContext(currentContext, locale)
    }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
        content = content
    )
}
