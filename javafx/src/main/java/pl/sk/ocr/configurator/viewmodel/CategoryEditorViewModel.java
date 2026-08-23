package pl.sk.ocr.configurator.viewmodel;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import pl.sk.ocr.config.dto.*;
import pl.sk.ocr.config.runtime.OcrSettings;
import pl.sk.ocr.configurator.app.ConfigurationFileService;
import pl.sk.ocr.configurator.app.FieldPreviewResult;
import pl.sk.ocr.configurator.app.OpenReferenceDocumentUseCase;
import pl.sk.ocr.configurator.app.PreviewFieldUseCase;
import pl.sk.ocr.configurator.app.RunPageOcrUseCase;
import pl.sk.ocr.configurator.async.BackgroundExecutor;
import pl.sk.ocr.configurator.async.PreviewRunGuard;
import pl.sk.ocr.configurator.draft.CategoryDraftEditor;
import pl.sk.ocr.configurator.session.ConfigurationSession;
import pl.sk.ocr.configurator.validation.DraftValidationProblem;
import pl.sk.ocr.configurator.validation.DraftValidationService;
import pl.sk.ocr.domain.identifier.PageNumber;
import pl.sk.ocr.domain.ocr.OcrText;

public final class CategoryEditorViewModel {
    private final ConfigurationSession session = new ConfigurationSession();
    private final ConfigurationFileService fileService;
    private final OpenReferenceDocumentUseCase openDocument;
    private final RunPageOcrUseCase runOcr;
    private final PreviewFieldUseCase previewField;
    private final DraftValidationService validationService;
    private final BackgroundExecutor backgroundExecutor;
    private final CategoryDraftEditor draftEditor;
    private final PreviewRunGuard previewRunGuard = new PreviewRunGuard();
    private final AtomicReference<String> status = new AtomicReference<>("Ready");
    private final AtomicReference<List<DraftValidationProblem>> validationProblems = new AtomicReference<>(List.of());

    public CategoryEditorViewModel(ConfigurationFileService fileService, OpenReferenceDocumentUseCase openDocument,
                                   RunPageOcrUseCase runOcr, PreviewFieldUseCase previewField, DraftValidationService validationService,
                                   BackgroundExecutor backgroundExecutor) {
        this(fileService, openDocument, runOcr, previewField, validationService, backgroundExecutor, new CategoryDraftEditor());
    }

    CategoryEditorViewModel(ConfigurationFileService fileService, OpenReferenceDocumentUseCase openDocument,
                            RunPageOcrUseCase runOcr, PreviewFieldUseCase previewField, DraftValidationService validationService,
                            BackgroundExecutor backgroundExecutor, CategoryDraftEditor draftEditor) {
        this.fileService = fileService;
        this.openDocument = openDocument;
        this.runOcr = runOcr;
        this.previewField = previewField;
        this.validationService = validationService;
        this.backgroundExecutor = backgroundExecutor;
        this.draftEditor = draftEditor;
    }

    public ConfigurationSession session() {
        return session;
    }

    public String status() {
        return status.get();
    }

    public List<DraftValidationProblem> validationProblems() {
        return validationProblems.get();
    }

    public void newCategory(String id, String displayName) {
        replaceDraft(draftEditor.newCategory(id, displayName));
        status.set("New category draft");
    }

    public void loadCategory(Path path) {
        var draft = fileService.loadCategory(path);
        session.categoryPath(path);
        session.openDraft(draft);
        validationProblems.set(validate());
        status.set("Loaded category: " + path.getFileName());
    }

    public void saveCategory(Path path) {
        fileService.saveCategory(path, session.draftCategory());
        session.categoryPath(path);
        session.markSaved();
        validationProblems.set(validate());
        status.set("Saved category: " + path.getFileName());
    }

    public void updateCategoryMetadata(String id, String displayName, String description, String version) {
        replaceDraft(draftEditor.updateCategoryMetadata(requireDraft(), id, displayName, description, version));
    }

    public void updatePages(PageSelectionDto pages) {
        replaceDraft(draftEditor.updatePages(requireDraft(), pages));
    }

    public void updateOcr(OcrSettingsDto ocr) {
        replaceDraft(draftEditor.updateOcr(requireDraft(), ocr));
    }

    public void addIdentificationGroup(ConditionGroupDto group) {
        replaceDraft(draftEditor.addIdentificationGroup(requireDraft(), group));
    }

    public void removeIdentificationGroup(int index) {
        replaceDraft(draftEditor.removeIdentificationGroup(requireDraft(), index));
    }

    public void moveIdentificationGroup(int fromIndex, int toIndex) {
        replaceDraft(draftEditor.moveIdentificationGroup(requireDraft(), fromIndex, toIndex));
    }

    public void addCondition(int groupIndex, ConditionDto condition) {
        replaceDraft(draftEditor.addCondition(requireDraft(), groupIndex, condition));
    }

    public void replaceCondition(int groupIndex, int conditionIndex, ConditionDto condition) {
        replaceDraft(draftEditor.replaceCondition(requireDraft(), groupIndex, conditionIndex, condition));
    }

    public void removeCondition(int groupIndex, int conditionIndex) {
        replaceDraft(draftEditor.removeCondition(requireDraft(), groupIndex, conditionIndex));
    }

    public void moveCondition(int groupIndex, int fromIndex, int toIndex) {
        replaceDraft(draftEditor.moveCondition(requireDraft(), groupIndex, fromIndex, toIndex));
    }

    public void addAnchor(AnchorDto anchor) {
        replaceDraft(draftEditor.addAnchor(requireDraft(), anchor));
    }

    public void replaceAnchor(int index, AnchorDto anchor) {
        replaceDraft(draftEditor.replaceAnchor(requireDraft(), index, anchor));
    }

    public void removeAnchor(int index) {
        replaceDraft(draftEditor.removeAnchor(requireDraft(), index));
    }

    public void moveAnchor(int fromIndex, int toIndex) {
        replaceDraft(draftEditor.moveAnchor(requireDraft(), fromIndex, toIndex));
    }

    public void updateGeometry(GeometryDto geometry) {
        replaceDraft(draftEditor.updateGeometry(requireDraft(), geometry));
    }

    public void addField(FieldDto field) {
        replaceDraft(draftEditor.addField(requireDraft(), field));
    }

    public void replaceField(int index, FieldDto field) {
        replaceDraft(draftEditor.replaceField(requireDraft(), index, field));
    }

    public void removeField(int index) {
        replaceDraft(draftEditor.removeField(requireDraft(), index));
    }

    public void moveField(int fromIndex, int toIndex) {
        replaceDraft(draftEditor.moveField(requireDraft(), fromIndex, toIndex));
    }

    public void updateFieldOcr(int fieldIndex, OcrSettingsDto ocr) {
        replaceDraft(draftEditor.updateFieldOcr(requireDraft(), fieldIndex, ocr));
    }

    public void updateFieldOutput(int fieldIndex, OutputDto output) {
        replaceDraft(draftEditor.updateFieldOutput(requireDraft(), fieldIndex, output));
    }

    public void updateFieldRegion(int fieldIndex, RegionDto region) {
        replaceDraft(draftEditor.updateFieldRegion(requireDraft(), fieldIndex, region));
    }

    public void addImageProcessor(int fieldIndex, ExtensionRefDto step) {
        replaceDraft(draftEditor.addImageProcessor(requireDraft(), fieldIndex, step));
    }

    public void removeImageProcessor(int fieldIndex, int stepIndex) {
        replaceDraft(draftEditor.removeImageProcessor(requireDraft(), fieldIndex, stepIndex));
    }

    public void moveImageProcessor(int fieldIndex, int fromIndex, int toIndex) {
        replaceDraft(draftEditor.moveImageProcessor(requireDraft(), fieldIndex, fromIndex, toIndex));
    }

    public void duplicateImageProcessor(int fieldIndex, int stepIndex) {
        replaceDraft(draftEditor.duplicateImageProcessor(requireDraft(), fieldIndex, stepIndex));
    }

    public void addTransformer(int fieldIndex, ExtensionRefDto step) {
        replaceDraft(draftEditor.addTransformer(requireDraft(), fieldIndex, step));
    }

    public void removeTransformer(int fieldIndex, int stepIndex) {
        replaceDraft(draftEditor.removeTransformer(requireDraft(), fieldIndex, stepIndex));
    }

    public void moveTransformer(int fieldIndex, int fromIndex, int toIndex) {
        replaceDraft(draftEditor.moveTransformer(requireDraft(), fieldIndex, fromIndex, toIndex));
    }

    public void duplicateTransformer(int fieldIndex, int stepIndex) {
        replaceDraft(draftEditor.duplicateTransformer(requireDraft(), fieldIndex, stepIndex));
    }

    public void addValidator(int fieldIndex, ExtensionRefDto step) {
        replaceDraft(draftEditor.addValidator(requireDraft(), fieldIndex, step));
    }

    public void removeValidator(int fieldIndex, int stepIndex) {
        replaceDraft(draftEditor.removeValidator(requireDraft(), fieldIndex, stepIndex));
    }

    public void moveValidator(int fieldIndex, int fromIndex, int toIndex) {
        replaceDraft(draftEditor.moveValidator(requireDraft(), fieldIndex, fromIndex, toIndex));
    }

    public void duplicateValidator(int fieldIndex, int stepIndex) {
        replaceDraft(draftEditor.duplicateValidator(requireDraft(), fieldIndex, stepIndex));
    }

    public CompletionStage<Void> openReferenceDocument(Path path) {
        status.set("Opening document...");
        return backgroundExecutor.submit(() -> openDocument.open(path)).thenAccept(rendered -> {
            session.referenceDocument(path);
            session.pageCache().clear();
            session.pageCache().putAll(rendered.pages());
            session.currentPage(1);
            status.set("Document opened: " + path.getFileName() + " | pages=" + rendered.pages().size());
        });
    }

    public CompletionStage<OcrText> runCurrentPageOcr() {
        var runId = previewRunGuard.next();
        var pageNumber = new PageNumber(session.currentPage());
        var image = session.pageCache().get(pageNumber);
        if (image == null) {
            throw new IllegalStateException("No rendered page is available");
        }
        status.set("Running OCR...");
        return backgroundExecutor.submit(() -> runOcr.run(image, OcrSettings.defaults())).thenApply(result -> {
            if (previewRunGuard.isLatest(runId)) {
                session.ocrCache().put(pageNumber, result);
                status.set("OCR ready: " + result.words().size() + " words");
                return result;
            }
            return session.ocrCache().get(pageNumber);
        });
    }

    public CompletionStage<FieldPreviewResult> previewField(int fieldIndex) {
        var draft = requireDraft();
        var fields = draft.fields() == null ? List.<FieldDto>of() : draft.fields();
        if (fieldIndex < 0 || fieldIndex >= fields.size()) {
            throw new IllegalArgumentException("Invalid field index " + fieldIndex + " for size " + fields.size());
        }
        if (previewField == null) {
            throw new IllegalStateException("Field preview is not configured");
        }
        var field = fields.get(fieldIndex);
        var pageNumber = new PageNumber(field.page() == null ? session.currentPage() : field.page());
        var pageImage = session.pageCache().get(pageNumber);
        if (pageImage == null) {
            throw new IllegalStateException("No rendered page is available for field page " + pageNumber.value());
        }
        var runId = previewRunGuard.next();
        status.set("Running field preview...");
        return backgroundExecutor.submit(() -> previewField.preview(draft, field, pageImage, session.traceImageStore())).thenApply(result -> {
            if (previewRunGuard.isLatest(runId)) {
                session.latestTrace(result.trace());
                session.latestFieldResult(result.fieldResult());
                status.set("Field preview ready: " + result.fieldResult().fieldId().value()
                    + " | " + result.fieldResult().status());
                return result;
            }
            return new FieldPreviewResult(null, session.latestTrace());
        });
    }

    public List<DraftValidationProblem> validate() {
        var problems = validationService.validate(session.draftCategory());
        validationProblems.set(problems);
        status.set(problems.isEmpty() ? "Configuration valid" : "Configuration has " + problems.size() + " problems");
        return problems;
    }

    public CategoryDto draft() {
        return session.draftCategory();
    }

    private CategoryDto requireDraft() {
        var draft = session.draftCategory();
        if (draft == null) {
            throw new IllegalStateException("No category draft is open");
        }
        return draft;
    }

    private void replaceDraft(CategoryDto draft) {
        session.replaceDraft(draft);
        validationProblems.set(validate());
    }
}
