package pl.sk.ocr.config.dto;

public record ConditionDto(String type, Integer page, String expectedText, ExtensionRefDto matcher, ExtensionRefDto detector, RegionDto searchRegion) {
}
