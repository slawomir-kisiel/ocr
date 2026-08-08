package pl.sk.ocr.config;

import java.nio.file.Path;
import pl.sk.ocr.config.dto.ProfileDto;
import pl.sk.ocr.config.runtime.ProfileRuntimeConfiguration;

public final class ProfileLoader {
    private final JsonConfigurationMapper mapper;
    private final ProfileValidator validator;

    public ProfileLoader(JsonConfigurationMapper mapper, ProfileValidator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public ProfileRuntimeConfiguration load(Path path) {
        var dto = mapper.read(path, ProfileDto.class);
        var problems = validator.validate(dto);
        if (!problems.isEmpty()) {
            throw new ConfigurationException(problems);
        }
        return ConfigurationMappers.profile(dto, path);
    }
}
