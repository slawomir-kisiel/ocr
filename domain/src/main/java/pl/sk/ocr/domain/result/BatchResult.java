package pl.sk.ocr.domain.result;

import java.util.List;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.identifier.BatchId;
import pl.sk.ocr.domain.issue.ProcessingIssue;

public record BatchResult(
    BatchId batchId,
    ProcessingStatus status,
    List<DocumentResult> documents,
    List<ProcessingIssue> issues
) {
    public BatchResult {
        batchId = Validation.requireNonNull(batchId, "batch id");
        status = Validation.requireNonNull(status, "status");
        documents = List.copyOf(Validation.requireNoNulls(documents == null ? List.of() : documents, "documents"));
        issues = List.copyOf(Validation.requireNoNulls(issues == null ? List.of() : issues, "issues"));
    }

    public static BatchResult from(BatchId batchId, List<DocumentResult> documents, List<ProcessingIssue> issues) {
        var statuses = new java.util.ArrayList<ProcessingStatus>();
        if (documents != null) {
            statuses.addAll(documents.stream().map(DocumentResult::status).toList());
        }
        if (issues != null) {
            statuses.addAll(issues.stream().map(issue -> switch (issue.severity()) {
                case FATAL -> ProcessingStatus.FATAL;
                case ERROR -> ProcessingStatus.FAILED;
                case WARNING -> ProcessingStatus.WARNING;
            }).toList());
        }
        return new BatchResult(batchId, ProcessingStatus.aggregate(statuses), documents, issues);
    }
}
