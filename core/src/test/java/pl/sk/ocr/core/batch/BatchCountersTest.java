package pl.sk.ocr.core.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
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

class BatchCountersTest {

    @Test
    void aggregatesDocumentAndFieldIssues() {
        var counters = new BatchCounters();

        counters.accept(new DocumentResult(
            new DocumentId("ok.pdf"),
            null,
            ProcessingStatus.WARNING,
            List.of(new FieldResult(new FieldId("number"), "", ProcessingStatus.WARNING, List.of(issue("FIELD_WARNING", Severity.WARNING)))),
            List.of(issue("DOCUMENT_WARNING", Severity.WARNING)),
            ProcessingTrace.off()
        ));
        counters.accept(new DocumentResult(
            new DocumentId("bad.pdf"),
            null,
            ProcessingStatus.FAILED,
            List.of(),
            List.of(issue("DOCUMENT_FAILED", Severity.ERROR)),
            ProcessingTrace.off()
        ));

        assertThat(counters.total()).isEqualTo(2);
        assertThat(counters.successWithWarnings()).isOne();
        assertThat(counters.failed()).isOne();
        assertThat(counters.warnings()).isEqualTo(2);
        assertThat(counters.issueCounts())
            .containsEntry("FIELD_WARNING", 1L)
            .containsEntry("DOCUMENT_WARNING", 1L)
            .containsEntry("DOCUMENT_FAILED", 1L);
    }

    private static ProcessingIssue issue(String code, Severity severity) {
        return new ProcessingIssue(
            new IssueCode(code),
            severity,
            ErrorScope.DOCUMENT,
            ProcessingStage.DOCUMENT_LOADING,
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
