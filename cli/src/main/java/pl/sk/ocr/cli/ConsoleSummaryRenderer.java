package pl.sk.ocr.cli;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import pl.sk.ocr.domain.result.BatchResult;
import pl.sk.ocr.domain.result.ProcessingStatus;

public final class ConsoleSummaryRenderer {
    public void started(PrintWriter out, String profileId, int workers, Path input) {
        out.println("Starting OCR batch");
        out.println("Profile: " + profileId);
        out.println("Workers: " + workers);
        out.println("Input: " + input);
        out.flush();
    }

    public void completed(PrintWriter out, BatchResult result, Duration duration, Path outputFile) {
        var success = result.documents().stream().filter(document -> document.status() == ProcessingStatus.SUCCESS).count();
        var warnings = result.documents().stream().filter(document -> document.status() == ProcessingStatus.WARNING).count();
        var failed = result.documents().stream().filter(document -> document.status() == ProcessingStatus.FAILED || document.status() == ProcessingStatus.FATAL).count();
        out.println("Batch completed");
        out.println("Documents: " + result.documents().size());
        out.println("Success: " + success);
        out.println("Warnings: " + warnings);
        out.println("Failed: " + failed);
        out.println("Duration: " + duration.toMillis() + " ms");
        out.println("Output: " + outputFile);
        out.flush();
    }
}
