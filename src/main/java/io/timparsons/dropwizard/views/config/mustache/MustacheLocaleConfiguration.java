package io.timparsons.dropwizard.views.config.mustache;

import java.util.Locale;

import com.google.common.collect.Table;

import io.timparsons.dropwizard.views.config.LocaleConfiguration;
import io.timparsons.dropwizard.views.config.LocaleConfigurationUtility;
import io.timparsons.dropwizard.views.config.LocaleMap;

public class MustacheLocaleConfiguration implements LocaleConfiguration {

    private Table<String, String, LocaleMap> localeTable;

    public MustacheLocaleConfiguration(String directory) {
        this.localeTable = LocaleConfigurationUtility.getLocaleFiles(directory);
    }

    @Override
    public LocaleMap getLocaleBundle(Locale locale, String bundle) {
        return localeTable.get(locale.getLanguage(), bundle);
    }

}
