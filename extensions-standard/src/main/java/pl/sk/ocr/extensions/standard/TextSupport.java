package pl.sk.ocr.extensions.standard;

import java.text.Normalizer;
import java.util.Locale;

public final class TextSupport {
    private TextSupport() {
    }

    public static String normalize(String value) {
        var text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return text.replaceAll("\\s+", " ");
    }

    public static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    public static int intParameter(pl.sk.ocr.extension.api.ExtensionParameters parameters, String name, int fallback) {
        return parameters.get(name)
            .map(value -> value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString()))
            .orElse(fallback);
    }

    public static double doubleParameter(pl.sk.ocr.extension.api.ExtensionParameters parameters, String name, double fallback) {
        return parameters.get(name)
            .map(value -> value instanceof Number number ? number.doubleValue() : Double.parseDouble(value.toString()))
            .orElse(fallback);
    }
}

