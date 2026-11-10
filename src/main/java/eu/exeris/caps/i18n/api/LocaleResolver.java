package eu.exeris.caps.i18n.api;

import eu.exeris.kernel.spi.http.HttpExchange;

import java.util.Locale;

/**
 * Resolves the client's {@link Locale} from HTTP request attributes per RFC 9110 §12.5.4.
 *
 * <p>Resolution precedence:
 * <ol>
 *   <li>Explicit query parameter ({@code lang} or {@code locale});</li>
 *   <li>{@code Accept-Language} header with quality (q) weighting;</li>
 *   <li>Configured default fallback locale (defaults to {@link Locale#ENGLISH}).</li>
 * </ol>
 */
public interface LocaleResolver {

    /**
     * Resolves the target {@link Locale} using the provided header value and query parameter.
     *
     * @param acceptLanguageHeader raw Accept-Language header value (nullable)
     * @param queryParam           explicit language/locale query param (nullable)
     * @return resolved Locale, falling back to {@link #defaultLocale()}
     */
    Locale resolve(String acceptLanguageHeader, String queryParam);

    /**
     * Resolves the target {@link Locale} using the provided header, query param, and custom fallback.
     *
     * @param acceptLanguageHeader raw Accept-Language header value (nullable)
     * @param queryParam           explicit language/locale query param (nullable)
     * @param fallback             fallback locale to use if resolution produces no match
     * @return resolved Locale
     */
    Locale resolve(String acceptLanguageHeader, String queryParam, Locale fallback);

    /**
     * Resolves the target {@link Locale} from an active {@link HttpExchange}.
     *
     * @param exchange HTTP exchange to extract headers and query parameters from
     * @return resolved Locale
     */
    Locale resolve(HttpExchange exchange);

    /**
     * Resolves the target {@link Locale} from an active {@link HttpExchange} with custom fallback.
     *
     * @param exchange HTTP exchange to extract headers and query parameters from
     * @param fallback fallback locale to use
     * @return resolved Locale
     */
    Locale resolve(HttpExchange exchange, Locale fallback);

    /**
     * Returns the default fallback locale for this resolver.
     *
     * @return default locale
     */
    Locale defaultLocale();
}
