package at.rtr.rmbt.android.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Forces the Latin (Western) numbering system for number formatting while keeping the
 * language and layout direction of the active locale.
 *
 * Some locales (e.g. Arabic) default to a non-Latin numbering system (Arabic-Indic digits).
 * The app should always display numbers with Latin digits (0-9). Plain [String.toString]
 * produces Latin digits, while [java.text.DecimalFormat], [String.format] and
 * resource formatting (e.g. getString with %d) follow the locale's numbering system,
 * which results in a mix of digit styles. Forcing the `nu-latn` Unicode extension on both
 * the default [Locale] and the resource [Configuration] makes all of them consistent.
 */
object LocaleHelper {

    /**
     * Returns a context whose configuration uses the active locale forced to Latin digits.
     * Also updates [Locale.setDefault] so that locale-unaware formatting (DecimalFormat,
     * String.format, ...) produces Latin digits as well.
     */
    fun wrap(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        val current = configuration.locales[0]

        val forced = forceLatinDigits(current)
        Locale.setDefault(forced)
        configuration.setLocale(forced)

        return context.createConfigurationContext(configuration)
    }

    private fun forceLatinDigits(locale: Locale): Locale {
        if (locale.getUnicodeLocaleType("nu") == "latn") {
            return locale
        }
        return Locale.Builder()
            .setLocale(locale)
            .setUnicodeLocaleKeyword("nu", "latn")
            .build()
    }
}
