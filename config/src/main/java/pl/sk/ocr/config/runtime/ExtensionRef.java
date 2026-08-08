package pl.sk.ocr.config.runtime;

import java.util.Map;
import pl.sk.ocr.domain.identifier.ExtensionId;

public record ExtensionRef(ExtensionId id, Map<String, Object> parameters) {
    public ExtensionRef {
        parameters = Map.copyOf(parameters == null ? Map.of() : parameters);
    }
}
