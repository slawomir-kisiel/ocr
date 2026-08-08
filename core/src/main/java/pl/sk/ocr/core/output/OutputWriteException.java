package pl.sk.ocr.core.output;

public class OutputWriteException extends RuntimeException {
    private final String code;

    public OutputWriteException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
