package pl.sk.ocr.configurator.app;

import pl.sk.ocr.domain.result.FieldResult;
import pl.sk.ocr.domain.trace.ProcessingTrace;

public record FieldPreviewResult(FieldResult fieldResult, ProcessingTrace trace) {
}
