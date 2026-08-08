package pl.sk.ocr.domain.result;

import java.util.List;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.trace.ProcessingTrace;

public record DocumentResult(
    DocumentId documentId,
    CategoryId categoryId,
    ProcessingStatus status,
    List<FieldResult> fields,
    List<ProcessingIssue> issues,
    ProcessingTrace trace
) {
    public DocumentResult {
        documentId = Validation.requireNonNull(documentId, "document id");
        status = Validation.requireNonNull(status, "status");
        fields = List.copyOf(Validation.requireNoNulls(fields == null ? List.of() : fields, "fields"));
        issues = List.copyOf(Validation.requireNoNulls(issues == null ? List.of() : issues, "issues"));
    }

    public static DocumentResult from(DocumentId documentId, CategoryId categoryId, List<FieldResult> fields,
                                      List<ProcessingIssue> issues, ProcessingTrace trace) {
        var fieldStatuses = fields == null ? List.<ProcessingStatus>of() : fields.stream().map(FieldResult::status).toList();
        var issueStatuses = issues == null ? List.<ProcessingStatus>of() : issues.stream()
            .map(issue -> switch (issue.severity()) {
                case FATAL -> ProcessingStatus.FATAL;
                case ERROR -> ProcessingStatus.FAILED;
                case WARNING -> ProcessingStatus.WARNING;
            })
            .toList();
        var statuses = new java.util.ArrayList<ProcessingStatus>();
        statuses.addAll(fieldStatuses);
        statuses.addAll(issueStatuses);
        return new DocumentResult(documentId, categoryId, ProcessingStatus.aggregate(statuses), fields, issues, trace);
    }
}
