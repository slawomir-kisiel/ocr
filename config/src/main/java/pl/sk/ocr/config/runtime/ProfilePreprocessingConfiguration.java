package pl.sk.ocr.config.runtime;

import java.util.List;

public record ProfilePreprocessingConfiguration(List<ExtensionRef> imageProcessors) {
    public ProfilePreprocessingConfiguration {
        imageProcessors = List.copyOf(imageProcessors == null ? List.of() : imageProcessors);
    }

    public static ProfilePreprocessingConfiguration empty() {
        return new ProfilePreprocessingConfiguration(List.of());
    }
}
