package pl.sk.ocr.domain.trace;

import java.util.List;
import pl.sk.ocr.domain.Validation;
import pl.sk.ocr.domain.result.StageResult;

public record ProcessingTrace(TraceMode mode, List<StageResult> stages, List<TraceEntry> entries) {
    public ProcessingTrace {
        mode = Validation.requireNonNull(mode, "trace mode");
        stages = List.copyOf(Validation.requireNoNulls(stages == null ? List.of() : stages, "stages"));
        entries = List.copyOf(Validation.requireNoNulls(entries == null ? List.of() : entries, "entries"));
    }

    public static ProcessingTrace off() {
        return new ProcessingTrace(TraceMode.OFF, List.of(), List.of());
    }
}
