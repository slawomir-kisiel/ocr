package pl.sk.ocr.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonConfigurationMapper {
    private final ObjectMapper objectMapper;

    public JsonConfigurationMapper() {
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public <T> T read(Path path, Class<T> type) {
        try {
            return objectMapper.readValue(Files.readString(path, StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new ConfigurationException(java.util.List.of(new ConfigurationProblem(
                "CONFIGURATION_READ_FAILED",
                path.toString(),
                e.getMessage()
            )));
        }
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value) + System.lineSeparator();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot serialize configuration", e);
        }
    }

    public void write(Path path, Object value) {
        try {
            Files.writeString(path, write(value), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigurationException(java.util.List.of(new ConfigurationProblem(
                "CONFIGURATION_WRITE_FAILED",
                path.toString(),
                e.getMessage()
            )));
        }
    }
}
