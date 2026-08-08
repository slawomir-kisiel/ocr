package pl.sk.ocr.configurator.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import pl.sk.ocr.config.JsonConfigurationMapper;
import pl.sk.ocr.configurator.app.ConfigurationFileService;
import pl.sk.ocr.configurator.app.OpenReferenceDocumentUseCase;
import pl.sk.ocr.configurator.async.BackgroundExecutor;
import pl.sk.ocr.configurator.draft.CategoryDraftEditor;
import pl.sk.ocr.configurator.validation.DraftValidationProblem;
import pl.sk.ocr.configurator.validation.DraftValidationService;
import pl.sk.ocr.core.document.RenderedDocument;
import pl.sk.ocr.core.image.BufferedProcessingImage;
import pl.sk.ocr.domain.identifier.PageNumber;
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

    @Test
    void openingDocumentCachesAllRenderedPages() {
        var viewModel = new CategoryEditorViewModel(
            new ConfigurationFileService((JsonConfigurationMapper) null),
            new OpenReferenceDocumentUseCase((source, options) -> new RenderedDocument(Map.of(
                new PageNumber(1), image(),
                new PageNumber(2), image()
            ))),
            null,
            new DraftValidationService(new DefaultExtensionRegistry(List.of())),
            new ImmediateBackgroundExecutor(),
            new CategoryDraftEditor()
        );

        viewModel.openReferenceDocument(java.nio.file.Path.of("document.pdf")).toCompletableFuture().join();

        assertThat(viewModel.session().pageCache()).containsKeys(new PageNumber(1), new PageNumber(2));
        assertThat(viewModel.status()).contains("pages=2");
    }

    private static BufferedProcessingImage image() {
        return new BufferedProcessingImage(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
    }

    private static final class ImmediateBackgroundExecutor implements BackgroundExecutor {
        @Override
        public <T> CompletionStage<T> submit(Callable<T> task) {
            try {
                return CompletableFuture.completedFuture(task.call());
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }

        @Override
        public void close() {
        }
    }
}
