package pl.sk.ocr.configurator.app;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import pl.sk.ocr.core.image.BufferedProcessingImage;
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
        images.put(ref.id(), snapshot(image));
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

    private ProcessingImage snapshot(ProcessingImage image) {
        var source = image.asBufferedImage();
        var type = source.getType() == 0 ? java.awt.image.BufferedImage.TYPE_INT_ARGB : source.getType();
        var copy = new java.awt.image.BufferedImage(source.getWidth(), source.getHeight(), type);
        var graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return new BufferedProcessingImage(copy);
    }
}
