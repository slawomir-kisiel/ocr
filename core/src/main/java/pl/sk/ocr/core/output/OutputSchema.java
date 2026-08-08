package pl.sk.ocr.core.output;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import pl.sk.ocr.domain.identifier.FieldId;

public record OutputSchema(List<OutputColumn> columns, Map<FieldId, String> fieldColumns) {
    public OutputSchema(List<OutputColumn> columns) {
        this(columns, Map.of());
    }

    public OutputSchema {
        columns = List.copyOf(columns == null ? List.of() : columns);
        fieldColumns = Map.copyOf(fieldColumns == null ? Map.of() : fieldColumns);
    }

    public List<String> columnNames() {
        return columns.stream().map(OutputColumn::name).toList();
    }

    public Optional<String> columnNameFor(FieldId fieldId) {
        return Optional.ofNullable(fieldColumns.get(fieldId));
    }
}
