package pl.sk.ocr.configurator.async;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

public interface BackgroundExecutor extends AutoCloseable {
    <T> CompletionStage<T> submit(Callable<T> task);

    @Override
    void close();
}
