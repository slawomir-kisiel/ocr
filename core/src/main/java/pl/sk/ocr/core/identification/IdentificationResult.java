package pl.sk.ocr.core.identification;

import java.util.List;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;

public record IdentificationResult(IdentificationStatus status, CategoryRuntimeConfiguration category, List<CategoryRuntimeConfiguration> matches) {
    public IdentificationResult {
        matches = List.copyOf(matches == null ? List.of() : matches);
    }
}
