package com.serveat.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MapperUtils {
    private MapperUtils() {}

    public static String formatEuro(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.of("es", "ES")).format(value);
    }
}