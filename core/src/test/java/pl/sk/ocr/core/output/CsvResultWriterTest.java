package pl.sk.ocr.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.sk.ocr.config.runtime.CsvOutputConfiguration;

class CsvResultWriterTest {
    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void writesCsvAtomicallyWithHeaderAndEscaping() throws Exception {
        var output = tempDir.resolve("result.csv");
        var schema = new OutputSchema(List.of(
            new OutputColumn("fileName", true),
            new OutputColumn("errorCodes", true)
        ));
        var row = new ResultRow(Map.of("fileName", "a.pdf", "errorCodes", "A;B"));

        new CsvResultWriter().write(
            output,
            schema,
            List.of(row),
            new CsvOutputConfiguration(output, StandardCharsets.UTF_8, ";", "\"", true, false)
        );

        assertThat(Files.readString(output)).contains("fileName;errorCodes");
        assertThat(Files.readString(output)).contains("a.pdf;\"A;B\"");
        assertThat(Files.exists(tempDir.resolve("result.csv.tmp"))).isFalse();
    }
}
