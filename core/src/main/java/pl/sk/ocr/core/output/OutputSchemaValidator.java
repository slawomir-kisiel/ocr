package pl.sk.ocr.core.output;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.domain.issue.IssueCode;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.issue.ErrorScope;
import pl.sk.ocr.domain.issue.Severity;

public final class OutputSchemaValidator {
    private static final Pattern COLUMN_NAME = Pattern.compile("[a-zA-Z0-9_]+");

    public List<ProcessingIssue> validate(RuntimeConfiguration configuration) {
        var issues = new ArrayList<ProcessingIssue>();
        var seen = new HashSet<String>();
        for (var category : configuration.categories()) {
            for (var field : category.fields()) {
                if (!field.exported()) {
                    continue;
                }
                var column = field.columnName();
                if (column == null || column.isBlank() || !COLUMN_NAME.matcher(column).matches()) {
                    issues.add(issue("OUTPUT_SCHEMA_INVALID", "Invalid output column name: " + column));
                }
                seen.add(column);
            }
        }
        if (seen.isEmpty()) {
            issues.add(issue("OUTPUT_SCHEMA_INVALID", "Output schema has no business columns"));
        }
        return issues;
    }

    private ProcessingIssue issue(String code, String message) {
        return new ProcessingIssue(
            new IssueCode(code),
            Severity.FATAL,
            ErrorScope.OUTPUT,
            ProcessingStage.OUTPUT_WRITING,
            message,
            null,
            null,
            null,
            null,
            null,
            java.util.Map.of()
        );
    }
}
