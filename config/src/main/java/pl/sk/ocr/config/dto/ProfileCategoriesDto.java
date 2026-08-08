package pl.sk.ocr.config.dto;

import java.util.List;

public record ProfileCategoriesDto(String directory, String mode, List<String> active) {
}
