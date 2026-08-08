package pl.sk.ocr.config.runtime;

import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.AnchorId;

public record AnchorDefinition(AnchorId id, int page, ExtensionRef detector, boolean required, Region bounds, Region searchRegion) {
}
