package pl.sk.ocr.domain;

import java.util.Collection;

public final class Validation {
    private Validation() {
    }

    public static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    public static <T> Collection<T> requireNoNulls(Collection<T> values, String name) {
        requireNonNull(values, name);
        if (values.stream().anyMatch(v -> v == null)) {
            throw new IllegalArgumentException(name + " must not contain nulls");
        }
        return values;
    }
}
