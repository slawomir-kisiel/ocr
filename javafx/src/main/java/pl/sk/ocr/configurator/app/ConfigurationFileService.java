package pl.sk.ocr.configurator.app;

import java.nio.file.Files;
import java.nio.file.Path;
import pl.sk.ocr.config.JsonConfigurationMapper;
import pl.sk.ocr.config.dto.*;

public final class ConfigurationFileService {
    private final JsonConfigurationMapper mapper;

    public ConfigurationFileService(JsonConfigurationMapper mapper) {
        this.mapper = mapper;
    }

    public CategoryDto loadCategory(Path path) {
        return mapper.read(path, CategoryDto.class);
    }

    public void saveCategory(Path path, CategoryDto draft) {
        mapper.write(path, draft);
    }

    public CategoryDto newCategory(String id, String displayName) {
        var normalizedId = id == null || id.isBlank() ? "new-category" : id.trim();
        return new CategoryDto(
            "1.0",
            normalizedId,
            "1.0",
            displayName == null || displayName.isBlank() ? normalizedId : displayName.trim(),
            "",
            new PageSelectionDto("SINGLE", 1, null, null, null),
            new OcrSettingsDto("pol", null),
            new IdentificationDto(java.util.List.of(new ConditionGroupDto(java.util.List.of(
                new ConditionDto("TEXT", 1, "", null, null, null)
            )))),
            new GeometryDto(0, 0, new GeometryStrategyDto("NONE", java.util.List.of())),
            java.util.List.of(),
            java.util.List.of()
        );
    }

    public boolean exists(Path path) {
        return Files.exists(path);
    }
}
