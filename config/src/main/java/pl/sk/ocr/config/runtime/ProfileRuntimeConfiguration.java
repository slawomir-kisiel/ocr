package pl.sk.ocr.config.runtime;

import java.nio.file.Path;
import java.util.List;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.trace.TraceMode;

public record ProfileRuntimeConfiguration(
    String id,
    ConfigurationVersion version,
    Path categoriesDirectory,
    CategoriesMode categoriesMode,
    List<CategoryId> activeCategories,
    DirectoriesConfiguration directories,
    ProcessingConfiguration processing,
    OcrSettings ocr,
    TraceMode traceMode,
    CsvOutputConfiguration csvOutput
) {
    public ProfileRuntimeConfiguration {
        activeCategories = List.copyOf(activeCategories == null ? List.of() : activeCategories);
    }
}
