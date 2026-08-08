package pl.sk.ocr.core.geometry;

import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.AnchorId;

public record ReferenceFeature(AnchorId anchorId, Region bounds, double confidence) {
}
