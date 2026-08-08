package pl.sk.ocr.config.dto;

public record CsvOutputDto(String file, String charset, String delimiter, String quote, Boolean includeHeader, Boolean overwrite) {
}
