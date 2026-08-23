package pl.sk.ocr.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import pl.sk.ocr.config.runtime.ProcessingMode;
import pl.sk.ocr.domain.trace.TraceMode;

@Command(
    name = "sk-ocr",
    mixinStandardHelpOptions = true,
    versionProvider = OcrVersionProvider.class,
    description = "Batch OCR document processing"
)
public final class CliOptions implements Callable<Integer> {
    @Option(names = "--profile", description = "Processing profile JSON")
    private Path profile;
    @Option(names = "--input", description = "Override input directory")
    private Path input;
    @Option(names = "--success", description = "Override success directory")
    private Path success;
    @Option(names = "--error", description = "Override error directory")
    private Path error;
    @Option(names = "--workers", description = "Override number of workers")
    private Integer workers;
    @Option(names = "--mode", description = "FULL or CLASSIFY_ONLY")
    private ProcessingMode mode;
    @Option(names = "--output", description = "Override CSV output file")
    private Path output;
    @Option(names = "--summary-json", description = "Write machine-readable batch summary JSON")
    private Path summaryJson;
    @Option(names = "--trace", description = "OFF, BASIC or FULL", converter = TraceModeConverter.class)
    private TraceMode trace;
    @Option(names = "--ocr-datapath", description = "Override Tesseract datapath")
    private Path ocrDatapath;
    @Option(names = "--ocr-language", description = "Override OCR language")
    private String ocrLanguage;
    @Option(names = "--log-level", description = "ERROR, WARN, INFO, DEBUG, TRACE", converter = LogLevelConverter.class)
    private LogLevel logLevel;

    @Override
    public Integer call() {
        return 0;
    }

    public Path profile() {
        return profile;
    }

    public Path input() {
        return input;
    }

    public Path success() {
        return success;
    }

    public Path error() {
        return error;
    }

    public Integer workers() {
        return workers;
    }

    public ProcessingMode mode() {
        return mode;
    }

    public Path output() {
        return output;
    }

    public Path summaryJson() {
        return summaryJson;
    }

    public TraceMode trace() {
        return trace;
    }

    public Path ocrDatapath() {
        return ocrDatapath;
    }

    public String ocrLanguage() {
        return ocrLanguage;
    }

    public LogLevel logLevel() {
        return logLevel;
    }
}
