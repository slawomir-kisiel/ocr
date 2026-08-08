package pl.sk.ocr.core.output;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.ProcessingStatus;

public final class BatchSummaryBuilder {
    public BatchSummary build(List<DocumentResult> results) {
        var issueCounts = results.stream()
            .flatMap(result -> allIssues(result).stream())
            .collect(Collectors.groupingBy(issue -> issue.code().value(), java.util.TreeMap::new, Collectors.counting()));
        return new BatchSummary(
            "1.0",
            results.size(),
            count(results, ProcessingStatus.SUCCESS),
            count(results, ProcessingStatus.WARNING),
            count(results, ProcessingStatus.FAILED) + count(results, ProcessingStatus.FATAL),
            issueCounts,
            results.stream().map(result -> result.documentId().value()).toList()
        );
    }

    private int count(List<DocumentResult> results, ProcessingStatus status) {
        return (int) results.stream().filter(result -> result.status() == status).count();
    }

    private List<ProcessingIssue> allIssues(DocumentResult result) {
        return Stream.concat(
            result.issues().stream(),
            result.fields().stream().flatMap(field -> field.issues().stream())
        ).toList();
    }
}
