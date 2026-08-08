package pl.sk.ocr.configurator.async;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExecutorBackgroundExecutor implements BackgroundExecutor {
    private final ExecutorService executor;

    public ExecutorBackgroundExecutor() {
        this(Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2)));
    }

    ExecutorBackgroundExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
