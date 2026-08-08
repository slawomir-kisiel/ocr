package pl.sk.ocr.core.output;

import java.util.Map;

public record ResultRow(Map<String, String> values) {
    public ResultRow {
        values = Map.copyOf(values == null ? Map.of() : values);
    }
}
