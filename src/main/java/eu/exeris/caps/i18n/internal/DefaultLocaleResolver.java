package eu.exeris.caps.i18n.internal;

import eu.exeris.caps.i18n.api.LocaleResolver;
import eu.exeris.kernel.spi.http.HttpExchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe, cached implementation of {@link LocaleResolver} adhering to RFC 9110 §12.5.4.
 *
 * <p><b>Header cache.</b> Parsed {@code Accept-Language} values are cached by their raw text, up to
 * {@code MAX_CACHE_SIZE} (1024) entries. A header is parsed on the calling thread, outside the map, and
 * inserted with {@code putIfAbsent} only while the cache is under its bound; a full cache stops
 * admitting new entries and is never cleared, so a header first seen while it is full is parsed on
 * every call. The bound is approximate: the size check and the insert are not atomic, so the cache
 * may exceed it by at most the number of concurrent callers. Cached values are immutable lists.
 * The request path never runs a callback inside the map and never takes more than one bin lock.
 *
 * <p><b>Quality values.</b> A range's {@code q} must match the RFC 9110 §12.4.2 grammar: {@code 0}
 * to {@code 1} with at most three decimals. A range whose {@code q} is malformed or outside that
 * range is dropped, as is a range with {@code q=0} ("not acceptable").
 */
public final class DefaultLocaleResolver implements LocaleResolver {

    static final int MAX_CACHE_SIZE = 1024;
    private final Locale defaultLocale;
    private final ConcurrentMap<String, List<WeightedLocale>> headerCache = new ConcurrentHashMap<>();

    public DefaultLocaleResolver() {
        this(Locale.ENGLISH);
    }

    public DefaultLocaleResolver(Locale defaultLocale) {
        this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale");
    }

    @Override
    public Locale defaultLocale() {
        return defaultLocale;
    }

    @Override
    public Locale resolve(String acceptLanguageHeader, String queryParam) {
        return resolve(acceptLanguageHeader, queryParam, defaultLocale);
    }

    @Override
    public Locale resolve(String acceptLanguageHeader, String queryParam, Locale fallback) {
        Locale effectiveFallback = fallback != null ? fallback : defaultLocale;

        // 1. Check explicit query parameter precedence ('lang' or 'locale')
        if (queryParam != null && !queryParam.isBlank()) {
            Locale qLocale = parseLocaleTag(queryParam.trim());
            if (qLocale != null) {
                return qLocale;
            }
        }

        // 2. Parse Accept-Language header per RFC 9110 with q-weighting
        if (acceptLanguageHeader != null && !acceptLanguageHeader.isBlank()) {
            List<WeightedLocale> weighted = headerCache.get(acceptLanguageHeader);
            if (weighted == null) {
                weighted = parseAcceptLanguage(acceptLanguageHeader);
                if (headerCache.size() < MAX_CACHE_SIZE) {
                    headerCache.putIfAbsent(acceptLanguageHeader, weighted);
                }
            }
            if (!weighted.isEmpty()) {
                return weighted.get(0).locale();
            }
        }

        return effectiveFallback;
    }

    @Override
    public Locale resolve(HttpExchange exchange) {
        return resolve(exchange, defaultLocale);
    }

    @Override
    public Locale resolve(HttpExchange exchange, Locale fallback) {
        if (exchange == null || exchange.request() == null) {
            return fallback != null ? fallback : defaultLocale;
        }

        String path = exchange.request().path();
        String queryParam = extractLanguageParam(path);
        String acceptLanguage = exchange.request().firstHeader("Accept-Language").orElse(null);

        return resolve(acceptLanguage, queryParam, fallback);
    }

    private String extractLanguageParam(String path) {
        if (path == null) {
            return null;
        }
        int q = path.indexOf('?');
        if (q < 0 || q == path.length() - 1) {
            return null;
        }
        String query = path.substring(q + 1);
        int hash = query.indexOf('#');
        if (hash >= 0) {
            query = query.substring(0, hash);
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq > 0 ? pair.substring(0, eq) : pair;
            String val = eq > 0 ? pair.substring(eq + 1) : "";
            if ("lang".equalsIgnoreCase(key) || "locale".equalsIgnoreCase(key)) {
                return val;
            }
        }
        return null;
    }

    private Locale parseLocaleTag(String tag) {
        if (tag.isBlank() || "*".equals(tag)) {
            return null;
        }
        Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
        if (locale.getLanguage().isBlank()) {
            return null;
        }
        return locale;
    }

    /** Number of cached headers; exposed for tests of the cache bound. */
    int cachedHeaderCount() {
        return headerCache.size();
    }

    private List<WeightedLocale> parseAcceptLanguage(String header) {
        List<WeightedLocale> list = new ArrayList<>();
        String[] parts = header.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] params = trimmed.split(";");
            String range = params[0].trim();
            if (range.isEmpty() || "*".equals(range)) {
                continue;
            }

            int millis = 1000;
            boolean malformed = false;
            for (int i = 1; i < params.length; i++) {
                String param = params[i].trim();
                if (param.startsWith("q=") || param.startsWith("Q=")) {
                    millis = parseQuality(param.substring(2).trim());
                    if (millis < 0) {
                        malformed = true;
                    }
                }
            }

            if (!malformed && millis > 0) {
                Locale loc = parseLocaleTag(range);
                if (loc != null) {
                    list.add(new WeightedLocale(loc, millis));
                }
            }
        }

        Collections.sort(list);
        return List.copyOf(list);
    }

    /**
     * Parses an RFC 9110 §12.4.2 qvalue ({@code "0" ["." 0*3DIGIT]} or {@code "1" ["." 0*3("0")]}).
     *
     * @return the weight in thousandths (0 to 1000), or -1 when the text is not a valid qvalue
     */
    private static int parseQuality(String text) {
        int n = text.length();
        if (n == 0 || n > 5) {
            return -1;
        }
        char lead = text.charAt(0);
        if (lead != '0' && lead != '1') {
            return -1;
        }
        if (n == 1) {
            return lead == '1' ? 1000 : 0;
        }
        if (text.charAt(1) != '.') {
            return -1;
        }
        int fraction = 0;
        int scale = 100;
        for (int i = 2; i < n; i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            fraction += (c - '0') * scale;
            scale /= 10;
        }
        if (lead == '1') {
            return fraction == 0 ? 1000 : -1;
        }
        return fraction;
    }

    /** A language range with its weight in thousandths; sorts by descending weight. */
    private record WeightedLocale(Locale locale, int millis) implements Comparable<WeightedLocale> {
        @Override
        public int compareTo(WeightedLocale o) {
            return Integer.compare(o.millis, this.millis);
        }
    }
}
