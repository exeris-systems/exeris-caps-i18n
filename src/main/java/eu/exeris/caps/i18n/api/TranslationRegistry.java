package eu.exeris.caps.i18n.api;

import java.util.Locale;
import java.util.Map;

/**
 * In-memory thread-safe registry of localized messages and translations.
 */
public interface TranslationRegistry {

    /**
     * Registers a translation message for the given locale and key.
     *
     * @param locale  target locale
     * @param key     message key identifier
     * @param message translated message string
     */
    void register(Locale locale, String key, String message);

    /**
     * Registers multiple translations for the given locale.
     *
     * @param locale       target locale
     * @param translations map of key to message
     */
    default void registerAll(Locale locale, Map<String, String> translations) {
        if (translations != null) {
            translations.forEach((k, v) -> register(locale, k, v));
        }
    }

    /**
     * Resolves a message for the specified locale and key, returning fallback if not found.
     *
     * @param locale   target locale
     * @param key      message key identifier
     * @param fallback fallback message string if key is unmapped
     * @return translated message or fallback
     */
    String resolve(Locale locale, String key, String fallback);

    /**
     * Returns an immutable map containing all registered translations for the given locale.
     *
     * @param locale target locale
     * @return map of key-value translations for the locale
     */
    Map<String, String> getAll(Locale locale);
}
