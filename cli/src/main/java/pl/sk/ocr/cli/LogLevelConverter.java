package pl.sk.ocr.cli;

import java.util.Locale;
import picocli.CommandLine.ITypeConverter;

public final class LogLevelConverter implements ITypeConverter<LogLevel> {
    @Override
    public LogLevel convert(String value) {
        return LogLevel.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
