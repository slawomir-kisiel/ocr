package pl.sk.ocr.config;

import pl.sk.ocr.config.dto.CategoryDto;
import pl.sk.ocr.config.runtime.CategoryRuntimeConfiguration;

public final class CategoryRuntimeMapper {
    public CategoryRuntimeConfiguration map(CategoryDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("category is required");
        }
        return ConfigurationMappers.category(dto);
    }
}
