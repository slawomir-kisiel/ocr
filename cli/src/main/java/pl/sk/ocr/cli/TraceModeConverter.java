package pl.sk.ocr.cli;

import java.util.Locale;
import picocli.CommandLine.ITypeConverter;
import pl.sk.ocr.domain.trace.TraceMode;

public final class TraceModeConverter implements ITypeConverter<TraceMode> {
    @Override
    public TraceMode convert(String value) {
        return TraceMode.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
