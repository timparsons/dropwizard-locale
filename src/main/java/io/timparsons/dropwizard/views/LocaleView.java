package io.timparsons.dropwizard.views;

import java.nio.charset.Charset;

import io.dropwizard.views.View;
import io.timparsons.dropwizard.views.config.LocaleMap;

public abstract class LocaleView extends View {

    private LocaleMap messageBundle;

    public LocaleView(String templateName, Charset charset) {
        super(templateName, charset);
    }

    public LocaleView(String templateName) {
        super(templateName);
    }

    public LocaleMap getMessageBundle() {
        return messageBundle;
    }

    public void setMessageBundle(LocaleMap messageBundle) {
        this.messageBundle = messageBundle;
    }

}
