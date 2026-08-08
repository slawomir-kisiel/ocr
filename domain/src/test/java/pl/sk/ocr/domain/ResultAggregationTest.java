package pl.sk.ocr.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.identifier.BatchId;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.issue.ErrorScope;
import pl.sk.ocr.domain.issue.IssueCode;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.result.BatchResult;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.trace.ProcessingTrace;

class ResultAggregationTest {

    @Test
    void aggregatesWarningIssueIntoDocumentStatus() {
        var issue = new ProcessingIssue(
            new IssueCode("FIELD_VALIDATION_WARNING"),
            pl.sk.ocr.domain.issue.Severity.WARNING,
            ErrorScope.FIELD,
            ProcessingStage.FIELD_VALIDATION,
            "Suspicious value",
            null,
            null,
            null,
            null,
            null,
            java.util.Map.of()
        );

        var result = DocumentResult.from(new DocumentId("doc-1"), null, List.of(), List.of(issue), ProcessingTrace.off());

        assertThat(result.status()).isEqualTo(ProcessingStatus.WARNING);
    }

    @Test
    void fatalBatchIssueDominatesDocumentStatuses() {
        var document = new DocumentResult(
            new DocumentId("doc-1"),
            null,
            ProcessingStatus.SUCCESS,
            List.of(),
            List.of(),
            ProcessingTrace.off()
        );
        var issue = ProcessingIssue.error(
            new IssueCode("OUTPUT_WRITE_FAILED"),
            ErrorScope.OUTPUT,
            ProcessingStage.OUTPUT_WRITING,
            "Cannot write output"
        );

        var result = BatchResult.from(new BatchId("batch-1"), List.of(document), List.of(issue));

        assertThat(result.status()).isEqualTo(ProcessingStatus.FAILED);
    }
}
