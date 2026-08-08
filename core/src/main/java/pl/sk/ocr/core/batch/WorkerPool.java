package pl.sk.ocr.core.batch;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.core.processing.DocumentProcessor;
import pl.sk.ocr.domain.result.DocumentResult;

public final class WorkerPool {
    private final DocumentProcessor processor;
    private final SourceFileMover mover;

    public WorkerPool(DocumentProcessor processor) {
        this(processor, new SourceFileMover());
    }

    public WorkerPool(DocumentProcessor processor, SourceFileMover mover) {
        this.processor = processor;
        this.mover = mover;
    }

    public List<BatchItemResult> process(List<DocumentJob> jobs, RuntimeConfiguration configuration, int workers, BatchCounters counters) {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            var tasks = jobs.stream()
                .<Callable<BatchItemResult>>map(job -> () -> process(job, configuration, counters))
                .toList();
            return executor.invokeAll(tasks).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new BatchProcessingException("BATCH_ABORTED", "Batch interrupted", e);
                    } catch (java.util.concurrent.ExecutionException e) {
                        throw new BatchProcessingException("WORKER_FAILED", "Worker failed", e.getCause());
                    }
                })
                .sorted(Comparator.comparingInt(item -> item.job().sequence()))
                .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BatchProcessingException("BATCH_ABORTED", "Batch interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private BatchItemResult process(DocumentJob job, RuntimeConfiguration configuration, BatchCounters counters) {
        var started = System.nanoTime();
        DocumentResult result = processor.process(job.source(), configuration);
        counters.accept(result);
        var finalLocation = mover.move(job.source(), result.status(), configuration.profile().directories());
        var durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        return new BatchItemResult(job, result, durationMs, finalLocation);
    }
}
