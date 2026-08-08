package pl.sk.ocr.config.dto;

import java.util.List;

public record PageSelectionDto(String type, Integer page, Integer from, Integer to, List<Integer> pages) {
}
