package pl.sk.ocr.config.runtime;

import java.util.List;

public record RuntimeConfiguration(ProfileRuntimeConfiguration profile, List<CategoryRuntimeConfiguration> categories) {
    public RuntimeConfiguration {
        categories = List.copyOf(categories == null ? List.of() : categories);
    }
}
