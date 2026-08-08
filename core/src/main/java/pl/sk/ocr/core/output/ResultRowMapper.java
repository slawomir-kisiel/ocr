package pl.sk.ocr.core.output;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.Severity;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.ProcessingStatus;

public final class ResultRowMapper {
    public ResultRow map(DocumentResult result, OutputSchema schema, long processingDurationMs) {
        var values = new LinkedHashMap<String, String>();
        schema.columnNames().forEach(column -> values.put(column, ""));
        values.put("fileName", result.documentId().value());
        values.put("categoryId", result.categoryId() == null ? "" : result.categoryId().value());
        values.put("documentStatus", outputStatus(result.status()));
        var issues = allIssues(result);
        values.put("errorCodes", codes(issues, Severity.ERROR, Severity.FATAL));
        values.put("warningCodes", codes(issues, Severity.WARNING));
        values.put("processingDurationMs", Long.toString(processingDurationMs));
        for (var field : result.fields()) {
            schema.columnNameFor(field.fieldId())
                .ifPresent(column -> values.put(column, field.value() == null ? "" : field.value()));
        }
        return new ResultRow(values);
    }

    private List<ProcessingIssue> allIssues(DocumentResult result) {
        return Stream.concat(
            result.issues().stream(),
            result.fields().stream().flatMap(field -> field.issues().stream())
        ).toList();
    }

    private String outputStatus(ProcessingStatus status) {
        return switch (status) {
            case SUCCESS -> "SUCCESS";
            case WARNING -> "SUCCESS_WITH_WARNINGS";
            case FAILED, FATAL -> "FAILED";
        };
    }

    private String codes(List<ProcessingIssue> issues, Severity... severities) {
        var accepted = java.util.Set.of(severities);
        var codes = new LinkedHashSet<String>();
        for (ProcessingIssue issue : issues) {
            if (accepted.contains(issue.severity())) {
                codes.add(issue.code().value());
            }
        }
        return String.join(";", codes);
    }
}
