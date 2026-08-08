package pl.sk.ocr.cli;

import pl.sk.ocr.domain.identifier.BatchId;
import pl.sk.ocr.domain.result.BatchResult;

public interface BatchExecutor {
    BatchResult execute(BatchId batchId, ProcessingContext context);
}
