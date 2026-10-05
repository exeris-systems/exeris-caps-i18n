package eu.exeris.caps.i18n.internal;

import eu.exeris.caps.i18n.api.TranslationRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe implementation of {@link TranslationRegistry} backed by {@link ConcurrentHashMap}.
 */
public final class DefaultTranslationRegistry implements TranslationRegistry {

    private final ConcurrentMap<String, ConcurrentMap<String, String>> translationsByTag = new ConcurrentHashMap<>();

    @Override
    public void register(Locale locale, String key, String message) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(message, "message");

        String tag = normalizeTag(locale);
        translationsByTag.computeIfAbsent(tag, k -> new ConcurrentHashMap<>()).put(key, message);
    }

    @Override
    public String resolve(Locale locale, String key, String fallback) {
        if (locale == null || key == null) {
            return fallback;
        }

        // 1. Check exact language tag (e.g., "pl-pl")
        String tag = normalizeTag(locale);
        ConcurrentMap<String, String> map = translationsByTag.get(tag);
        if (map != null) {
            String val = map.get(key);
            if (val != null) {
                return val;
            }
        }

        // 2. Fall back to base language (e.g., "pl")
        String lang = locale.getLanguage().toLowerCase(Locale.ROOT);
        if (!lang.isBlank() && !lang.equals(tag)) {
            ConcurrentMap<String, String> langMap = translationsByTag.get(lang);
            if (langMap != null) {
                String val = langMap.get(key);
                if (val != null) {
                    return val;
                }
            }
        }

        return fallback;
    }

    @Override
    public Map<String, String> getAll(Locale locale) {
        if (locale == null) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();

        // 1. Base language translations
        String lang = locale.getLanguage().toLowerCase(Locale.ROOT);
        ConcurrentMap<String, String> langMap = translationsByTag.get(lang);
        if (langMap != null) {
            result.putAll(langMap);
        }

        // 2. Specific tag translations overlay base language
        String tag = normalizeTag(locale);
        if (!tag.equals(lang)) {
            ConcurrentMap<String, String> tagMap = translationsByTag.get(tag);
            if (tagMap != null) {
                result.putAll(tagMap);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    private String normalizeTag(Locale locale) {
        return locale.toLanguageTag().toLowerCase(Locale.ROOT);
    }
}
