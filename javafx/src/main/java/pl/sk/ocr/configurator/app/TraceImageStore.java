package pl.sk.ocr.configurator.app;

import java.util.Optional;
import pl.sk.ocr.domain.trace.TraceImageRef;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public interface TraceImageStore {
    TraceImageRef put(String label, ProcessingImage image);

    Optional<ProcessingImage> get(TraceImageRef ref);

    void clear();

    int size();
}
