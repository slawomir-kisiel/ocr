package pl.sk.ocr.config;

import java.nio.file.Path;
import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;

public final class CategoryLoader {
    private final JsonConfigurationMapper mapper;
    private final CategoryValidator validator;

    public CategoryLoader(JsonConfigurationMapper mapper, CategoryValidator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public CategoryRuntimeConfiguration load(Path path) {
        var dto = mapper.read(path, CategoryDto.class);
        var problems = validator.validate(dto);
        if (!problems.isEmpty()) {
            throw new ConfigurationException(problems);
        }
        return ConfigurationMappers.category(dto);
    }
}
