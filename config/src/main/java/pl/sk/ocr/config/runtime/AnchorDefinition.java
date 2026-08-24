package pl.sk.ocr.config.runtime;

import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.AnchorId;

public record AnchorDefinition(AnchorId id, int page, ExtensionRef detector, String expectedText, ExtensionRef matcher, boolean required, Region bounds,
                               Region searchRegion) {
}
