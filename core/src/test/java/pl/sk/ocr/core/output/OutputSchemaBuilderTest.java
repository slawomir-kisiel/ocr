package pl.sk.ocr.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.runtime.CategoriesMode;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;
import pl.sk.ocr.config.runtime.CsvOutputConfiguration;
import pl.sk.ocr.config.runtime.DirectoriesConfiguration;
import pl.sk.ocr.config.runtime.FieldDefinition;
import pl.sk.ocr.config.runtime.GeometryConfiguration;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.config.runtime.ProcessingConfiguration;
import pl.sk.ocr.config.runtime.ProfileRuntimeConfiguration;
import pl.sk.ocr.config.runtime.RuntimeConfiguration;
import pl.sk.ocr.config.runtime.SinglePageSelection;
import pl.sk.ocr.domain.config.ConfigurationVersion;
import pl.sk.ocr.domain.geometry.Region;
import pl.sk.ocr.domain.identifier.CategoryId;
import pl.sk.ocr.domain.identifier.FieldId;
import pl.sk.ocr.domain.trace.TraceMode;

class OutputSchemaBuilderTest {

    @Test
    void buildsTechnicalColumnsAndUnionOfBusinessColumns() {
        var schema = new OutputSchemaBuilder().build(configuration());

        assertThat(schema.columnNames()).containsExactly(
            "fileName",
            "categoryId",
            "documentStatus",
            "errorCodes",
            "warningCodes",
            "processingDurationMs",
            "document_number",
            "total"
        );
    }

    static RuntimeConfiguration configuration() {
        var category = new CategoryRuntimeConfiguration(
            new CategoryId("invoice"),
            new ConfigurationVersion("1.0"),
            "Invoice",
            new SinglePageSelection(1),
            OcrSettings.defaults(),
            new GeometryConfiguration(100, 100, "NONE", List.of()),
            List.of(),
            List.of(),
            List.of(field("document-number", "document_number"), field("total", "total"))
        );
        var profile = new ProfileRuntimeConfiguration(
            "default",
            new ConfigurationVersion("1.0"),
            Path.of("."),
            CategoriesMode.EXPLICIT,
            List.of(new CategoryId("invoice")),
            new DirectoriesConfiguration(Path.of("input"), Path.of("success"), Path.of("error")),
            new ProcessingConfiguration(1, 4),
            OcrSettings.defaults(),
            TraceMode.OFF,
            new CsvOutputConfiguration(Path.of("result.csv"), java.nio.charset.StandardCharsets.UTF_8, ";", "\"", true, false)
        );
        return new RuntimeConfiguration(profile, List.of(category));
    }

    private static FieldDefinition field(String id, String column) {
        return new FieldDefinition(
            new FieldId(id),
            id,
            1,
            new Region(0, 0, 10, 10),
            true,
            OcrSettings.defaults(),
            true,
            column,
            List.of(),
            List.of(),
            List.of()
        );
    }
}
