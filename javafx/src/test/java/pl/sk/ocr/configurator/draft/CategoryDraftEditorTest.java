package pl.sk.ocr.configurator.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.dto.*;

class CategoryDraftEditorTest {
    private final CategoryDraftEditor editor = new CategoryDraftEditor();

    @Test
    void updatesCategoryMetadataWithImmutableReplacement() {
        var original = editor.newCategory("invoice", "Invoice");

        var updated = editor.updateCategoryMetadata(original, "receipt", "Receipt", "Updated", "2.0");

        assertThat(updated).isNotSameAs(original);
        assertThat(updated.id()).isEqualTo("receipt");
        assertThat(updated.displayName()).isEqualTo("Receipt");
        assertThat(updated.description()).isEqualTo("Updated");
        assertThat(updated.version()).isEqualTo("2.0");
        assertThat(original.id()).isEqualTo("invoice");
    }

    @Test
    void addsRemovesAndMovesFields() {
        var draft = editor.newCategory("invoice", "Invoice");

        draft = editor.addField(draft, field("number"));
        draft = editor.addField(draft, field("date"));
        draft = editor.addField(draft, field("total"));
        draft = editor.moveField(draft, 2, 0);
        draft = editor.removeField(draft, 1);

        assertThat(draft.fields()).extracting(FieldDto::id).containsExactly("total", "date");
    }

    @Test
    void rejectsDuplicateFieldId() {
        var draft = editor.addField(editor.newCategory("invoice", "Invoice"), field("number"));

        assertThatThrownBy(() -> editor.addField(draft, field("number")))
            .isInstanceOf(DraftMutationException.class)
            .hasMessage("Duplicate field id: number");
    }

    @Test
    void addsRemovesAndMovesIdentificationConditions() {
        var draft = editor.newCategory("invoice", "Invoice");
        draft = editor.addIdentificationGroup(draft, new ConditionGroupDto(List.of()));
        draft = editor.addCondition(draft, 1, condition("A"));
        draft = editor.addCondition(draft, 1, condition("B"));
        draft = editor.addCondition(draft, 1, condition("C"));
        draft = editor.moveCondition(draft, 1, 2, 0);
        draft = editor.removeCondition(draft, 1, 1);

        assertThat(draft.identification().groups().get(1).conditions())
            .extracting(ConditionDto::expectedText)
            .containsExactly("C", "B");
    }

    @Test
    void updatesPipelineSteps() {
        var draft = editor.addField(editor.newCategory("invoice", "Invoice"), field("number"));

        draft = editor.addTransformer(draft, 0, extension("trim"));
        draft = editor.addTransformer(draft, 0, extension("uppercase"));
        draft = editor.moveTransformer(draft, 0, 1, 0);
        draft = editor.removeTransformer(draft, 0, 1);

        assertThat(draft.fields().get(0).transformers()).extracting(ExtensionRefDto::id).containsExactly("uppercase");
    }

    @Test
    void invalidIndexThrowsClearException() {
        var draft = editor.newCategory("invoice", "Invoice");

        assertThatThrownBy(() -> editor.removeField(draft, 0))
            .isInstanceOf(DraftMutationException.class)
            .hasMessage("Invalid field index 0 for size 0");
    }

    private static FieldDto field(String id) {
        return new FieldDto(
            id,
            id,
            1,
            new RegionDto(0, 0, 10, 10),
            true,
            null,
            new OutputDto(true, id),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private static ConditionDto condition(String text) {
        return new ConditionDto("TEXT", 1, text, null, null, null);
    }

    private static ExtensionRefDto extension(String id) {
        return new ExtensionRefDto(id, Map.of());
    }
}
