package pl.sk.ocr.cli;

import pl.sk.ocr.core.batch.BatchDispatcher;
import pl.sk.ocr.domain.identifier.BatchId;
import pl.sk.ocr.domain.result.BatchResult;

public final class DefaultBatchExecutor implements BatchExecutor {
    @Override
    public BatchResult execute(BatchId batchId, ProcessingContext context) {
        return new BatchDispatcher(context.documentProcessor()).run(batchId, context.batchOptions());
    }
}
