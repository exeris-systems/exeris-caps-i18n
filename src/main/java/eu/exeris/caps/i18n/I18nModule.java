package eu.exeris.caps.i18n;

import eu.exeris.caps.i18n.api.LocaleResolver;
import eu.exeris.caps.i18n.api.TranslationRegistry;
import eu.exeris.caps.i18n.internal.DefaultLocaleResolver;
import eu.exeris.caps.i18n.internal.DefaultTranslationRegistry;
import eu.exeris.sdk.annotation.capability.CapabilityLifecycle;
import eu.exeris.sdk.annotation.capability.CapabilityModule;
import eu.exeris.sdk.annotation.capability.Provides;
import eu.exeris.sdk.composition.lifecycle.CapabilityLifecycleHooks;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Capability declaration and lifecycle owner for {@code exeris-caps-i18n}.
 *
 * <p>Provides multi-lingual support, locale resolution, and message registries.
 *
 * @since 0.1.0
 */
@CapabilityModule
@CapabilityLifecycle
@Provides(service = LocaleResolver.class, version = "0.1.0")
@Provides(service = TranslationRegistry.class, version = "0.1.0")
public class I18nModule implements CapabilityLifecycleHooks {

    private final LocaleResolver localeResolver;
    private final TranslationRegistry translationRegistry;
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public I18nModule() {
        this(new DefaultLocaleResolver(), new DefaultTranslationRegistry());
    }

    public I18nModule(LocaleResolver localeResolver, TranslationRegistry translationRegistry) {
        this.localeResolver = Objects.requireNonNull(localeResolver, "localeResolver");
        this.translationRegistry = Objects.requireNonNull(translationRegistry, "translationRegistry");
    }

    public LocaleResolver localeResolver() {
        return localeResolver;
    }

    public TranslationRegistry translationRegistry() {
        return translationRegistry;
    }

    @Override
    public void initialize() throws Exception {
    }

    @Override
    public void ready() throws Exception {
        ready.set(true);
    }

    @Override
    public void drain(Duration remaining) throws Exception {
        ready.set(false);
    }

    @Override
    public void terminate() {
        ready.set(false);
    }

    public boolean isReady() {
        return ready.get();
    }
}
