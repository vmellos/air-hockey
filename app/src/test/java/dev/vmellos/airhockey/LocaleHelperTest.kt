package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.localization.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

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

    @Test
    fun testCreditsStringsLocalizedAcrossAllSupportedLocales() {
        val baseDir = listOf(File("src/main/res"), File("app/src/main/res")).first { it.exists() }
        val keys = listOf(
            "settings_credits_link",
            "settings_credits_title",
            "settings_credits_body",
            "settings_credits_close"
        )

        fun readStrings(folder: String): Map<String, String> {
            val file = File(baseDir, "$folder/strings.xml")
            assertTrue("Missing $file", file.exists())
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            return buildMap {
                for (i in 0 until nodes.length) {
                    val el = nodes.item(i) as org.w3c.dom.Element
                    put(el.getAttribute("name"), el.textContent)
                }
            }
        }

        val enStrings = readStrings("values")
        val ptStrings = readStrings("values-pt")
        val esStrings = readStrings("values-es")

        for (key in keys) {
            val enVal = enStrings[key].orEmpty()
            val ptVal = ptStrings[key].orEmpty()
            val esVal = esStrings[key].orEmpty()
            assertTrue("Empty $key in values", enVal.isNotBlank())
            assertTrue("Empty $key in values-pt", ptVal.isNotBlank())
            assertTrue("Empty $key in values-es", esVal.isNotBlank())
            assertNotEquals("$key should be translated in pt", enVal, ptVal)
            assertNotEquals("$key should be translated in es", enVal, esVal)
        }
    }
}
