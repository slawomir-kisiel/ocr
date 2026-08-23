package pl.sk.ocr.config.dto;

import java.util.List;

public record ProfilePreprocessingDto(List<ExtensionRefDto> imageProcessors) {
}
