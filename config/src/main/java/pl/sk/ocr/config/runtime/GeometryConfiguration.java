package pl.sk.ocr.config.runtime;

import java.util.List;
import pl.sk.ocr.domain.identifier.AnchorId;

public record GeometryConfiguration(int referenceWidth, int referenceHeight, String strategy, List<AnchorId> anchors) {
    public GeometryConfiguration {
        anchors = List.copyOf(anchors == null ? List.of() : anchors);
    }
}
