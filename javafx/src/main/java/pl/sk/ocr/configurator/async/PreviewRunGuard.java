package pl.sk.ocr.configurator.async;

import java.util.concurrent.atomic.AtomicLong;

public final class PreviewRunGuard {
    private final AtomicLong latest = new AtomicLong();

    public PreviewRunId next() {
        return new PreviewRunId(latest.incrementAndGet());
    }

    public boolean isLatest(PreviewRunId runId) {
        return latest.get() == runId.value();
    }
}
