package io.timparsons.dropwizard.views.freemarker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;
import io.timparsons.dropwizard.views.LocaleView;
import io.timparsons.dropwizard.views.config.LocaleConfigurationUtility;
import io.timparsons.dropwizard.views.config.LocaleMap;
import io.timparsons.dropwizard.views.config.freemarker.FreemarkerLocaleConfiguration;

public class FreemarkerLocaleViewRenderer implements ViewRenderer {

    private static final Version FREEMARKER_VERSION = FreemarkerLocaleConfiguration.getVersion();
    private final TemplateLoader loader;
    private final LocaleLoader localeLoader;

    private class TemplateLoader extends CacheLoader<Class<?>, FreemarkerLocaleConfiguration> {
        private Map<String, String> baseConfig = ImmutableMap.of();

        @Override
        public FreemarkerLocaleConfiguration load(Class<?> key) throws Exception {
            final FreemarkerLocaleConfiguration configuration = new FreemarkerLocaleConfiguration(FREEMARKER_VERSION);
            configuration.setObjectWrapper(new DefaultObjectWrapperBuilder(FREEMARKER_VERSION).build());
            configuration.loadBuiltInEncodingMap();
            configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
            configuration.setClassForTemplateLoading(key, "/");
            for (Map.Entry<String, String> entry : baseConfig.entrySet()) {
                configuration.setSetting(entry.getKey(), entry.getValue());
            }
            return configuration;
        }

        void setBaseConfig(Map<String, String> baseConfig) {
            this.baseConfig = baseConfig;
        }
    }

    private class LocaleLoader extends CacheLoader<Pair<Class<? extends LocaleView>, Locale>, LocaleMap> {

        @Override
        public LocaleMap load(Pair<Class<? extends LocaleView>, Locale> key) throws Exception {
            final FreemarkerLocaleConfiguration configuration = configurationCache.getUnchecked(key.getLeft());

            Class<? extends LocaleView> localeViewClass = key.getLeft();
            List<String> viewBundles = LocaleConfigurationUtility.getViewBundles(localeViewClass);

            if (!viewBundles.isEmpty()) {
                LocaleMap.Builder localeBundlesBuilder = LocaleMap.builder();
                for (String bundle : viewBundles) {
                    localeBundlesBuilder.putAll(configuration.getLocaleBundle(key.getRight(), bundle));
                }

                LocaleMap viewLocaleBundles = localeBundlesBuilder.build();

                return viewLocaleBundles;
            } else {
                return null;
            }
        }
    }

    private LoadingCache<Class<?>, FreemarkerLocaleConfiguration> configurationCache;
    private LoadingCache<Pair<Class<? extends LocaleView>, Locale>, LocaleMap> bundleCache;

    public FreemarkerLocaleViewRenderer() {
        this.loader = new TemplateLoader();
        this.localeLoader = new LocaleLoader();

    }

    @Override
    public boolean isRenderable(View view) {
        return view.getTemplateName().endsWith(getSuffix());
    }

    @Override
    public void render(View view, Locale locale, OutputStream output) throws IOException {

        try {
            Template template = null;
            final FreemarkerLocaleConfiguration configuration = configurationCache.getUnchecked(view.getClass());
            final Charset charset = view.getCharset()
                    .orElseGet(() -> Charset.forName(configuration.getEncoding(locale)));
            if (LocaleView.class.isInstance(view)) {
                @SuppressWarnings("unchecked")
                Class<LocaleView> viewClass = (Class<LocaleView>) view.getClass();
                final LocaleMap viewLocaleBundles = bundleCache
                        .getUnchecked(new ImmutablePair<Class<? extends LocaleView>, Locale>(viewClass, locale));

                ((LocaleView) view).setMessageBundle(viewLocaleBundles);

                template = configuration.getTemplate(view.getTemplateName(), charset.name());
            } else {
                template = configuration.getTemplate(view.getTemplateName(), locale, charset.name());
            }
            template.process(view, new OutputStreamWriter(output, template.getEncoding()));
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configure(Map<String, String> baseConfig) {
        this.loader.setBaseConfig(baseConfig);

        boolean devMode = false;

        if (baseConfig.containsKey("STAGE")) {
            devMode = baseConfig.get("STAGE").equalsIgnoreCase("DEVELOPMENT");
        }

        CacheBuilder configBuilder = CacheBuilder.newBuilder().concurrencyLevel(128);
        if (devMode) {
            configBuilder.expireAfterWrite(1L, TimeUnit.SECONDS);
        }
        this.configurationCache = configBuilder.build(loader);

        CacheBuilder bundleBuilder = CacheBuilder.newBuilder().concurrencyLevel(128);
        if (devMode) {
            bundleBuilder.expireAfterWrite(1L, TimeUnit.SECONDS);
        }
        this.bundleCache = bundleBuilder.build(localeLoader);
    }

    @Override
    public String getSuffix() {
        return ".ftl";
    }

    public void clearCache() {
        configurationCache.invalidateAll();
        bundleCache.invalidateAll();
    }

}
