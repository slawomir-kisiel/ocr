package pl.sk.ocr.core.batch;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;
import pl.sk.ocr.domain.issue.ProcessingIssue;
import pl.sk.ocr.domain.result.DocumentResult;
import pl.sk.ocr.domain.result.ProcessingStatus;

public final class BatchCounters {
    private final LongAdder total = new LongAdder();
    private final LongAdder success = new LongAdder();
    private final LongAdder successWithWarnings = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder warnings = new LongAdder();
    private final ConcurrentHashMap<String, LongAdder> issueCounts = new ConcurrentHashMap<>();

    public void accept(DocumentResult result) {
        total.increment();
        switch (result.status()) {
            case SUCCESS -> success.increment();
            case WARNING -> successWithWarnings.increment();
            case FAILED, FATAL -> failed.increment();
        }
        allIssues(result).forEach(issue -> {
            if (issue.severity() == pl.sk.ocr.domain.issue.Severity.WARNING) {
                warnings.increment();
            }
            issueCounts.computeIfAbsent(issue.code().value(), key -> new LongAdder()).increment();
        });
    }

    public long total() {
        return total.sum();
    }

    public long success() {
        return success.sum();
    }

    public long successWithWarnings() {
        return successWithWarnings.sum();
    }

    public long failed() {
        return failed.sum();
    }

    public long warnings() {
        return warnings.sum();
    }

    public Map<String, Long> issueCounts() {
        var copy = new TreeMap<String, Long>();
        issueCounts.forEach((code, count) -> copy.put(code, count.sum()));
        return copy;
    }

    private Stream<ProcessingIssue> allIssues(DocumentResult result) {
        return Stream.concat(
            result.issues().stream(),
            result.fields().stream().flatMap(field -> field.issues().stream())
        );
    }
}
