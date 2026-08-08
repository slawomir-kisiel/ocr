package pl.sk.ocr.config.runtime;

import pl.sk.ocr.domain.geometry.Region;

public record IdentificationCondition(String type, int page, String expectedText, ExtensionRef matcher, ExtensionRef detector, Region searchRegion) {
}
