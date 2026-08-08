package pl.sk.ocr.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.issue.ErrorScope;
import pl.sk.ocr.domain.issue.IssueCode;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.issue.Severity;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.FieldResult;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.trace.ProcessingTrace;

class ResultRowMapperTest {

    @Test
    void mapsTechnicalColumnsBusinessValuesAndIssueCodes() {
        var schema = new OutputSchema(List.of(
            new OutputColumn("fileName", true),
            new OutputColumn("categoryId", true),
            new OutputColumn("documentStatus", true),
            new OutputColumn("errorCodes", true),
            new OutputColumn("warningCodes", true),
            new OutputColumn("processingDurationMs", true),
            new OutputColumn("document_number", false)
        ), Map.of(new FieldId("document-number"), "document_number"));
        var fieldIssue = issue("FIELD_VALIDATION_FAILED", Severity.ERROR);
        var result = new DocumentResult(
            new DocumentId("invoice.pdf"),
            new CategoryId("invoice"),
            ProcessingStatus.FAILED,
            List.of(new FieldResult(new FieldId("document-number"), "FV;1", ProcessingStatus.FAILED, List.of(fieldIssue))),
            List.of(issue("GEOMETRY_LOW_CONFIDENCE", Severity.WARNING)),
            ProcessingTrace.off()
        );

        var row = new ResultRowMapper().map(result, schema, 123);

        assertThat(row.values()).containsEntry("fileName", "invoice.pdf");
        assertThat(row.values()).containsEntry("documentStatus", "FAILED");
        assertThat(row.values()).containsEntry("document_number", "FV;1");
        assertThat(row.values()).containsEntry("errorCodes", "FIELD_VALIDATION_FAILED");
        assertThat(row.values()).containsEntry("warningCodes", "GEOMETRY_LOW_CONFIDENCE");
    }

    private static ProcessingIssue issue(String code, Severity severity) {
        return new ProcessingIssue(
            new IssueCode(code),
            severity,
            ErrorScope.FIELD,
            ProcessingStage.FIELD_VALIDATION,
            code,
            null,
            null,
            null,
            null,
            null,
            Map.of()
        );
    }
}
