package com.serveat.core.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Component
public class ServeatI18nProvider implements I18NProvider {

    public static final String BUNDLE_PREFIX = "i18n.messages";

    private static final List<Locale> LOCALES = List.of(
            new Locale("es", "ES"),
            Locale.ENGLISH
    );

    @Override
    public List<Locale> getProvidedLocales() {
        return LOCALES;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale);
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }
}
