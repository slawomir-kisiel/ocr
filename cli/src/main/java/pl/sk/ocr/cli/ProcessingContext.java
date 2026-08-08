package pl.sk.ocr.cli;

import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.batch.BatchOptions;
import pl.sk.ocr.core.processing.DocumentProcessor;

public record ProcessingContext(RuntimeConfiguration configuration, DocumentProcessor documentProcessor, BatchOptions batchOptions) {
}
