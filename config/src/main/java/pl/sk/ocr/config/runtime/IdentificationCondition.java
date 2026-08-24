package pl.sk.ocr.config.runtime;

import pl.sk.ocr.domain.geometry.Region;

public record IdentificationCondition(int page, String expectedText, ExtensionRef matcher, ExtensionRef detector, Region searchRegion) {
}
