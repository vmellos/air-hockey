package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.localization.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleHelperTest {

    @Test
    fun testLanguageResolution() {
        assertEquals("pt-BR", LocaleHelper.resolveLanguage("pt-BR"))
        assertEquals("es", LocaleHelper.resolveLanguage("es"))
        assertEquals("en", LocaleHelper.resolveLanguage("en"))
    }

    @Test
    fun testSystemDefaultFallback() {
        val ptLocale = Locale.forLanguageTag("pt-BR")
        val resolvedPt = LocaleHelper.resolveSystemLanguage(ptLocale)
        assertEquals("pt-BR", resolvedPt)

        val esLocale = Locale.forLanguageTag("es-ES")
        val resolvedEs = LocaleHelper.resolveSystemLanguage(esLocale)
        assertEquals("es", resolvedEs)

        val deLocale = Locale.forLanguageTag("de-DE")
        val resolvedDe = LocaleHelper.resolveSystemLanguage(deLocale)
        assertEquals("en", resolvedDe) // Universal fallback to en
    }

    @Test
    fun testParseLocale() {
        val pt = LocaleHelper.parseLocale("pt-BR")
        assertEquals("pt", pt.language)

        val es = LocaleHelper.parseLocale("es")
        assertEquals("es", es.language)

        val en = LocaleHelper.parseLocale("en")
        assertEquals("en", en.language)
    }
}
