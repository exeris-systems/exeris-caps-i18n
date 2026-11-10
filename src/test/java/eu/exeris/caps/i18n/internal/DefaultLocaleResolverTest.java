package eu.exeris.caps.i18n.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultLocaleResolver — header cache and quality values")
class DefaultLocaleResolverTest {

    private static final int THREADS = 16;
    private static final long TIMEOUT_SECONDS = 30;

    /** A distinct header per index, with several ranges so that parsing is not instantaneous. */
    private static String distinctHeader(int i) {
        StringBuilder sb = new StringBuilder("fr;q=0.5, de;q=0.9, en;q=0.2");
        for (int k = 0; k < 20; k++) {
            sb.append(", x-").append(i).append('-').append(k).append(";q=0.1");
        }
        return sb.toString();
    }

    private static Thread daemon(Runnable r) {
        Thread t = new Thread(r, "resolver-test");
        t.setDaemon(true);
        return t;
    }

    @Test
    @DisplayName("distinct headers far beyond the bound resolve concurrently without blocking or throwing")
    void distinctHeadersBeyondBoundResolveConcurrently() throws Exception {
        DefaultLocaleResolver resolver = new DefaultLocaleResolver(Locale.ENGLISH);
        int perThread = DefaultLocaleResolver.MAX_CACHE_SIZE * 8;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS, DefaultLocaleResolverTest::daemon);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                final int base = t * perThread;
                futures.add(pool.submit(() -> {
                    start.await();
                    int wrong = 0;
                    for (int i = 0; i < perThread; i++) {
                        Locale l = resolver.resolve(distinctHeader(base + i), (String) null);
                        if (!"de".equals(l.getLanguage())) {
                            wrong++;
                        }
                    }
                    return wrong;
                }));
            }
            start.countDown();
            for (Future<Integer> f : futures) {
                assertThat(f.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isZero();
            }
        } catch (TimeoutException e) {
            throw new AssertionError("resolution did not complete within " + TIMEOUT_SECONDS + "s", e);
        } catch (ExecutionException e) {
            throw new AssertionError("resolution threw", e.getCause());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("the cache stays within its bound plus the number of concurrent callers")
    void cacheStaysWithinBoundPlusCallers() throws Exception {
        DefaultLocaleResolver resolver = new DefaultLocaleResolver(Locale.ENGLISH);
        int perThread = DefaultLocaleResolver.MAX_CACHE_SIZE;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS, DefaultLocaleResolverTest::daemon);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                final int base = t * perThread;
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        resolver.resolve(distinctHeader(base + i), (String) null);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(resolver.cachedHeaderCount())
                .isLessThanOrEqualTo(DefaultLocaleResolver.MAX_CACHE_SIZE + THREADS);
        assertThat(resolver.cachedHeaderCount()).isGreaterThanOrEqualTo(DefaultLocaleResolver.MAX_CACHE_SIZE);
    }

    @Test
    @DisplayName("a header first seen when the cache is full resolves correctly and is not admitted")
    void headerSeenWhenCacheIsFullStillResolves() {
        DefaultLocaleResolver resolver = new DefaultLocaleResolver(Locale.ENGLISH);
        for (int i = 0; i < DefaultLocaleResolver.MAX_CACHE_SIZE; i++) {
            resolver.resolve(distinctHeader(i), (String) null);
        }
        assertThat(resolver.cachedHeaderCount()).isEqualTo(DefaultLocaleResolver.MAX_CACHE_SIZE);

        String fresh = "pl;q=0.8, es;q=0.95, it;q=0.1";
        assertThat(resolver.resolve(fresh, (String) null).getLanguage()).isEqualTo("es");
        assertThat(resolver.resolve(fresh, (String) null).getLanguage()).isEqualTo("es");
        assertThat(resolver.cachedHeaderCount()).isEqualTo(DefaultLocaleResolver.MAX_CACHE_SIZE);

        // Entries admitted before the cache filled still resolve.
        assertThat(resolver.resolve(distinctHeader(0), (String) null).getLanguage()).isEqualTo("de");
    }

    private static String resolve(String header) {
        Locale l = new DefaultLocaleResolver(Locale.ENGLISH).resolve(header, (String) null, Locale.JAPANESE);
        return l.getLanguage();
    }

    @Test
    @DisplayName("a range with a malformed q is dropped and does not outrank a valid one")
    void malformedQIsDropped() {
        assertThat(resolve("en;q=abc, pl;q=0.9")).isEqualTo("pl");
        assertThat(resolve("en;q=, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=0.1234, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=1.5, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=.5, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=1e0, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=0x1p0, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=0.5d, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=abc")).isEqualTo("ja");
    }

    @Test
    @DisplayName("a q greater than 1 is dropped")
    void qGreaterThanOneIsDropped() {
        assertThat(resolve("en;q=2, pl;q=0.5")).isEqualTo("pl");
        assertThat(resolve("en;q=1.001, pl;q=0.5")).isEqualTo("pl");
        assertThat(resolve("en;q=10, pl;q=0.5")).isEqualTo("pl");
    }

    @Test
    @DisplayName("a negative q is dropped")
    void negativeQIsDropped() {
        assertThat(resolve("en;q=-0.5, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=-1, pl;q=0.1")).isEqualTo("pl");
    }

    @Test
    @DisplayName("NaN and Infinity text is dropped")
    void nonFiniteQIsDropped() {
        assertThat(resolve("en;q=NaN, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=Infinity, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=-Infinity, pl;q=0.1")).isEqualTo("pl");
    }

    @Test
    @DisplayName("q=0 means not acceptable and is dropped")
    void zeroQIsNotAcceptable() {
        assertThat(resolve("en;q=0, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=0.000, pl;q=0.1")).isEqualTo("pl");
        assertThat(resolve("en;q=0")).isEqualTo("ja");
    }

    @Test
    @DisplayName("valid q values weight ranges, including the boundary forms")
    void validQWeightsRanges() {
        assertThat(resolve("fr;q=0.5, de;q=0.9, en;q=0.2")).isEqualTo("de");
        assertThat(resolve("fr;q=0.001, de;q=0.002")).isEqualTo("de");
        assertThat(resolve("fr;q=0.999, de;q=1")).isEqualTo("de");
        assertThat(resolve("fr;q=1.000, de;q=0.9")).isEqualTo("fr");
        assertThat(resolve("fr;Q=0.2, de;q=0.3")).isEqualTo("de");
        assertThat(resolve("fr;q=0.5, de;q=0.5")).isEqualTo("fr");
    }
}
