package pl.sk.ocr.config.runtime;

public record ProcessingConfiguration(int workers, int queueCapacity, ProcessingMode mode) {
    public ProcessingConfiguration(int workers, int queueCapacity) {
        this(workers, queueCapacity, ProcessingMode.FULL);
    }

    public ProcessingConfiguration {
        mode = mode == null ? ProcessingMode.FULL : mode;
    }
}
