package pl.sk.ocr.core.batch;

import java.nio.file.Path;
import java.util.Optional;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;

public record BatchOptions(
    RuntimeConfiguration configuration,
    Integer workersOverride,
    Path outputOverride,
    Path summaryOutput
) {
    public BatchOptions {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration is required");
        }
    }

    public int workers() {
        var configured = configuration.profile().processing().workers();
        var workers = workersOverride == null ? configured : workersOverride;
        if (workers < 1) {
            throw new IllegalArgumentException("workers must be >= 1");
        }
        return workers;
    }

    public Path outputFile() {
        return outputOverride == null ? configuration.profile().csvOutput().file() : outputOverride;
    }

    public Optional<Path> summaryOutputFile() {
        return Optional.ofNullable(summaryOutput);
    }
}
