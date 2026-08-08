package pl.sk.ocr.configurator.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.dto.*;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;

class DraftValidationServiceTest {

    @Test
    void validatesDraftCategory() {
        var service = new DraftValidationService(new DefaultExtensionRegistry(List.of()));

        var problems = service.validate(new CategoryDto(
            "1.0",
            "",
            "1.0",
            "Broken",
            "",
            new PageSelectionDto("SINGLE", 1, null, null, null),
            new OcrSettingsDto("pol", null),
            new IdentificationDto(List.of()),
            new GeometryDto(0, 0, new GeometryStrategyDto("NONE", List.of())),
            List.of(),
            List.of()
        ));

        assertThat(problems).extracting(DraftValidationProblem::code)
            .contains("CONFIGURATION_INVALID", "CATEGORY_IDENTIFICATION_REQUIRED", "FIELDS_REQUIRED");
    }
}
