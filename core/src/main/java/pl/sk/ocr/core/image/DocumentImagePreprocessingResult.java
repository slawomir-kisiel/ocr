package pl.sk.ocr.core.image;

import java.util.List;
import java.util.Map;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.extension.api.image.ProcessingImage;

public record DocumentImagePreprocessingResult(
    PageNumber page,
    ProcessingImage image,
    List<StepTrace> steps
) {
    public DocumentImagePreprocessingResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public record StepTrace(
        int order,
        String processorId,
        ProcessingImage input,
        ProcessingImage output,
        List<EventTrace> events
    ) {
        public StepTrace {
            events = events == null ? List.of() : List.copyOf(events);
        }
    }

    public record EventTrace(String event, Map<String, Object> attributes) {
        public EventTrace {
            attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        }
    }
}
