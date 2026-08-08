package pl.sk.ocr.domain.trace;

import java.util.List;
import java.util.Map;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.issue.ProcessingStage;

public record TraceEntry(ProcessingStage stage, String message, Map<String, Object> attributes, List<TraceImageRef> images) {
    public TraceEntry {
        stage = Validation.requireNonNull(stage, "stage");
        message = Validation.requireText(message, "message");
        attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
        images = List.copyOf(Validation.requireNoNulls(images == null ? List.of() : images, "images"));
    }
}
