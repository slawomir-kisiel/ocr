package pl.sk.ocr.configurator.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.dto.*;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.ocr.OcrText;

class ConfigurationSessionTest {

    @Test
    void marksDirtyAndInvalidatesDownstreamCachesOnDraftChange() {
        var session = new ConfigurationSession();
        session.ocrCache().put(new PageNumber(1), new OcrText("old", List.of()));
        session.markSaved();

        session.replaceDraft(category("invoice"));

        assertThat(session.dirty()).isTrue();
        assertThat(session.ocrCache()).isEmpty();
    }

    @Test
    void opensDraftWithoutDirtyStateAndInvalidatesDownstreamCaches() {
        var session = new ConfigurationSession();
        session.ocrCache().put(new PageNumber(1), new OcrText("old", List.of()));
        session.replaceDraft(category("old"));

        session.openDraft(category("invoice"));

        assertThat(session.dirty()).isFalse();
        assertThat(session.draftCategory().id()).isEqualTo("invoice");
        assertThat(session.ocrCache()).isEmpty();
    }

    private static CategoryDto category(String id) {
        return new CategoryDto(
            "1.0",
            id,
            "1.0",
            id,
            "",
            new PageSelectionDto("SINGLE", 1, null, null, null),
            new OcrSettingsDto("pol", null),
            new IdentificationDto(List.of(new ConditionGroupDto(List.of(new ConditionDto("TEXT", 1, "DOC", null, null, null))))),
            new GeometryDto(0, 0, new GeometryStrategyDto("NONE", List.of())),
            List.of(),
            List.of(new FieldDto(
                "number",
                "Number",
                1,
                new RegionDto(0, 0, 10, 10),
                true,
                null,
                new OutputDto(true, "number"),
                List.of(),
                List.of(),
                List.of()
            ))
        );
    }
}
