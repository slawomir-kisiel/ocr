package pl.sk.ocr.core.batch;

import java.nio.file.Path;

public record DocumentJob(int sequence, Path source) {
    public DocumentJob {
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be >= 0");
        }
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
    }
}
