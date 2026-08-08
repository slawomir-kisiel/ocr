package pl.sk.ocr.domain.issue;

import java.util.Map;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.identifier.AnchorId;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.ExtensionId;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.identifier.PageNumber;

public record ProcessingIssue(
    IssueCode code,
    Severity severity,
    ErrorScope scope,
    ProcessingStage stage,
    String message,
    CategoryId categoryId,
    AnchorId anchorId,
    FieldId fieldId,
    ExtensionId extensionId,
    PageNumber page,
    Map<String, Object> context
) {
    public ProcessingIssue {
        code = Validation.requireNonNull(code, "issue code");
        severity = Validation.requireNonNull(severity, "severity");
        scope = Validation.requireNonNull(scope, "scope");
        stage = Validation.requireNonNull(stage, "stage");
        message = Validation.requireText(message, "message");
        context = Map.copyOf(context == null ? Map.of() : context);
    }

    public static ProcessingIssue error(IssueCode code, ErrorScope scope, ProcessingStage stage, String message) {
        return new ProcessingIssue(code, Severity.ERROR, scope, stage, message, null, null, null, null, null, Map.of());
    }
}
