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
 */
public final class DefaultLocaleResolver implements LocaleResolver {

    private static final int MAX_CACHE_SIZE = 1024;
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
            List<WeightedLocale> weighted = headerCache.computeIfAbsent(acceptLanguageHeader, this::parseAcceptLanguage);
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

    private List<WeightedLocale> parseAcceptLanguage(String header) {
        if (headerCache.size() > MAX_CACHE_SIZE) {
            headerCache.clear();
        }

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

            double q = 1.0;
            for (int i = 1; i < params.length; i++) {
                String param = params[i].trim();
                if (param.startsWith("q=") || param.startsWith("Q=")) {
                    try {
                        q = Double.parseDouble(param.substring(2).trim());
                    } catch (NumberFormatException ignored) {
                        q = 1.0;
                    }
                }
            }

            if (q > 0.0) {
                Locale loc = parseLocaleTag(range);
                if (loc != null) {
                    list.add(new WeightedLocale(loc, q));
                }
            }
        }

        Collections.sort(list);
        return List.copyOf(list);
    }

    private record WeightedLocale(Locale locale, double q) implements Comparable<WeightedLocale> {
        @Override
        public int compareTo(WeightedLocale o) {
            int cmp = Double.compare(o.q, this.q);
            if (cmp != 0) {
                return cmp;
            }
            return this.locale.toLanguageTag().compareTo(o.locale.toLanguageTag());
        }
    }
}
