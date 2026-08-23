package pl.sk.ocr.config.dto;

import java.util.List;

public record CategoryReferenceDocumentsDto(String active, List<CategoryReferenceDocumentDto> documents) {
}
