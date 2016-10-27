package io.timparsons.dropwizard.views.config;

import java.util.Locale;

public interface LocaleConfiguration {

    LocaleMap getLocaleBundle(Locale locale, String bundle);
}
