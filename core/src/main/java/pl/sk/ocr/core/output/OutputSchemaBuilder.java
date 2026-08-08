package pl.sk.ocr.core.output;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.domain.identifier.FieldId;

public final class OutputSchemaBuilder {
    private static final List<String> TECHNICAL_COLUMNS = List.of(
        "fileName",
        "categoryId",
        "documentStatus",
        "errorCodes",
        "warningCodes",
        "processingDurationMs"
    );

    public OutputSchema build(RuntimeConfiguration configuration) {
        var columns = new ArrayList<OutputColumn>();
        TECHNICAL_COLUMNS.forEach(name -> columns.add(new OutputColumn(name, true)));
        var businessColumns = new LinkedHashSet<String>();
        var fieldColumns = new LinkedHashMap<FieldId, String>();
        for (var category : configuration.categories()) {
            for (var field : category.fields()) {
                if (field.exported() && field.columnName() != null && !field.columnName().isBlank()) {
                    businessColumns.add(field.columnName());
                    fieldColumns.putIfAbsent(field.id(), field.columnName());
                }
            }
        }
        businessColumns.forEach(name -> columns.add(new OutputColumn(name, false)));
        return new OutputSchema(columns, fieldColumns);
    }
}
