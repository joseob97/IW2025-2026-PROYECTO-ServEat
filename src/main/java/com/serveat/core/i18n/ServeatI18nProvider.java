package com.serveat.core.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Component
public class ServeatI18nProvider implements I18NProvider {

    public static final String BUNDLE_PREFIX = "i18n/messages";

    private static final Locale LOCALE_ES = new Locale("es");
    private static final Locale LOCALE_EN = Locale.ENGLISH;

    private static final List<Locale> SUPPORTED_LOCALES = List.of(
            LOCALE_ES,
            LOCALE_EN
    );

    @Override
    public List<Locale> getProvidedLocales() {
        return SUPPORTED_LOCALES;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {

        if (key == null) {
            return "";
        }

        Locale effectiveLocale = locale != null ? locale : LOCALE_ES;

        try {
            ResourceBundle bundle =
                    ResourceBundle.getBundle(BUNDLE_PREFIX, effectiveLocale);

            if (!bundle.containsKey(key)) {
                return "!" + key + "!";
            }

            String value = bundle.getString(key);

            if (params.length > 0) {
                return String.format(value, params);
            }

            return value;

        } catch (Exception e) {
            return "!" + key + "!";
        }
    }
}
