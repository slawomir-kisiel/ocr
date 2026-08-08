package pl.sk.ocr.domain.result;

import java.util.List;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.issue.ProcessingIssue;

public record FieldResult(FieldId fieldId, String value, ProcessingStatus status, List<ProcessingIssue> issues) {
    public FieldResult {
        fieldId = Validation.requireNonNull(fieldId, "field id");
        status = Validation.requireNonNull(status, "status");
        issues = List.copyOf(Validation.requireNoNulls(issues == null ? List.of() : issues, "issues"));
    }
}
