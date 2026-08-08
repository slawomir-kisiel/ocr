package pl.sk.ocr.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.domain.identifier.DocumentId;
import pl.sk.ocr.domain.issue.ErrorScope;
import pl.sk.ocr.domain.issue.IssueCode;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.issue.ProcessingStage;
import pl.sk.ocr.domain.issue.Severity;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.ProcessingStatus;
import pl.sk.ocr.domain.trace.ProcessingTrace;

class BatchSummaryBuilderTest {

    @Test
    void buildsMachineReadableSummary() {
        var summary = new BatchSummaryBuilder().build(List.of(
            new DocumentResult(new DocumentId("ok.pdf"), null, ProcessingStatus.SUCCESS, List.of(), List.of(), ProcessingTrace.off()),
            new DocumentResult(new DocumentId("bad.pdf"), null, ProcessingStatus.FAILED, List.of(), List.of(issue()), ProcessingTrace.off())
        ));

        assertThat(summary.schemaVersion()).isEqualTo("1.0");
        assertThat(summary.totalDocuments()).isEqualTo(2);
        assertThat(summary.failedDocuments()).isOne();
        assertThat(summary.issueCounts()).containsEntry("DOCUMENT_LOAD_FAILED", 1L);
    }

    private static ProcessingIssue issue() {
        return new ProcessingIssue(
            new IssueCode("DOCUMENT_LOAD_FAILED"),
            Severity.ERROR,
            ErrorScope.DOCUMENT,
            ProcessingStage.DOCUMENT_LOADING,
            "load failed",
            null,
            null,
            null,
            null,
            null,
            Map.of()
        );
    }
}
