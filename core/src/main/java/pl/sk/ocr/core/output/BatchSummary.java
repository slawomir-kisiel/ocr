package pl.sk.ocr.core.output;

import java.util.List;
import java.util.Map;

public record BatchSummary(
    String schemaVersion,
    int totalDocuments,
    int successDocuments,
    int warningDocuments,
    int failedDocuments,
    Map<String, Long> issueCounts,
    List<String> files
) {
    public BatchSummary {
        issueCounts = Map.copyOf(issueCounts == null ? Map.of() : issueCounts);
        files = List.copyOf(files == null ? List.of() : files);
    }
}
