package io.timparsons.dropwizard.views.config.mustache;

import java.util.Locale;

import com.google.common.collect.Table;

import io.timparsons.dropwizard.views.config.LocaleConfiguration;
import io.timparsons.dropwizard.views.config.LocaleConfigurationUtility;
import io.timparsons.dropwizard.views.config.LocaleMap;

public class MustacheLocaleConfiguration implements LocaleConfiguration {

    private Table<String, String, LocaleMap> localeTable;
    private Locale defaultLocale = Locale.getDefault();

    public MustacheLocaleConfiguration(String directory, String defaultLocale) {
        this.localeTable = LocaleConfigurationUtility.getLocaleFiles(directory);
        if (defaultLocale != null) {
            this.defaultLocale = Locale.forLanguageTag(defaultLocale);
        }
    }

    @Override
    public LocaleMap getLocaleBundle(Locale locale, String bundle) {
        if (localeTable.contains(locale.getLanguage(), bundle)) {
            return localeTable.get(locale.getLanguage(), bundle);
        } else {
            return localeTable.get(defaultLocale.getLanguage(), bundle);
        }
    }

}
