package io.timparsons.dropwizard.views.writer;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderException;
import io.dropwizard.views.ViewRenderer;

@Provider
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML })
public class ViewMessageBodyWriter implements MessageBodyWriter<View> {
    public static final String TEMPLATE_ERROR_MSG = "<html>" + "<head><title>Template Error</title></head>"
            + "<body><h1>Template Error</h1><p>Something went wrong rendering the page</p></body>" + "</html>";

    @Context
    private HttpHeaders headers;

    private final Iterable<ViewRenderer> renderers;
    private final MetricRegistry metricRegistry;

    @Deprecated
    public ViewMessageBodyWriter(final MetricRegistry metricRegistry) {
        this(metricRegistry, ServiceLoader.load(ViewRenderer.class));
    }

    public ViewMessageBodyWriter(final MetricRegistry metricRegistry, final Iterable<ViewRenderer> viewRenderers) {
        this.metricRegistry = metricRegistry;
        this.renderers = viewRenderers;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return View.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final View t, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final View t, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException {
        final Timer.Context context = metricRegistry.timer(name(t.getClass(), "rendering")).time();
        try {
            for (ViewRenderer renderer : renderers) {
                if (renderer.isRenderable(t)) {
                    renderer.render(t, detectLocale(headers), entityStream);
                    return;
                }
            }
            throw new ViewRenderException("Unable to find a renderer for " + t.getTemplateName());
        } finally {
            context.stop();
        }
    }

    private Locale detectLocale(final HttpHeaders headers) {
        final List<Locale> languages = headers.getAcceptableLanguages();
        for (Locale locale : languages) {
            if (!locale.toString().contains("*")) { // Freemarker doesn't do wildcards well
                return locale;
            }
        }
        return Locale.getDefault();
    }
}
