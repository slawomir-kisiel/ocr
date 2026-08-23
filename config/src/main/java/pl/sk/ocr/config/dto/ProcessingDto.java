package pl.sk.ocr.config.dto;

public record ProcessingDto(Integer workers, Integer queueCapacity, String mode) {
    public ProcessingDto(Integer workers, Integer queueCapacity) {
        this(workers, queueCapacity, null);
    }
}
