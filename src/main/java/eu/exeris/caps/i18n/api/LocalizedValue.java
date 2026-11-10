package eu.exeris.caps.i18n.api;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable carrier for multi-lingual values across languages with graceful fallback.
 *
 * @param translations map of language tag or language code to localized string
 */
public record LocalizedValue(Map<String, String> translations) {

    public LocalizedValue {
        java.util.LinkedHashMap<String, String> copy = new java.util.LinkedHashMap<>();
        if (translations != null) {
            for (var entry : translations.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    copy.put(entry.getKey().replace('_', '-').toLowerCase(Locale.ROOT), entry.getValue());
                }
            }
        }
        translations = java.util.Collections.unmodifiableMap(copy);
    }

    public static LocalizedValue of(Map<String, String> translations) {
        return new LocalizedValue(translations);
    }

    public static LocalizedValue of(Locale locale, String value) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(value, "value");
        return new LocalizedValue(Map.of(locale.toLanguageTag(), value));
    }

    public static LocalizedValue of(String languageTag, String value) {
        Objects.requireNonNull(languageTag, "languageTag");
        Objects.requireNonNull(value, "value");
        return new LocalizedValue(Map.of(languageTag, value));
    }

    /**
     * Retrieves the localized string for the first of these that matches: the requested locale, the
     * fallback locale, English ({@code en}), then the first translation in insertion order; returns
     * {@code null} only when there are no translations.
     *
     * <p>A locale matches on its exact lower-cased language tag (for example {@code pl-pl}), then on
     * its language alone (for example {@code pl}). A {@code null} requested or fallback locale is skipped.
     *
     * @param requested primary desired locale
     * @param fallback  secondary fallback locale
     * @return matching localized string or null
     */
    public String get(Locale requested, Locale fallback) {
        if (translations.isEmpty()) {
            return null;
        }
        if (requested != null) {
            String val = findMatch(requested);
            if (val != null) {
                return val;
            }
        }
        if (fallback != null) {
            String val = findMatch(fallback);
            if (val != null) {
                return val;
            }
        }
        // Fallback to English if present
        String enVal = findMatch(Locale.ENGLISH);
        if (enVal != null) {
            return enVal;
        }
        // First available translation
        return translations.values().iterator().next();
    }

    private String findMatch(Locale locale) {
        if (locale == null) return null;
        // 1. Exact language tag (e.g. "en-us", "pl-pl")
        String tag = locale.toLanguageTag().toLowerCase(Locale.ROOT);
        String direct = translations.get(tag);
        if (direct != null) {
            return direct;
        }
        // 2. Language only (e.g. "en", "pl")
        String lang = locale.getLanguage().toLowerCase(Locale.ROOT);
        if (!lang.isBlank()) {
            return translations.get(lang);
        }
        return null;
    }
}
