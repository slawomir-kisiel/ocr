package pl.sk.ocr.extension.api.trace;

import java.util.Map;

public interface TraceSink {
    TraceSink NOOP = (event, attributes) -> { };

    void add(String event, Map<String, Object> attributes);
}
