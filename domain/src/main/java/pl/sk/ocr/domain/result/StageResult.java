package pl.sk.ocr.domain.result;

import java.util.List;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;

public record StageResult(ProcessingStage stage, ProcessingStatus status, List<ProcessingIssue> issues) {
    public StageResult {
        stage = Validation.requireNonNull(stage, "stage");
        status = Validation.requireNonNull(status, "status");
        issues = List.copyOf(Validation.requireNoNulls(issues == null ? List.of() : issues, "issues"));
    }
}
