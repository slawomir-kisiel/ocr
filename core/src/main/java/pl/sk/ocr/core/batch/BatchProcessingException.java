package pl.sk.ocr.core.batch;

public final class BatchProcessingException extends RuntimeException {
    private final String code;

    public BatchProcessingException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
