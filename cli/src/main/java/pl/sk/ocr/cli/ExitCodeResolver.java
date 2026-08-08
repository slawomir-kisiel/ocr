package pl.sk.ocr.cli;

public final class ExitCodeResolver {
    public int resolve(CliExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> 0;
            case ARGUMENT_ERROR -> 1;
            case CONFIGURATION_ERROR -> 2;
            case ENVIRONMENT_ERROR -> 3;
            case EXECUTION_ERROR -> 4;
            case INTERRUPTED -> 130;
        };
    }
}
