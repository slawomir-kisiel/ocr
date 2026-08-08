package pl.sk.ocr.core.output;

import pl.sk.ocr.domain.Validation;

public record OutputColumn(String name, boolean technical) {
    public OutputColumn {
        name = Validation.requireText(name, "column name");
    }
}
