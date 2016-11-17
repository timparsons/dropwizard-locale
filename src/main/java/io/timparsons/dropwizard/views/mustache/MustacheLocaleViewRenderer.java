package io.timparsons.dropwizard.views.mustache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheResolver;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;
import io.timparsons.dropwizard.views.LocaleView;
import io.timparsons.dropwizard.views.config.LocaleConfigurationUtility;
import io.timparsons.dropwizard.views.config.LocaleMap;
import io.timparsons.dropwizard.views.config.mustache.MustacheLocaleConfiguration;

public class MustacheLocaleViewRenderer implements ViewRenderer {
    private final LocaleLoader localeLoader;

    private LoadingCache<Class<? extends View>, MustacheFactory> factories;
    private LoadingCache<Pair<Class<? extends LocaleView>, Locale>, LocaleMap> bundleCache;

    private class LocaleLoader extends CacheLoader<Pair<Class<? extends LocaleView>, Locale>, LocaleMap> {
        private MustacheLocaleConfiguration config;

        @Override
        public LocaleMap load(Pair<Class<? extends LocaleView>, Locale> key) throws Exception {
            Class<? extends LocaleView> localeViewClass = key.getLeft();
            List<String> viewBundles = LocaleConfigurationUtility.getViewBundles(localeViewClass);

            if (!viewBundles.isEmpty()) {
                LocaleMap.Builder localeBundlesBuilder = LocaleMap.builder();
                for (String bundle : viewBundles) {
                    localeBundlesBuilder.putAll(config.getLocaleBundle(key.getRight(), bundle));
                }

                LocaleMap viewLocaleBundles = localeBundlesBuilder.build();

                return viewLocaleBundles;
            } else {
                return null;
            }
        }

        public void setConfig(MustacheLocaleConfiguration config) {
            this.config = config;
        }
    }

    public MustacheLocaleViewRenderer() {
        this.factories = CacheBuilder.newBuilder().build(new CacheLoader<Class<? extends View>, MustacheFactory>() {
            @Override
            public MustacheFactory load(Class<? extends View> key) throws Exception {
                return new DefaultMustacheFactory(new PerClassMustacheResolver(key));
            }
        });

        localeLoader = new LocaleLoader();
    }

    @Override
    public boolean isRenderable(View view) {
        return view.getTemplateName().endsWith(getSuffix());
    }

    @Override
    public void render(View view, Locale locale, OutputStream output) throws IOException {
        try {
            if (LocaleView.class.isInstance(view)) {
                @SuppressWarnings("unchecked")
                Class<LocaleView> viewClass = (Class<LocaleView>) view.getClass();
                final LocaleMap viewLocaleBundles = bundleCache
                        .getUnchecked(new ImmutablePair<Class<? extends LocaleView>, Locale>(viewClass, locale));

                ((LocaleView) view).setMessageBundle(viewLocaleBundles);
            }
            final Mustache template = factories.get(view.getClass()).compile(view.getTemplateName());
            final Charset charset = view.getCharset().orElse(StandardCharsets.UTF_8);
            try (OutputStreamWriter writer = new OutputStreamWriter(output, charset)) {
                template.execute(writer, view);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Mustache template error: " + view.getTemplateName(), e);
        }
    }

    @Override
    public void configure(Map<String, String> baseConfig) {
        localeLoader
                .setConfig(new MustacheLocaleConfiguration(baseConfig.get("locale"), baseConfig.get("defaultLocale")));

        boolean devMode = false;

        if (baseConfig.containsKey("STAGE")) {
            devMode = baseConfig.get("STAGE").equalsIgnoreCase("DEVELOPMENT");
        }

        CacheBuilder factoriesBuilder = CacheBuilder.newBuilder().concurrencyLevel(128);
        if (devMode) {
            factoriesBuilder.expireAfterWrite(1L, TimeUnit.SECONDS);
        }
        this.factories = factoriesBuilder.build(new CacheLoader<Class<? extends View>, MustacheFactory>() {
            @Override
            public MustacheFactory load(Class<? extends View> key) throws Exception {
                return new DefaultMustacheFactory(new PerClassMustacheResolver(key));
            }
        });

        CacheBuilder bundleBuilder = CacheBuilder.newBuilder().concurrencyLevel(128);
        if (devMode) {
            bundleBuilder.expireAfterWrite(1L, TimeUnit.SECONDS);
        }
        this.bundleCache = bundleBuilder.build(localeLoader);
    }

    @Override
    public String getSuffix() {
        return ".mustache";
    }

    public void clearCache() {
        factories.invalidateAll();
        bundleCache.invalidateAll();
    }

    class PerClassMustacheResolver implements MustacheResolver {
        private final Class<? extends View> klass;

        PerClassMustacheResolver(Class<? extends View> klass) {
            this.klass = klass;
        }

        @Override
        public Reader getReader(String resourceName) {
            final InputStream is = klass.getResourceAsStream(resourceName);
            if (is == null) {
                return null;
            }
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        }
    }
}
