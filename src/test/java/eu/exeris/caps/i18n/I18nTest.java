package eu.exeris.caps.i18n;

import eu.exeris.caps.i18n.api.LocaleResolver;
import eu.exeris.caps.i18n.api.LocalizedValue;
import eu.exeris.caps.i18n.api.TranslationRegistry;
import eu.exeris.caps.i18n.internal.DefaultLocaleResolver;
import eu.exeris.caps.i18n.internal.DefaultTranslationRegistry;
import eu.exeris.kernel.spi.http.HttpExchange;
import eu.exeris.kernel.spi.http.HttpHeader;
import eu.exeris.kernel.spi.http.HttpMethod;
import eu.exeris.kernel.spi.http.HttpRequest;
import eu.exeris.kernel.spi.http.HttpResponse;
import eu.exeris.kernel.spi.http.HttpStatus;
import eu.exeris.kernel.spi.http.HttpTypedResponse;
import eu.exeris.kernel.spi.http.HttpVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("exeris-caps-i18n — Capability Unit Tests")
class I18nTest {

    @Test
    @DisplayName("LocaleResolver resolves from Accept-Language with q-weighting")
    void testAcceptLanguageQWeighting() {
        LocaleResolver resolver = new DefaultLocaleResolver(Locale.ENGLISH);

        // Highest weight is German (0.9), higher than French (0.5)
        Locale resolved1 = resolver.resolve("fr;q=0.5, de;q=0.9, en;q=0.2", (String) null);
        assertThat(resolved1.getLanguage()).isEqualTo("de");

        // Polish with default q=1.0 is higher than en;q=0.8
        Locale resolved2 = resolver.resolve("pl-PL, en-US;q=0.8, en;q=0.5", (String) null);
        assertThat(resolved2).isEqualTo(Locale.forLanguageTag("pl-PL"));

        // Complex weights
        Locale resolved3 = resolver.resolve("en;q=0.7, es;q=0.85, it;q=0.6", (String) null);
        assertThat(resolved3.getLanguage()).isEqualTo("es");
    }

    @Test
    @DisplayName("LocaleResolver gives query parameter precedence over header")
    void testQueryParamPrecedence() {
        LocaleResolver resolver = new DefaultLocaleResolver(Locale.ENGLISH);

        Locale resolved = resolver.resolve("de-DE,de;q=0.9", "pl");
        assertThat(resolved.getLanguage()).isEqualTo("pl");

        Locale resolvedWithLocaleParam = resolver.resolve("de-DE", "fr-FR");
        assertThat(resolvedWithLocaleParam).isEqualTo(Locale.forLanguageTag("fr-FR"));
    }

    @Test
    @DisplayName("LocaleResolver falls back when headers are empty, invalid or wildcard")
    void testFallback() {
        LocaleResolver resolver = new DefaultLocaleResolver(Locale.ENGLISH);

        assertThat(resolver.resolve((String) null, (String) null)).isEqualTo(Locale.ENGLISH);
        assertThat(resolver.resolve("", "")).isEqualTo(Locale.ENGLISH);
        assertThat(resolver.resolve("*", "")).isEqualTo(Locale.ENGLISH);
        assertThat(resolver.resolve("*, en-GB;q=0.1", (String) null)).isEqualTo(Locale.forLanguageTag("en-GB"));

        Locale customFallback = Locale.forLanguageTag("pl");
        assertThat(resolver.resolve((String) null, (String) null, customFallback)).isEqualTo(customFallback);
    }

    @Test
    @DisplayName("LocaleResolver resolves from HttpExchange")
    void testResolveFromHttpExchange() {
        LocaleResolver resolver = new DefaultLocaleResolver(Locale.ENGLISH);

        TestHttpExchange exchange = new TestHttpExchange(
                "/api/items?lang=pl",
                List.of(new HttpHeader("Accept-Language", "en-US,en;q=0.9"))
        );

        Locale resolved = resolver.resolve(exchange);
        assertThat(resolved.getLanguage()).isEqualTo("pl");

        TestHttpExchange exchangeHeaderOnly = new TestHttpExchange(
                "/api/items",
                List.of(new HttpHeader("Accept-Language", "de-DE,de;q=0.9"))
        );
        assertThat(resolver.resolve(exchangeHeaderOnly).getLanguage()).isEqualTo("de");
    }

    @Test
    @DisplayName("TranslationRegistry stores, resolves and provides language hierarchy")
    void testTranslationRegistry() {
        TranslationRegistry registry = new DefaultTranslationRegistry();

        registry.register(Locale.ENGLISH, "greeting", "Hello");
        registry.register(Locale.forLanguageTag("pl"), "greeting", "Cześć");
        registry.register(Locale.forLanguageTag("pl-PL"), "greeting", "Dzień dobry");
        registry.register(Locale.forLanguageTag("pl"), "save", "Zapisz");

        // Exact pl-PL
        assertThat(registry.resolve(Locale.forLanguageTag("pl-PL"), "greeting", "Hi")).isEqualTo("Dzień dobry");
        // Base pl
        assertThat(registry.resolve(Locale.forLanguageTag("pl"), "greeting", "Hi")).isEqualTo("Cześć");
        // pl-PL falls back to pl for "save"
        assertThat(registry.resolve(Locale.forLanguageTag("pl-PL"), "save", "Save")).isEqualTo("Zapisz");
        // Non-existent key falls back
        assertThat(registry.resolve(Locale.GERMAN, "greeting", "Guten Tag")).isEqualTo("Guten Tag");

        // getAll returns unmodifiable map
        Map<String, String> plAll = registry.getAll(Locale.forLanguageTag("pl-PL"));
        assertThat(plAll).containsEntry("greeting", "Dzień dobry").containsEntry("save", "Zapisz");
    }

    @Test
    @DisplayName("TranslationRegistry is thread-safe under concurrent registrations")
    void testTranslationRegistryConcurrency() throws Exception {
        TranslationRegistry registry = new DefaultTranslationRegistry();
        int threads = 8;
        int perThread = 100;
        CountDownLatch latch = new CountDownLatch(threads);

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                pool.submit(() -> {
                    try {
                        Locale loc = threadId % 2 == 0 ? Locale.ENGLISH : Locale.forLanguageTag("pl");
                        for (int i = 0; i < perThread; i++) {
                            registry.register(loc, "key_" + threadId + "_" + i, "val_" + i);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        assertThat(registry.resolve(Locale.ENGLISH, "key_0_50", null)).isEqualTo("val_50");
        assertThat(registry.resolve(Locale.forLanguageTag("pl"), "key_1_50", null)).isEqualTo("val_50");
    }

    @Test
    @DisplayName("LocalizedValue resolves localized strings with fallback chain and handles underscore tags and nulls")
    void testLocalizedValue() {
        java.util.Map<String, String> rawMap = new java.util.HashMap<>();
        rawMap.put("en", "Hello World");
        rawMap.put("pl_PL", "Witaj w Polsce");
        rawMap.put("de", null); // null tolerance
        LocalizedValue val = LocalizedValue.of(rawMap);

        assertThat(val.get(Locale.forLanguageTag("pl-PL"), Locale.ENGLISH)).isEqualTo("Witaj w Polsce");
        assertThat(val.get(Locale.forLanguageTag("pl"), Locale.ENGLISH)).isEqualTo("Hello World");
        assertThat(val.get(Locale.FRENCH, Locale.ENGLISH)).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("LocaleResolver preserves RFC 9110 stable preference ordering for equal weights")
    void testStableAcceptLanguagePreference() {
        LocaleResolver resolver = new DefaultLocaleResolver(Locale.ENGLISH);

        // When q is equal (default 1.0), order of appearance in header must take precedence
        Locale resolvedPlFirst = resolver.resolve("pl, en", (String) null);
        assertThat(resolvedPlFirst.getLanguage()).isEqualTo("pl");

        Locale resolvedEnFirst = resolver.resolve("en, pl", (String) null);
        assertThat(resolvedEnFirst.getLanguage()).isEqualTo("en");

        // With URI fragment stripped
        TestHttpExchange exchangeWithFragment = new TestHttpExchange(
                "/api/items?lang=pl#section",
                List.of(new HttpHeader("Accept-Language", "en-US"))
        );
        assertThat(resolver.resolve(exchangeWithFragment).getLanguage()).isEqualTo("pl");
    }

    @Test
    @DisplayName("I18nModule lifecycle and services are accessible")
    void testI18nModule() throws Exception {
        I18nModule module = new I18nModule();
        assertThat(module.localeResolver()).isNotNull();
        assertThat(module.translationRegistry()).isNotNull();
        assertThat(module.isReady()).isFalse();

        module.initialize();
        module.ready();
        assertThat(module.isReady()).isTrue();

        module.drain(Duration.ofSeconds(1));
        assertThat(module.isReady()).isFalse();

        module.terminate();
        assertThat(module.isReady()).isFalse();

        // Also test internal.I18nModule
        eu.exeris.caps.i18n.internal.I18nModule internalModule = new eu.exeris.caps.i18n.internal.I18nModule();
        assertThat(internalModule.localeResolver()).isNotNull();
        assertThat(internalModule.translationRegistry()).isNotNull();
    }

    private static final class TestHttpExchange implements HttpExchange {
        private final HttpRequest request;

        TestHttpExchange(String path, List<HttpHeader> headers) {
            this.request = new HttpRequest(HttpMethod.GET, path, HttpVersion.HTTP_1_1, headers, null);
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public void respond(HttpResponse response) {}

        @Override
        public void respond(HttpStatus status) {}

        @Override
        public void respond(HttpTypedResponse response) {}

        @Override
        public void respond(HttpStatus status, Object body) {}

        @Override
        public Map<String, String> pathParams() {
            return Map.of();
        }
    }
}
