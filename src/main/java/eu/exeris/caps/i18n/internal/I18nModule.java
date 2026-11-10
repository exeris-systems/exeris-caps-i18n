package eu.exeris.caps.i18n.internal;

import eu.exeris.caps.i18n.api.LocaleResolver;
import eu.exeris.caps.i18n.api.TranslationRegistry;

/**
 * ServiceBoundary capability lifecycle class in internal package providing LocaleResolver and TranslationRegistry.
 */
public final class I18nModule extends eu.exeris.caps.i18n.I18nModule {

    public I18nModule() {
        super();
    }

    public I18nModule(LocaleResolver localeResolver, TranslationRegistry translationRegistry) {
        super(localeResolver, translationRegistry);
    }
}
