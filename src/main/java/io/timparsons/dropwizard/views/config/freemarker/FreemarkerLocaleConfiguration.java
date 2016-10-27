package io.timparsons.dropwizard.views.config.freemarker;

import java.util.Locale;

import com.google.common.collect.Table;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import io.timparsons.dropwizard.views.config.LocaleConfiguration;
import io.timparsons.dropwizard.views.config.LocaleConfigurationUtility;
import io.timparsons.dropwizard.views.config.LocaleMap;

public class FreemarkerLocaleConfiguration extends Configuration implements LocaleConfiguration {

    private Table<String, String, LocaleMap> localeTable;

    public FreemarkerLocaleConfiguration(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    @Override
    public void setSetting(String name, String value) throws TemplateException {
        if (name.equals("locale")) {
            localeTable = LocaleConfigurationUtility.getLocaleFiles(value);
        } else if (!name.equals("STAGE")) {
            super.setSetting(name, value);
        }
    }

    @Override
    public LocaleMap getLocaleBundle(Locale locale, String bundle) {
        return localeTable.get(locale.getLanguage(), bundle);
    }
}
