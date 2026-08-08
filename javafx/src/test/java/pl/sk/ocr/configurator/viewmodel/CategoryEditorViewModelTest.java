package pl.sk.ocr.configurator.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.configurator.app.ConfigurationFileService;
import pl.sk.ocr.configurator.draft.CategoryDraftEditor;
import pl.sk.ocr.configurator.validation.DraftValidationProblem;
import pl.sk.ocr.configurator.validation.DraftValidationService;
import pl.sk.ocr.extension.api.DefaultExtensionRegistry;

class CategoryEditorViewModelTest {

    @Test
    void categoryMetadataChangeMarksDirtyAndRefreshesValidation() {
        var viewModel = new CategoryEditorViewModel(
            new ConfigurationFileService(null),
            null,
            null,
            new DraftValidationService(new DefaultExtensionRegistry(List.of())),
            null,
            new CategoryDraftEditor()
        );
        viewModel.newCategory("invoice", "Invoice");
        viewModel.session().markSaved();

        viewModel.updateCategoryMetadata("", "Invoice", "", "1.0");

        assertThat(viewModel.session().dirty()).isTrue();
        assertThat(viewModel.draft().id()).isEmpty();
        assertThat(viewModel.validationProblems()).extracting(DraftValidationProblem::code)
            .contains("CONFIGURATION_INVALID");
    }
}
