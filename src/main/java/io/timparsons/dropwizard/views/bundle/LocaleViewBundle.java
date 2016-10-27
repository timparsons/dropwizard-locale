package io.timparsons.dropwizard.views.bundle;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewConfigurable;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.ViewRenderer;
import io.timparsons.dropwizard.views.freemarker.FreemarkerLocaleViewRenderer;
import io.timparsons.dropwizard.views.mustache.MustacheLocaleViewRenderer;

public class LocaleViewBundle<T extends Configuration> implements ConfiguredBundle<T>, ViewConfigurable<T> {
    private FreemarkerLocaleViewRenderer freemarkerLocaleViewRenderer;
    private MustacheLocaleViewRenderer mustacheLocaleViewRenderer;
    private final Iterable<ViewRenderer> viewRenderers;

    public LocaleViewBundle() {
        List<ViewRenderer> renderers = new ArrayList<>();
        try {
            this.freemarkerLocaleViewRenderer = new FreemarkerLocaleViewRenderer();
            renderers.add(freemarkerLocaleViewRenderer);
        } catch (NoClassDefFoundError e) {
        }
        try {
            this.mustacheLocaleViewRenderer = new MustacheLocaleViewRenderer();
            renderers.add(mustacheLocaleViewRenderer);
        } catch (NoClassDefFoundError e) {
        }
        
        this.viewRenderers = ImmutableSet.copyOf(renderers);
    }

    public LocaleViewBundle(Iterable<ViewRenderer> viewRenderers) {
        this.viewRenderers = ImmutableSet.copyOf(viewRenderers);
    }

    @Override
    public Map<String, Map<String, String>> getViewConfiguration(T configuration) {
        return ImmutableMap.of();
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        final Map<String, Map<String, String>> options = getViewConfiguration(configuration);
        for (ViewRenderer viewRenderer : viewRenderers) {
            final Map<String, String> viewOptions = options.get(viewRenderer.getSuffix());
            viewRenderer.configure(firstNonNull(viewOptions, Collections.emptyMap()));
        }
        environment.jersey().register(new ViewMessageBodyWriter(environment.metrics(), viewRenderers));
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // nothing doing
    }
    
    public final FreemarkerLocaleViewRenderer getFreemarkerLocaleViewRenderer() {
        return freemarkerLocaleViewRenderer;
    }
    
    public final MustacheLocaleViewRenderer getMustacheLocaleViewRenderer() {
        return mustacheLocaleViewRenderer;
    }
}
