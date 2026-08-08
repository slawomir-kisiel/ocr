package pl.sk.ocr.extension.api;

import java.util.Map;
import java.util.Optional;

public interface ExtensionParameters {
    Optional<Object> get(String name);

    Map<String, Object> asMap();

    default Optional<String> getString(String name) {
        return get(name).map(String.class::cast);
    }

    static ExtensionParameters empty() {
        return of(Map.of());
    }

    static ExtensionParameters of(Map<String, Object> values) {
        var snapshot = Map.copyOf(values == null ? Map.of() : values);
        return new ExtensionParameters() {
            @Override
            public Optional<Object> get(String name) {
                return Optional.ofNullable(snapshot.get(name));
            }

            @Override
            public Map<String, Object> asMap() {
                return snapshot;
            }
        };
    }
}
