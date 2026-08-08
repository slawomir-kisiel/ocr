package pl.sk.ocr.core.batch;

import java.nio.file.Path;
import pl.sk.ocr.domain.result.DocumentResult;

public record BatchItemResult(DocumentJob job, DocumentResult result, long processingDurationMs, Path finalLocation) {
    public BatchItemResult {
        if (job == null) {
            throw new IllegalArgumentException("job is required");
        }
        if (result == null) {
            throw new IllegalArgumentException("result is required");
        }
        if (processingDurationMs < 0) {
            throw new IllegalArgumentException("processing duration must be >= 0");
        }
    }
}
