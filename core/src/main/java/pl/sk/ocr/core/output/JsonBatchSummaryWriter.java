package pl.sk.ocr.core.output;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonBatchSummaryWriter {
    private final ObjectMapper mapper = new ObjectMapper()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .enable(SerializationFeature.INDENT_OUTPUT);

    public void write(Path outputFile, BatchSummary summary) {
        var tmp = outputFile.resolveSibling(outputFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(outputFile.toAbsolutePath().getParent());
            Files.writeString(tmp, mapper.writeValueAsString(summary) + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.move(tmp, outputFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new OutputWriteException("OUTPUT_WRITE_FAILED", "Cannot write summary output: " + outputFile, e);
        }
    }
}
