package pl.sk.ocr.configurator.app;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import pl.sk.ocr.domain.trace.TraceImageRef;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public final class InMemoryTraceImageStore implements TraceImageStore {
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<String, ProcessingImage> images = new ConcurrentHashMap<>();

    @Override
    public TraceImageRef put(String label, ProcessingImage image) {
        if (image == null) {
            throw new IllegalArgumentException("trace image is required");
        }
        var ref = new TraceImageRef("trace-image-" + sequence.incrementAndGet(), label == null || label.isBlank() ? "Trace image" : label.trim());
        images.put(ref.id(), image);
        return ref;
    }

    @Override
    public Optional<ProcessingImage> get(TraceImageRef ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(images.get(ref.id()));
    }

    @Override
    public void clear() {
        images.clear();
    }

    @Override
    public int size() {
        return images.size();
    }
}
