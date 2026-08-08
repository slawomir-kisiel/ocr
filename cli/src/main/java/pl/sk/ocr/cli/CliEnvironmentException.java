package pl.sk.ocr.cli;

public final class CliEnvironmentException extends RuntimeException {
    public CliEnvironmentException(String message) {
        super(message);
    }

    public CliEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
