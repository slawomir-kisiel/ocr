package pl.sk.ocr.config.runtime;

import java.util.List;

public record IdentificationGroup(List<IdentificationCondition> conditions) {
    public IdentificationGroup {
        conditions = List.copyOf(conditions == null ? List.of() : conditions);
    }
}
